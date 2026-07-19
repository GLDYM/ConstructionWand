package dev.polaris_light.constructionwand.client;

import dev.polaris_light.constructionwand.items.ModItems;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

public class ClientHandler {
    public static RenderBlockPreview renderBlockPreview;

    public static void onClientSetup(final FMLClientSetupEvent event) {
        renderBlockPreview = new RenderBlockPreview();
        NeoForge.EVENT_BUS.register(renderBlockPreview);
        NeoForge.EVENT_BUS.register(new KeybindHandler());

        event.enqueueWork(ModItems::registerModelProperties);
    }

    public static void registerKeymapping(final RegisterKeyMappingsEvent event) {
        event.register(KeybindHandler.KEY_OPT);
    }
}