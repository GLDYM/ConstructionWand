package dev.polaris_light.constructionwand.containers;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import dev.polaris_light.constructionwand.api.IContainerHandler;
import java.util.ArrayList;

public class ContainerManager
{
    private final ArrayList<IContainerHandler> handlers;

    public ContainerManager() {
        handlers = new ArrayList<IContainerHandler>();
    }

    public boolean register(IContainerHandler handler) {
        return handlers.add(handler);
    }

    public int countItems(Player player, ContainerTrace trace, ItemStack itemStack, ItemStack inventoryStack) {
        for(IContainerHandler handler : handlers) {
            if(handler.matches(player, itemStack, inventoryStack)) {
                int sig = handler.getSignature(player, inventoryStack);
                if (trace.push(sig)) {
                    int count = handler.countItems(player, trace, itemStack, inventoryStack);
                    return count;
                } else {
                    return 0;
                }
            }
        }
        return 0;
    }

    public int useItems(Player player, ContainerTrace trace, ItemStack itemStack, ItemStack inventoryStack, int count) {
        for(IContainerHandler handler : handlers) {
            if(handler.matches(player, itemStack, inventoryStack)) {
                int sig = handler.getSignature(player, inventoryStack);
                if (trace.push(sig)) {
                    int remaining = handler.useItems(player, trace, itemStack, inventoryStack, count);
                    return remaining;
                } else {
                    return count;
                }
            }
        }
        return count;
    }
}