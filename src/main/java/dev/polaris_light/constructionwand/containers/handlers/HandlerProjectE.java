package dev.polaris_light.constructionwand.containers.handlers;

import java.math.BigInteger;
import java.util.Objects;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import dev.polaris_light.constructionwand.api.IContainerHandler;
import dev.polaris_light.constructionwand.containers.ContainerTrace;

/** Uses the player's ProjectE EMC and knowledge as a virtual inventory. */
public class HandlerProjectE implements IContainerHandler {
    private static final String PROJECTE = "projecte";

    @Override
    public boolean matches(Player player, ItemStack itemStack, ItemStack inventoryStack) {
        return inventoryStack != null && !inventoryStack.isEmpty() && isTransmutationAccess(inventoryStack);
    }

    @Override
    public int getSignature(Player player, ItemStack inventoryStack) {
        return Objects.hash(PROJECTE, player.getUUID());
    }

    @Override
    public int countItems(Player player, ContainerTrace trace, ItemStack itemStack, ItemStack inventoryStack) {
        IKnowledgeProvider knowledge = player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY);
        if (knowledge == null) {
            return 0;
        }

        ItemInfo info = IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(itemStack));
        long value = IEMCProxy.INSTANCE.getValue(info);
        if (!knowledge.hasKnowledge(info) || value <= 0) {
            return 0;
        }

        BigInteger available = knowledge.getEmc().divide(BigInteger.valueOf(value));
        return available.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0
                ? Integer.MAX_VALUE
                : available.intValue();
    }

    @Override
    public int useItems(Player player, ContainerTrace trace, ItemStack itemStack, ItemStack inventoryStack, int count) {
        if (count <= 0 || player.level().isClientSide()) {
            return count;
        }

        IKnowledgeProvider knowledge = player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY);
        if (knowledge == null) {
            return count;
        }

        ItemInfo info = IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(itemStack));
        long value = IEMCProxy.INSTANCE.getValue(info);
        if (!knowledge.hasKnowledge(info) || value <= 0) {
            return count;
        }

        BigInteger unitCost = BigInteger.valueOf(value);
        int toTake = knowledge.getEmc().divide(unitCost)
                .min(BigInteger.valueOf(count)).intValue();
        if (toTake <= 0) {
            return count;
        }

        knowledge.setEmc(knowledge.getEmc().subtract(unitCost.multiply(BigInteger.valueOf(toTake))));
        if (player instanceof ServerPlayer serverPlayer) {
            knowledge.syncEmc(serverPlayer);
        }
        return count - toTake;
    }

    private static boolean isTransmutationAccess(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(PROJECTE)
                && (BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals("transmutation_table")
                || BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals("transmutation_tablet"));
    }

}
