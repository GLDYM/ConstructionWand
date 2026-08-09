package dev.polaris_light.constructionwand.containers.handlers;

import com.tom.storagemod.Config;
import com.tom.storagemod.Content;
import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import com.tom.storagemod.components.WorldPos;
import com.tom.storagemod.item.AdvWirelessTerminalItem;
import com.tom.storagemod.inventory.StoredItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import dev.polaris_light.constructionwand.api.IContainerHandler;
import dev.polaris_light.constructionwand.basics.WandUtil;
import dev.polaris_light.constructionwand.containers.ContainerTrace;

public class HandlerAdvWirelessTerminal implements IContainerHandler {

    @Override
    public boolean matches(Player player, ItemStack itemStack, ItemStack inventoryStack) {
        return inventoryStack != null && inventoryStack.getItem() instanceof AdvWirelessTerminalItem;
    }

    @Override
    public int getSignature(Player player, ItemStack inventoryStack) {
        WorldPos pos = inventoryStack.get(Content.boundPosComponent.get());

        if (pos == null) return -1;

        int x = pos.pos().getX();
        int y = pos.pos().getY();
        int z = pos.pos().getZ();
        String dim = pos.dim().location().toString();
        return (x * 31 + y * 17 + z) ^ dim.hashCode();
    }

    @Override
    public int countItems(Player player, ContainerTrace trace, ItemStack itemStack, ItemStack inventoryStack) {
        if (!(player instanceof ServerPlayer)) return 0;
        StorageTerminalBlockEntity terminal = resolveTerminal(player, inventoryStack);
        if (terminal == null) return 0;
        int found = 0;
        for (var entry : terminal.getStacks().entrySet()) {
            if (WandUtil.stackEquals(entry.getKey().getStack(), itemStack)) {
                found += entry.getValue().getQuantity();
                if (found >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
            }
        }
        return found;
    }

    @Override
    public int useItems(Player player, ContainerTrace trace, ItemStack itemStack, ItemStack inventoryStack, int count) {
        if (!(player instanceof ServerPlayer)) return count;
        StorageTerminalBlockEntity terminal = resolveTerminal(player, inventoryStack);
        if (terminal == null) return count;

        long extracted = terminal.pullStack(new StoredItemStack(itemStack, count), count).getQuantity();
        return count - (int)extracted;
    }

    private StorageTerminalBlockEntity resolveTerminal(Player player, ItemStack stack) {
        WorldPos pos = stack.get(Content.boundPosComponent.get());

        if (pos == null) return null;

        int x = pos.pos().getX();
        int y = pos.pos().getY();
        int z = pos.pos().getZ();
        BlockPos blockPos = new BlockPos(x, y, z);
        String dim = pos.dim().location().toString();

        Level termWorld = player.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryParse(dim)));
        if (termWorld == null || !termWorld.isLoaded(blockPos)) return null;

        BlockEntity be = termWorld.getBlockEntity(blockPos);

        if (be instanceof StorageTerminalBlockEntity terminal) {
            terminal.updateServer();
            return canInteractWith(terminal, player) ? terminal : null;
        }
        return null;
    }

	private boolean canInteractWith(StorageTerminalBlockEntity terminal, Player player) {
		int d = 4;
        int termReach = Config.get().advWirelessRange;

        int beaconLevel = terminal.getBeaconLevel();
		if(Config.get().wirelessTermBeaconLvl != -1 && beaconLevel >= Config.get().wirelessTermBeaconLvl && termReach > 0) {
            if(Config.get().wirelessTermBeaconLvlCrossDim != -1 && beaconLevel >= Config.get().wirelessTermBeaconLvlCrossDim) return true;
			else return player.level() == terminal.getLevel();
		}

		d = Math.max(d, termReach);
		return player.level() == terminal.getLevel() && !(player.distanceToSqr(terminal.getBlockPos().getX() + 0.5D, terminal.getBlockPos().getY() + 0.5D, terminal.getBlockPos().getZ() + 0.5D) > d*2*d*2);
	}
}