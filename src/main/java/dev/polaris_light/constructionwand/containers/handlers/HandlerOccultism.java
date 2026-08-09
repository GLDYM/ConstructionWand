package dev.polaris_light.constructionwand.containers.handlers;

import com.klikli_dev.occultism.api.common.blockentity.IStorageController;
import com.klikli_dev.occultism.api.common.data.GlobalBlockPos;
import com.klikli_dev.occultism.common.item.storage.StorageRemoteItem;
import com.klikli_dev.occultism.common.misc.ItemStackComparator;
import com.klikli_dev.occultism.registry.OccultismDataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import dev.polaris_light.constructionwand.api.IContainerHandler;
import dev.polaris_light.constructionwand.containers.ContainerTrace;

public class HandlerOccultism implements IContainerHandler {
    @Override
    public boolean matches(Player player, ItemStack itemStack, ItemStack inventoryStack) {
        return inventoryStack != null && inventoryStack.getItem() instanceof StorageRemoteItem;
    }

    @Override
    public int getSignature(Player player, ItemStack inventoryStack) {
        if (!inventoryStack.has(OccultismDataComponents.LINKED_STORAGE_CONTROLLER)) {
            return -1;
        }

        GlobalBlockPos linkedPos = inventoryStack.get(OccultismDataComponents.LINKED_STORAGE_CONTROLLER);
        return linkedPos != null ? linkedPos.hashCode() : -1;
    }

    @Override
    public int countItems(Player player, ContainerTrace trace, ItemStack itemStack, ItemStack inventoryStack) {
        IStorageController storageController = getStorageController(inventoryStack, player);
        if (storageController == null) {
            return 0;
        }

        ItemStackComparator comparator = new ItemStackComparator(itemStack, true);
        int available = storageController.getAvailableAmount(comparator);
        return Math.max(available, 0);
    }

    @Override
    public int useItems(Player player, ContainerTrace trace, ItemStack itemStack, ItemStack inventoryStack, int count) {
        if (count <= 0) {
            return count;
        }

        IStorageController storageController = getStorageController(inventoryStack, player);
        if (storageController == null) {
            return count;
        }

        ItemStackComparator comparator = new ItemStackComparator(itemStack, true);
        ItemStack simulated = storageController.getItemStack(comparator, count, true);
        if (simulated.isEmpty()) {
            return count;
        }

        int request = Math.min(count, simulated.getCount());
        ItemStack extracted = storageController.getItemStack(comparator, request, false);
        if (extracted.isEmpty()) {
            return count;
        }

        int remaining = count - extracted.getCount();
        return Math.max(remaining, 0);
    }

    private IStorageController getStorageController(ItemStack remote, Player player) {
        if (!(remote.getItem() instanceof StorageRemoteItem)) {
            return null;
        }

        return StorageRemoteItem.getStorageController(remote, player.level());
    }
}
