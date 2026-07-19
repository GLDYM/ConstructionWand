package dev.polaris_light.constructionwand.containers.handlers;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import dev.polaris_light.constructionwand.api.IContainerHandler;
import dev.polaris_light.constructionwand.containers.ContainerTrace;
import dev.polaris_light.constructionwand.items.wand.ItemWand;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;

public class HandlerDimensionsNet implements IContainerHandler {
    // This Handler does not related with any specific Item, it works as long as the player has a Dimensions Net.
    // For every itemStack, matches method will return true, but the signature confirms only one net used.

    @Override
    public boolean matches(Player player, ItemStack itemStack, ItemStack inventoryStack) {
        if (!(inventoryStack.getItem() instanceof ItemWand)) return false;
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        return net != null;
    }

    @Override
    public int getSignature(Player player, ItemStack inventoryStack) {
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        return (net != null) ? 10000 + net.getId() : -1;
    }

    @Override
    public int countItems(Player player, ContainerTrace trace, ItemStack itemStack, ItemStack inventoryStack) {
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net == null) return 0;

        UnifiedStorage storage = net.getUnifiedStorage();
        long result = storage.extract(new ItemStackKey(itemStack), Integer.MAX_VALUE, true, false).amount();

        if (result > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) result;
    }

    @Override
    public int useItems(Player player, ContainerTrace trace, ItemStack itemStack, ItemStack inventoryStack, int count) {
        DimensionsNet net = DimensionsNet.getNetFromPlayer(player);
        if (net == null) return 0;

        UnifiedStorage storage = net.getUnifiedStorage();
        long result = storage.extract(new ItemStackKey(itemStack), count, false, false).amount();


        if (result > count) {
            return 0;
        }

        return count - (int) result;
    }
}
