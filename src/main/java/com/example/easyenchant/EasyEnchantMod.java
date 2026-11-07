package com.example.easyenchant;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * /ench <エンチャント> [レベル]  … 手に持ったアイテムにエンチャント(レベル 1〜255)
 * /ench all [レベル]             … 付けられる全エンチャントを付与
 * /ench clear                    … エンチャントを全部消す
 * レベルを省略すると 255。
 */
public class EasyEnchantMod implements ModInitializer {
    public static final int MAX_LEVEL = 255;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) ->
                register(dispatcher, buildContext));
    }

    private static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher,
                                 CommandBuildContext buildContext) {
        dispatcher.register(Commands.literal("ench")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("enchantment", ResourceArgument.resource(buildContext, Registries.ENCHANTMENT))
                        .executes(ctx -> applyOne(ctx, MAX_LEVEL))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, MAX_LEVEL))
                                .executes(ctx -> applyOne(ctx, IntegerArgumentType.getInteger(ctx, "level")))))
                .then(Commands.literal("all")
                        .executes(ctx -> applyAll(ctx, MAX_LEVEL))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, MAX_LEVEL))
                                .executes(ctx -> applyAll(ctx, IntegerArgumentType.getInteger(ctx, "level")))))
                .then(Commands.literal("clear")
                        .executes(EasyEnchantMod::clear)));
    }

    private static ItemStack heldStack(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("メインハンドにアイテムを持ってください"));
            return ItemStack.EMPTY;
        }
        return stack;
    }

    private static int applyOne(CommandContext<CommandSourceStack> ctx, int level) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ItemStack stack = heldStack(ctx);
        if (stack.isEmpty()) return 0;
        Holder.Reference<Enchantment> ench = ResourceArgument.getEnchantment(ctx, "enchantment");
        EnchantmentHelper.updateEnchantments(stack, m -> m.set(ench, level));
        ctx.getSource().sendSuccess(() -> Component.literal("エンチャントを付与しました (Lv." + level + ")"), true);
        return 1;
    }

    private static int applyAll(CommandContext<CommandSourceStack> ctx, int level) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ItemStack stack = heldStack(ctx);
        if (stack.isEmpty()) return 0;
        var registry = ctx.getSource().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int[] count = {0};
        EnchantmentHelper.updateEnchantments(stack, m -> registry.listElements().forEach(h -> {
            if (h.value().canEnchant(stack)) {
                m.set(h, level);
                count[0]++;
            }
        }));
        ctx.getSource().sendSuccess(() -> Component.literal(count[0] + "種類のエンチャントを Lv." + level + " で付与しました"), true);
        return count[0];
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ItemStack stack = heldStack(ctx);
        if (stack.isEmpty()) return 0;
        EnchantmentHelper.updateEnchantments(stack, m -> m.keySet().clear());
        ctx.getSource().sendSuccess(() -> Component.literal("エンチャントを消しました"), true);
        return 1;
    }
}
