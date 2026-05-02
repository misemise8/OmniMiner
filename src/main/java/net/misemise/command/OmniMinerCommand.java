package net.misemise.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.misemise.BedrockPlayerUtils;
import net.misemise.ClothConfig.Config;

public class OmniMinerCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerCommand(dispatcher));
    }

    private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("omniminer")
                        .then(Commands.literal("config")
                                .executes(OmniMinerCommand::showConfig))
                        .then(Commands.literal("set")
                                .then(Commands.literal("maxBlocks")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 512))
                                                .executes(ctx -> setMaxBlocks(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "value")))))
                                .then(Commands.literal("searchDiagonal")
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> setSearchDiagonal(ctx,
                                                        BoolArgumentType.getBool(ctx, "value")))))
                                .then(Commands.literal("autoCollect")
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> setAutoCollect(ctx,
                                                        BoolArgumentType.getBool(ctx, "value")))))
                                .then(Commands.literal("autoCollectExp")
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> setAutoCollectExp(ctx,
                                                        BoolArgumentType.getBool(ctx, "value")))))
                                .then(Commands.literal("breakLeaves")
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> setBreakLeaves(ctx,
                                                        BoolArgumentType.getBool(ctx, "value")))))
                                .then(Commands.literal("toggleMode")
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> setToggleMode(ctx,
                                                        BoolArgumentType.getBool(ctx, "value")))))
                                .then(Commands.literal("bedrockSneakEnable")
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> setBedrockSneakEnable(ctx,
                                                        BoolArgumentType.getBool(ctx, "value")))))
                                .then(Commands.literal("bedrockAllowKeyBind")
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> setBedrockAllowKeyBind(ctx,
                                                        BoolArgumentType.getBool(ctx, "value")))))
                                .then(Commands.literal("bedrockShowParticles")
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> setBedrockShowParticles(ctx,
                                                        BoolArgumentType.getBool(ctx, "value")))))
                                .then(Commands.literal("bedrockParticleMode")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 1))
                                                .executes(ctx -> setBedrockParticleMode(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "value"))))))
                        .then(Commands.literal("addBlock")
                                .then(Commands.argument("blockId", StringArgumentType.string())
                                        .executes(ctx -> addBlock(ctx,
                                                StringArgumentType.getString(ctx, "blockId")))))
                        .then(Commands.literal("removeBlock")
                                .then(Commands.argument("blockId", StringArgumentType.string())
                                        .executes(ctx -> removeBlock(ctx,
                                                StringArgumentType.getString(ctx, "blockId")))))
                        .then(Commands.literal("listBlocks")
                                .executes(OmniMinerCommand::listBlocks))
                        .then(Commands.literal("help")
                                .executes(OmniMinerCommand::showHelp)));
    }

    private static int showConfig(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        final String playerInfo;

        if (source.getEntity() instanceof ServerPlayer player) {
            boolean isBedrock = BedrockPlayerUtils.isBedrockPlayer(player);
            playerInfo = " [" + (isBedrock ? "Bedrock" : "Java") + " Edition]";
        } else {
            playerInfo = "";
        }

        source.sendSuccess(() -> Component.literal("=== OmniMiner Config ===" + playerInfo), false);
        source.sendSuccess(() -> Component.literal("Max Blocks: " + Config.maxBlocks), false);
        source.sendSuccess(() -> Component.literal("Search Diagonal: " + Config.searchDiagonal), false);
        source.sendSuccess(() -> Component.literal("Auto Collect: " + Config.autoCollect), false);
        source.sendSuccess(() -> Component.literal("Auto Collect Exp: " + Config.autoCollectExp), false);
        source.sendSuccess(() -> Component.literal("Break Leaves: " + Config.breakLeaves), false);
        source.sendSuccess(() -> Component.literal("Toggle Mode: " + Config.toggleMode), false);

        if (BedrockPlayerUtils.isFloodgateAvailable()) {
            source.sendSuccess(() -> Component.literal("Bedrock Sneak Enable: " + Config.bedrockSneakEnable), false);
            source.sendSuccess(() -> Component.literal("Bedrock Allow KeyBind: " + Config.bedrockAllowKeyBind), false);
            source.sendSuccess(() -> Component.literal("Bedrock Show Particles: " + Config.bedrockShowParticles), false);
            source.sendSuccess(() -> Component.literal("Bedrock Particle Mode: "
                    + Config.bedrockParticleMode + " (" + particleModeName(Config.bedrockParticleMode) + ")"), false);
        }

        source.sendSuccess(() -> Component.literal("Use '/omniminer help' for command list."), false);
        return 1;
    }

    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        source.sendSuccess(() -> Component.literal("=== OmniMiner Commands ==="), false);
        source.sendSuccess(() -> Component.literal("/omniminer config - Show current config"), false);
        source.sendSuccess(() -> Component.literal("/omniminer set maxBlocks <1-512> - Set max blocks"), false);
        source.sendSuccess(() -> Component.literal("/omniminer set searchDiagonal <true|false>"), false);
        source.sendSuccess(() -> Component.literal("/omniminer set autoCollect <true|false>"), false);
        source.sendSuccess(() -> Component.literal("/omniminer set autoCollectExp <true|false>"), false);
        source.sendSuccess(() -> Component.literal("/omniminer set breakLeaves <true|false>"), false);
        source.sendSuccess(() -> Component.literal("/omniminer set toggleMode <true|false>"), false);
        source.sendSuccess(() -> Component.literal("/omniminer addBlock <blockId> - Add a custom block"), false);
        source.sendSuccess(() -> Component.literal("/omniminer removeBlock <blockId> - Remove a custom block"), false);
        source.sendSuccess(() -> Component.literal("/omniminer listBlocks - List custom blocks"), false);

        if (BedrockPlayerUtils.isFloodgateAvailable()) {
            source.sendSuccess(() -> Component.literal("/omniminer set bedrockSneakEnable <true|false>"), false);
            source.sendSuccess(() -> Component.literal("/omniminer set bedrockAllowKeyBind <true|false>"), false);
            source.sendSuccess(() -> Component.literal("/omniminer set bedrockShowParticles <true|false>"), false);
            source.sendSuccess(() -> Component.literal("/omniminer set bedrockParticleMode <0|1>"), false);
        }

        return 1;
    }

    private static int setMaxBlocks(CommandContext<CommandSourceStack> ctx, int value) {
        Config.maxBlocks = value;
        Config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("Max Blocks set to: " + value), false);
        return 1;
    }

    private static int setSearchDiagonal(CommandContext<CommandSourceStack> ctx, boolean value) {
        Config.searchDiagonal = value;
        Config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("Search Diagonal set to: " + value), false);
        return 1;
    }

    private static int setAutoCollect(CommandContext<CommandSourceStack> ctx, boolean value) {
        Config.autoCollect = value;
        Config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("Auto Collect set to: " + value), false);
        return 1;
    }

    private static int setAutoCollectExp(CommandContext<CommandSourceStack> ctx, boolean value) {
        Config.autoCollectExp = value;
        Config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("Auto Collect Exp set to: " + value), false);
        return 1;
    }

    private static int setBreakLeaves(CommandContext<CommandSourceStack> ctx, boolean value) {
        Config.breakLeaves = value;
        Config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("Break Leaves set to: " + value), false);
        return 1;
    }

    private static int setToggleMode(CommandContext<CommandSourceStack> ctx, boolean value) {
        Config.toggleMode = value;
        Config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("Toggle Mode set to: " + value), false);
        return 1;
    }

    private static int setBedrockSneakEnable(CommandContext<CommandSourceStack> ctx, boolean value) {
        Config.bedrockSneakEnable = value;
        Config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("Bedrock Sneak Enable set to: " + value), false);
        return 1;
    }

    private static int setBedrockAllowKeyBind(CommandContext<CommandSourceStack> ctx, boolean value) {
        Config.bedrockAllowKeyBind = value;
        Config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("Bedrock Allow KeyBind set to: " + value), false);
        return 1;
    }

    private static int setBedrockShowParticles(CommandContext<CommandSourceStack> ctx, boolean value) {
        Config.bedrockShowParticles = value;
        Config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("Bedrock Show Particles set to: " + value), false);
        return 1;
    }

    private static int setBedrockParticleMode(CommandContext<CommandSourceStack> ctx, int value) {
        Config.bedrockParticleMode = value;
        Config.save();
        ctx.getSource().sendSuccess(() -> Component.literal("Bedrock Particle Mode set to: "
                + value + " (" + particleModeName(value) + ")"), false);
        return 1;
    }

    private static int addBlock(CommandContext<CommandSourceStack> ctx, String blockId) {
        if (Config.customBlocks.contains(blockId)) {
            ctx.getSource().sendSuccess(() -> Component.literal(blockId
                    + " is already in the custom block list."), false);
        } else {
            Config.customBlocks.add(blockId);
            Config.save();
            ctx.getSource().sendSuccess(() -> Component.literal("Added " + blockId
                    + " to custom block list."), false);
        }
        return 1;
    }

    private static int removeBlock(CommandContext<CommandSourceStack> ctx, String blockId) {
        if (Config.customBlocks.remove(blockId)) {
            Config.save();
            ctx.getSource().sendSuccess(() -> Component.literal("Removed " + blockId
                    + " from custom block list."), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal(blockId
                    + " was not in the custom block list."), false);
        }
        return 1;
    }

    private static int listBlocks(CommandContext<CommandSourceStack> ctx) {
        if (Config.customBlocks.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Custom block list is empty. Use /omniminer addBlock <blockId> to add blocks."), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "Custom block list (" + Config.customBlocks.size() + " entries):"), false);
            for (String id : Config.customBlocks) {
                ctx.getSource().sendSuccess(() -> Component.literal("  - " + id), false);
            }
        }
        return 1;
    }

    private static String particleModeName(int value) {
        return value == 1 ? "Detailed" : "Corner";
    }
}
