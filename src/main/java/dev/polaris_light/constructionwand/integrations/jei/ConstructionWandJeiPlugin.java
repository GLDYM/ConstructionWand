package dev.polaris_light.constructionwand.integrations.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;
import dev.polaris_light.constructionwand.ConstructionWand;
import dev.polaris_light.constructionwand.basics.ConfigClient;
import dev.polaris_light.constructionwand.basics.ConfigServer;
import dev.polaris_light.constructionwand.client.KeybindHandler;
import dev.polaris_light.constructionwand.items.ModItems;

import javax.annotation.Nonnull;

@JeiPlugin
public class ConstructionWandJeiPlugin implements IModPlugin
{
    private static final ResourceLocation pluginId = ResourceLocation.fromNamespaceAndPath(ConstructionWand.MODID, ConstructionWand.MODID);
    private static final String baseKey = ConstructionWand.MODID + ".description.";
    private static final String baseKeyItem = "item." + ConstructionWand.MODID + ".";

    @Nonnull
    @Override
    public ResourceLocation getPluginUid() {
        return pluginId;
    }

    private Component keyComboComponent(boolean shiftOpt, Component optkeyComponent) {
        String key = shiftOpt ? "sneak_opt" : "opt";
        return Component.translatable(baseKey + "key." + key, optkeyComponent).withStyle(ChatFormatting.BLUE);
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        Component optkeyComponent = KeybindHandler.KEY_OPT.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.BLUE);
        Component wandModeComponent = keyComboComponent(ConfigClient.SHIFTOPT_MODE.get(), optkeyComponent);
        Component wandGuiComponent = keyComboComponent(ConfigClient.SHIFTOPT_GUI.get(), optkeyComponent);

        for(DeferredHolder<Item, Item> wandSupplier : ModItems.WANDS) {
            Item wand = wandSupplier.get();
            ConfigServer.WandProperties wandProperties = ConfigServer.getWandProperties(wand);

            String durabilityKey = wand == ModItems.WAND_INFINITY.get() ? "unlimited" : "limited";
            Component durabilityComponent = Component.translatable(baseKey + "durability." + durabilityKey, wandProperties.getDurability());

            registration.addIngredientInfo(new ItemStack(wand), VanillaTypes.ITEM_STACK,
                    Component.translatable(baseKey + "wand",
                            Component.translatable(baseKeyItem + BuiltInRegistries.ITEM.getKey(wand).getPath()),
                            wandProperties.getLimit(), durabilityComponent, optkeyComponent, wandModeComponent, wandGuiComponent)
            );
        }

        for(DeferredHolder<Item, Item> coreSupplier : ModItems.CORES) {
            Item core = coreSupplier.get();
            registration.addIngredientInfo(new ItemStack(core), VanillaTypes.ITEM_STACK,
                    Component.translatable(baseKey + BuiltInRegistries.ITEM.getKey(core).getPath())
                            .append("\n\n")
                            .append(Component.translatable(baseKey + "core", wandModeComponent))
            );
        }
    }
}
