package dev.polaris_light.constructionwand.items.wand;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import dev.polaris_light.constructionwand.ConstructionWand;
import dev.polaris_light.constructionwand.api.IWandCore;
import dev.polaris_light.constructionwand.basics.WandUtil;
import dev.polaris_light.constructionwand.basics.option.IOption;
import dev.polaris_light.constructionwand.basics.option.WandOptions;
import dev.polaris_light.constructionwand.data.ICustomItemModel;
import dev.polaris_light.constructionwand.data.ItemModelGenerator;
import dev.polaris_light.constructionwand.wand.WandJob;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class ItemWand extends Item implements ICustomItemModel
{
    public ItemWand(Properties properties) {
        super(properties);
    }

    @Nonnull
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        InteractionHand hand = context.getHand();
        Level world = context.getLevel();

        if(world.isClientSide || player == null) return InteractionResult.FAIL;

        ItemStack stack = player.getItemInHand(hand);

        if(ConstructionWand.undoHistory.isUndoActive(player)) {
            return ConstructionWand.undoHistory.undo(player, world, context.getClickedPos()) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        else {
            WandJob job = getWandJob(player, world, new BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), false), stack);
            return job.doIt() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
    }

    @Nonnull
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level world, Player player, @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if(!ConstructionWand.undoHistory.isUndoActive(player)) {
            if(world.isClientSide) return InteractionResultHolder.fail(stack);

            // Some moving-structure mods handle projected blocks through custom raycasts instead
            // of vanilla block interaction. Try a direct raycast before falling back to angel mode.
            BlockHitResult target = getPlayerTarget(world, player);
            if(target == null) {
                target = BlockHitResult.miss(player.getLookAngle(),
                        WandUtil.fromVector(player.getLookAngle()), player.blockPosition());
            }

            WandJob job = getWandJob(player, world, target, stack);
            return job.doIt() ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Nullable
    private static BlockHitResult getPlayerTarget(Level world, Player player) {
        double range = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));

        BlockHitResult hit = world.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    public static WandJob getWandJob(Player player, Level world, @Nullable BlockHitResult rayTraceResult, ItemStack wand) {
        WandJob wandJob = new WandJob(player, world, rayTraceResult, wand);
        wandJob.getSnapshots();

        return wandJob;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState blockIn) {
        return false;
    }

    @Override
    public boolean isValidRepairItem(@Nonnull ItemStack toRepair, @Nonnull ItemStack repair) {
        return false;
    }

    public int remainingDurability(ItemStack stack) {
        return Integer.MAX_VALUE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack itemstack, TooltipContext context, @Nonnull List<Component> lines, @Nonnull TooltipFlag extraInfo) {
        WandOptions options = new WandOptions(itemstack);
        int limit = options.cores.get().getWandAction().getLimit(itemstack);

        String langTooltip = ConstructionWand.MODID + ".tooltip.";

        // +SHIFT tooltip: show all options + installed cores
        if(Screen.hasShiftDown()) {
            for(int i = 1; i < options.allOptions.length; i++) {
                IOption<?> opt = options.allOptions[i];
                lines.add(Component.translatable(opt.getKeyTranslation()).withStyle(ChatFormatting.AQUA)
                        .append(Component.translatable(opt.getValueTranslation()).withStyle(ChatFormatting.GRAY))
                );
            }
            if(!options.cores.getUpgrades().isEmpty()) {
                lines.add(Component.literal(""));
                lines.add(Component.translatable(langTooltip + "cores").withStyle(ChatFormatting.GRAY));

                for(IWandCore core : options.cores.getUpgrades()) {
                    lines.add(Component.translatable(options.cores.getKeyTranslation() + "." + core.getRegistryName().toString()));
                }
            }
        }
        // Default tooltip: show block limit + active wand core
        else {
            IOption<?> opt = options.allOptions[0];
            lines.add(Component.translatable(langTooltip + "blocks", limit).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(opt.getKeyTranslation()).withStyle(ChatFormatting.AQUA)
                    .append(Component.translatable(opt.getValueTranslation()).withStyle(ChatFormatting.WHITE)));
            lines.add(Component.translatable(langTooltip + "shift").withStyle(ChatFormatting.AQUA));
        }
    }

    public static void optionMessage(Player player, IOption<?> option) {
        player.displayClientMessage(
                        Component.translatable(option.getKeyTranslation()).withStyle(ChatFormatting.AQUA)
                        .append(Component.translatable(option.getValueTranslation()).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                        .append(Component.translatable(option.getDescTranslation()).withStyle(ChatFormatting.WHITE))
                , true);
    }

    @Override
    public void generateCustomItemModel(ItemModelGenerator generator, String name) {
        ModelFile wandWithCore = generator.withExistingParent(name + "_core", "item/handheld")
                .texture("layer0", generator.modLoc("item/" + name))
                .texture("layer1", generator.modLoc("item/overlay_core"));

        generator.withExistingParent(name, "item/handheld")
                .texture("layer0", generator.modLoc("item/" + name))
                .override()
                .predicate(generator.modLoc("using_core"), 1)
                .model(wandWithCore).end();

    }
}
