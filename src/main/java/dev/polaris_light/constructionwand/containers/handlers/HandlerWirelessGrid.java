package dev.polaris_light.constructionwand.containers.handlers;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.node.GraphNetworkComponent;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.Platform;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemPlayerValidator;
import com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemTargetBlockEntity;
import com.refinedmods.refinedstorage.common.content.DataComponents;
import com.refinedmods.refinedstorage.common.grid.WirelessGridItem;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import dev.polaris_light.constructionwand.api.IContainerHandler;
import dev.polaris_light.constructionwand.containers.ContainerTrace;

public class HandlerWirelessGrid implements IContainerHandler {
    // private static final String MSG_NOT_BOUND = "misc.refinedstorage.wireless_grid.not_bound";
    // private static final String MSG_OUT_OF_ENERGY = "misc.refinedstorage.wireless_grid.out_of_energy";

    @Override
    public boolean matches(Player player, ItemStack itemStack, ItemStack inventoryStack) {
        return inventoryStack.getItem() instanceof WirelessGridItem;
    }

    @Override
    public int getSignature(Player player, ItemStack inventoryStack) {
        GlobalPos pos = inventoryStack.get(DataComponents.INSTANCE.getNetworkLocation());
        if (pos == null) return -1;

        int x = pos.pos().getX();
        int y = pos.pos().getY();
        int z = pos.pos().getZ();
        String dim = pos.dimension().location().toString();
        return (x * 31 + y * 17 + z) ^ dim.hashCode();
    }

    @Override
    public int countItems(Player player, ContainerTrace trace, ItemStack itemStack, ItemStack inventoryStack) {
        if (!(player instanceof ServerPlayer)) return 0;

        Network network = resolveNetworkForItem(player, inventoryStack);
        if (network == null) return 0;

        // Creative wireless grid doesn't require energy
        if (!isCreativeWirelessGrid(inventoryStack)) {
            long extractCost = getWirelessGridExtractEnergyUsage();
            long energyStored = getItemEnergyStored(inventoryStack);
            if (extractCost > 0 && energyStored <= 0) {
                return 0;
            }

            long maxByEnergy = extractCost <= 0 ? Integer.MAX_VALUE : energyStored / extractCost;
            if (maxByEnergy <= 0) {
                return 0;
            }

            long canExtract = extractItemAmount(network, itemStack, maxByEnergy, Action.SIMULATE);
            if (canExtract <= 0) return 0;
            return canExtract > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) canExtract;
        }

        // Creative grid: no energy limit
        long canExtract = extractItemAmount(network, itemStack, Integer.MAX_VALUE, Action.SIMULATE);
        if (canExtract <= 0) return 0;
        return canExtract > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) canExtract;
    }

    @Override
    public int useItems(Player player, ContainerTrace trace, ItemStack itemStack, ItemStack inventoryStack, int count) {
        if (!(player instanceof ServerPlayer) || count <= 0) return count;

        Network network = resolveNetworkForItem(player, inventoryStack);
        if (network == null) {
            // player.displayClientMessage(Component.translatable(MSG_NOT_BOUND), true);
            return count;
        }

        // Creative wireless grid doesn't require energy
        if (isCreativeWirelessGrid(inventoryStack)) {
            long canExtract = extractItemAmount(network, itemStack, count, Action.SIMULATE);
            if (canExtract <= 0) return count;

            long extracted = extractItemAmount(network, itemStack, canExtract, Action.EXECUTE);
            if (extracted <= 0) return count;

            long remaining = count - extracted;
            return remaining <= 0 ? 0 : (remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remaining);
        }

        // Regular wireless grid: enforce energy cost
        long extractCost = getWirelessGridExtractEnergyUsage();
        long energyStored = getItemEnergyStored(inventoryStack);

        long maxByEnergy = extractCost <= 0 ? count : energyStored / extractCost;
        if (maxByEnergy <= 0) {
            // player.displayClientMessage(Component.translatable(MSG_OUT_OF_ENERGY), true);
            return count;
        }

        long request = Math.min(count, maxByEnergy);
        long canExtract = extractItemAmount(network, itemStack, request, Action.SIMULATE);
        if (canExtract <= 0) return count;

        long extracted = extractItemAmount(network, itemStack, canExtract, Action.EXECUTE);
        if (extracted <= 0) return count;

        if (extractCost > 0) {
            long toDrain = extracted * extractCost;
            drainItemEnergy(inventoryStack, toDrain);
        }

        long remaining = count - extracted;
        return remaining <= 0 ? 0 : (remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remaining);
    }

    private Network resolveNetworkForItem(Player player, ItemStack stack) {
        GlobalPos pos = stack.get(DataComponents.INSTANCE.getNetworkLocation());
        if (pos == null) return null;

        if (player.getServer() == null) return null;
        Level level = player.getServer().getLevel(pos.dimension());
        if (level == null || !level.isLoaded(pos.pos())) return null;

        BlockEntity blockEntity = level.getBlockEntity(pos.pos());
        if (!(blockEntity instanceof NetworkItemTargetBlockEntity targetBlockEntity)) return null;

        Network network = targetBlockEntity.getNetworkForItem();
        if (network == null) return null;

        // Let RS2 validators decide if this player can access the network from current coords.
        return isNetworkValidForPlayer(network, player) ? network : null;
    }

    private boolean isNetworkValidForPlayer(Network network, Player player) {
        GraphNetworkComponent graph = network.getComponent(GraphNetworkComponent.class);
        NetworkItemPlayerValidator.PlayerCoordinates coordinates =
            new NetworkItemPlayerValidator.PlayerCoordinates(player.level().dimension(), player.position());

        for (NetworkItemPlayerValidator validator : graph.getContainers(NetworkItemPlayerValidator.class)) {
            if (validator.isValid(coordinates)) {
                return true;
            }
        }

        return false;
    }

    private long extractItemAmount(Network network, ItemStack stack, long amount, Action action) {
        if (amount <= 0) return 0;
        ItemResource resource = ItemResource.ofItemStack(stack);
        StorageNetworkComponent storage = network.getComponent(StorageNetworkComponent.class);
        return storage.extract(resource, amount, action, Actor.EMPTY);
    }

    private long getWirelessGridExtractEnergyUsage() {
        return Platform.INSTANCE.getConfig().getWirelessGrid().getExtractEnergyUsage();
    }

    private long getItemEnergyStored(ItemStack stack) {
        return RefinedStorageApi.INSTANCE.getEnergyStorage(stack)
            .map(energyStorage -> Math.max(0L, energyStorage.getStored()))
            .orElse(0L);
    }

    private void drainItemEnergy(ItemStack stack, long amount) {
        if (amount <= 0) return;
        RefinedStorageApi.INSTANCE.getEnergyStorage(stack)
            .ifPresent(energyStorage -> energyStorage.extract(amount, Action.EXECUTE));
    }

    private boolean isCreativeWirelessGrid(ItemStack stack) {
        Item item = stack.getItem();
        ResourceLocation location = BuiltInRegistries.ITEM.getKey(item);
        return location != null && location.toString().equals("refinedstorage:creative_wireless_grid");
    }
}
