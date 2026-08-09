package dev.polaris_light.constructionwand;

import dev.polaris_light.constructionwand.basics.ConfigClient;
import dev.polaris_light.constructionwand.basics.ConfigServer;
import dev.polaris_light.constructionwand.basics.ModStats;
import dev.polaris_light.constructionwand.client.ClientHandler;
import dev.polaris_light.constructionwand.containers.ContainerManager;
import dev.polaris_light.constructionwand.containers.ContainerRegistrar;
import dev.polaris_light.constructionwand.crafting.ModRecipes;
import dev.polaris_light.constructionwand.items.ModItems;
import dev.polaris_light.constructionwand.network.ModMessages;
import dev.polaris_light.constructionwand.wand.undo.UndoHistory;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ConstructionWand.MODID)
public class ConstructionWand {
    public static final String MODID = "constructionwand";
    public static final String MODNAME = "ConstructionWand";

    public static final Logger LOGGER = LogManager.getLogger();

    public static ContainerManager containerManager;
    public static UndoHistory undoHistory;

    public ConstructionWand(IEventBus eventBus, ModContainer container, Dist dist) {
        containerManager = new ContainerManager();
        undoHistory = new UndoHistory();

        eventBus.addListener(this::commonSetup);
        eventBus.addListener(ModMessages::registerPayloads);

        ModItems.ITEMS.register(eventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(eventBus);
        ModStats.CUSTOM_STATS.register(eventBus);

        container.registerConfig(ModConfig.Type.STARTUP, ConfigServer.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ConfigClient.SPEC);

        if (dist.isClient()) {
            eventBus.addListener(ClientHandler::onClientSetup);
            eventBus.addListener(ClientHandler::registerKeymapping);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("ConstructionWand says hello - may the odds be ever in your favor.");

        ContainerRegistrar.register();
    }

    public static Identifier loc(String name) {
        return Identifier.fromNamespaceAndPath(MODID, name);
    }
}
