package thetadev.constructionwand.containers.handlers;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import thetadev.constructionwand.api.IContainerHandler;
import thetadev.constructionwand.containers.ContainerTrace;

public class HandlerProjectE implements IContainerHandler {
    private static final String PROJECTE = "projecte";

    @Override
    public boolean matches(Player player, ItemStack inventoryStack) {
        return inventoryStack != null
                && !inventoryStack.isEmpty()
                && isTransmutationAccess(inventoryStack);
    }

    @Override
    public int getSignature(Player player, ItemStack inventoryStack) {
        return Objects.hash(PROJECTE, player.getUUID());
    }

    @Override
    public int countItems(Player player, ContainerTrace trace, ItemStack itemStack, ItemStack inventoryStack) {
        IKnowledgeProvider knowledge = getKnowledge(player);
        if (knowledge == null) {
            return 0;
        }

        ItemInfo info = IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(itemStack));
        long emcValue = IEMCProxy.INSTANCE.getValue(info);
        if (!knowledge.hasKnowledge(info) || emcValue <= 0) {
            return 0;
        }

        BigInteger affordable = knowledge.getEmc().divide(BigInteger.valueOf(emcValue));
        return affordable.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0
                ? Integer.MAX_VALUE
                : affordable.intValue();
    }

    @Override
    public int useItems(Player player, ContainerTrace trace, ItemStack itemStack, ItemStack inventoryStack, int count) {
        if (count <= 0 || player.level().isClientSide) {
            return count;
        }

        IKnowledgeProvider knowledge = getKnowledge(player);
        if (knowledge == null) {
            return count;
        }

        ItemInfo info = IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(itemStack));
        long emcValue = IEMCProxy.INSTANCE.getValue(info);
        if (!knowledge.hasKnowledge(info) || emcValue <= 0) {
            return count;
        }

        BigInteger unitCost = BigInteger.valueOf(emcValue);
        BigInteger maxAffordable = knowledge.getEmc().divide(unitCost);
        int toTake = maxAffordable.min(BigInteger.valueOf(count)).intValue();
        if (toTake <= 0) {
            return count;
        }

        knowledge.setEmc(knowledge.getEmc().subtract(unitCost.multiply(BigInteger.valueOf(toTake))));
        if (player instanceof ServerPlayer serverPlayer) {
            knowledge.syncEmc(serverPlayer);
        }
        return count - toTake;
    }

    private static IKnowledgeProvider getKnowledge(Player player) {
        Optional<IKnowledgeProvider> capability =
                player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY).resolve();
        return capability.orElse(null);
    }

    private static boolean isTransmutationAccess(ItemStack stack) {
        Item table = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(PROJECTE, "transmutation_table"));
        if (table != null && stack.is(table)) {
            return true;
        }

        Item tablet = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(PROJECTE, "transmutation_tablet"));
        return tablet != null && stack.is(tablet);
    }
}
