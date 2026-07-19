package dev.polaris_light.constructionwand.client;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import dev.polaris_light.constructionwand.ConstructionWand;
import dev.polaris_light.constructionwand.basics.ConfigClient;
import dev.polaris_light.constructionwand.basics.WandUtil;
import dev.polaris_light.constructionwand.basics.option.WandOptions;
import dev.polaris_light.constructionwand.items.wand.ItemWand;
import dev.polaris_light.constructionwand.network.ModMessages;
import dev.polaris_light.constructionwand.network.PacketQueryUndo;
import dev.polaris_light.constructionwand.network.PacketWandOption;

public class KeybindHandler {
    public static final KeyMapping KEY_OPT = new KeyMapping(getKey("wand_option"), GLFW.GLFW_KEY_LEFT_CONTROL, getKey("category"));

    private static String getKey(String name) {
		return String.join(".", "key", ConstructionWand.MODID, name);
	}

	public KeybindHandler() {
        leftShiftPressed = false;
        optPressed = false;
	}

    private boolean leftShiftPressed;
    private boolean optPressed;

    @SubscribeEvent
    public void KeyEvent(InputEvent.Key event) {
        Player player = Minecraft.getInstance().player;
        if(player == null) return;
        if(WandUtil.holdingWand(player) == null) return;

        boolean optState = isOptKeyDown();
        boolean leftShiftState = isLeftShiftKeyDown();
        if(optPressed != optState || leftShiftPressed != leftShiftState) {
            optPressed = optState;
            leftShiftPressed = leftShiftState;
            ModMessages.sendToServer(new PacketQueryUndo(optPressed, leftShiftPressed));
            //ConstructionWand.LOGGER.debug("OPT key update: " + optPressed);
        }
    }

    // (Sneak)+OPT+Scroll to change direction lock
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void MouseScrollEvent(InputEvent.MouseScrollingEvent event) {
        Player player = Minecraft.getInstance().player;
        double scroll = event.getScrollDeltaY();
        if (scroll == 0.0D) {
            scroll = event.getScrollDeltaX();
        }

        if(!modeKeyCombDown() || scroll == 0) return;

        ItemStack wand = WandUtil.holdingWand(player);
        if(wand == null) return;

        WandOptions wandOptions = new WandOptions(wand);
        wandOptions.lock.next(scroll < 0);
        ModMessages.sendToServer(new PacketWandOption(wandOptions.lock, true));
        event.setCanceled(true);
    }

    // (Sneak)+OPT+Left click wand to change core
    @SubscribeEvent
    public void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if(!modeKeyCombDown()) return;

        ItemStack wand = event.getItemStack();
        if(!(wand.getItem() instanceof ItemWand)) return;

        WandOptions wandOptions = new WandOptions(wand);
        wandOptions.cores.next();
        ModMessages.sendToServer(new PacketWandOption(wandOptions.cores, true));
    }

    // (Sneak)+OPT+Right click wand to open GUI
    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if(event.getSide().isServer()) return;

        if(!guiKeyCombDown()) return;

        ItemStack wand = event.getItemStack();
        if(!(wand.getItem() instanceof ItemWand)) return;

        Minecraft.getInstance().setScreen(new ScreenWand(wand));
        event.setCanceled(true);
    }

    public static boolean isLeftShiftKeyDown() {
        return Minecraft.getInstance().options.keyShift.isDown();
    }

    public static boolean isOptKeyDown() {
        return KEY_OPT.isDown();
    }

    public static boolean modeKeyCombDown() {
        return isOptKeyDown() && (isLeftShiftKeyDown() || !ConfigClient.SHIFTOPT_MODE.get());
    }

    public static boolean guiKeyCombDown() {
        return isOptKeyDown() && (isLeftShiftKeyDown() || !ConfigClient.SHIFTOPT_GUI.get());
    }
}
