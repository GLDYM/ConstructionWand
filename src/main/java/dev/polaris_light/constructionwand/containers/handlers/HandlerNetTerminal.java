package dev.polaris_light.constructionwand.containers.handlers;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import dev.polaris_light.constructionwand.api.IContainerHandler;
import dev.polaris_light.constructionwand.containers.ContainerTrace;

import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import com.wintercogs.beyonddimensions.api.dimensionnet.UnifiedStorage;
import com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey;
import com.wintercogs.beyonddimensions.common.item.NetedItem;
import com.wintercogs.beyonddimensions.common.init.BDDataComponents;
import com.wintercogs.beyonddimensions.common.item.NetTerminalItem;

public class HandlerNetTerminal implements IContainerHandler {

    @Override
    public boolean matches(Player player, ItemStack itemStack, ItemStack inventoryStack) {
        return inventoryStack.getItem() instanceof NetTerminalItem;
    }

    @Override
    public int getSignature(Player player, ItemStack inventoryStack) {
        if (!(inventoryStack.getItem() instanceof NetTerminalItem)) return -1;
        int netId = (Integer)inventoryStack.getOrDefault(BDDataComponents.NET_ID_DATA, -1);
        return (netId != -1) ? 10000 + netId : -1;
    }

    @Override
    public int countItems(Player player, ContainerTrace trace, ItemStack itemStack, ItemStack inventoryStack) {
        DimensionsNet net = NetedItem.getNet(inventoryStack);
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
        DimensionsNet net = NetedItem.getNet(inventoryStack);
        if (net == null) return count;

        UnifiedStorage storage = net.getUnifiedStorage();
        long result = storage.extract(new ItemStackKey(itemStack), count, false, false).amount();

        if (result > count) {
            return 0;
        }

        return count - (int) result;
    }
}
