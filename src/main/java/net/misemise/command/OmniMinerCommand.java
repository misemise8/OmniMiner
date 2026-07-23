package net.misemise.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.misemise.BedrockPlayerUtils;
import net.misemise.ClothConfig.Config;
import net.misemise.network.NetworkHandler;

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
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
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
                                .then(Commands.literal("includeBlockEntities")
                                        .then(Commands.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> setIncludeBlockEntities(ctx,
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
        source.sendSuccess(() -> Component.literal("Include Block Entities: " + Config.includeBlockEntities), false);

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
        source.sendSuccess(() -> Component.literal("/omniminer set includeBlockEntities <true|false>"), false);

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
        saveAndSync(ctx);
        ctx.getSource().sendSuccess(() -> Component.literal("Max Blocks set to: " + value), false);
        return 1;
    }

    private static int setSearchDiagonal(CommandContext<CommandSourceStack> ctx, boolean value) {
        Config.searchDiagonal = value;
        saveAndSync(ctx);
        ctx.getSource().sendSuccess(() -> Component.literal("Search Diagonal set to: " + value), false);
        return 1;
    }

    private static int setAutoCollect(CommandContext<CommandSourceStack> ctx, boolean value) {
        Config.autoCollect = value;
        saveAndSync(ctx);
        ctx.getSource().sendSuccess(() -> Component.literal("Auto Collect set to: " + value), false);
        return 1;
    }

    private static int setAutoCollectExp(CommandContext<CommandSourceStack> ctx, boolean value) {
        Config.autoCollectExp = value;
        saveAndSync(ctx);
        ctx.getSource().sendSuccess(() -> Component.literal("Auto Collect Exp set to: " + value), false);
        return 1;
    }

    private static int setBreakLeaves(CommandContext<CommandSourceStack> ctx, boolean value) {
        Config.breakLeaves = value;
        saveAndSync(ctx);
        ctx.getSource().sendSuccess(() -> Component.literal("Break Leaves set to: " + value), false);
        return 1;
    }

    private static int setIncludeBlockEntities(CommandContext<CommandSourceStack> ctx, boolean value) {
        Config.includeBlockEntities = value;
        saveAndSync(ctx);
        ctx.getSource().sendSuccess(() -> Component.literal("Include Block Entities set to: " + value), false);
        return 1;
    }

    private static int setBedrockSneakEnable(CommandContext<CommandSourceStack> ctx, boolean value) {
        Config.bedrockSneakEnable = value;
        saveAndSync(ctx);
        ctx.getSource().sendSuccess(() -> Component.literal("Bedrock Sneak Enable set to: " + value), false);
        return 1;
    }

    private static int setBedrockAllowKeyBind(CommandContext<CommandSourceStack> ctx, boolean value) {
        Config.bedrockAllowKeyBind = value;
        saveAndSync(ctx);
        ctx.getSource().sendSuccess(() -> Component.literal("Bedrock Allow KeyBind set to: " + value), false);
        return 1;
    }

    private static int setBedrockShowParticles(CommandContext<CommandSourceStack> ctx, boolean value) {
        Config.bedrockShowParticles = value;
        saveAndSync(ctx);
        ctx.getSource().sendSuccess(() -> Component.literal("Bedrock Show Particles set to: " + value), false);
        return 1;
    }

    private static int setBedrockParticleMode(CommandContext<CommandSourceStack> ctx, int value) {
        Config.bedrockParticleMode = value;
        saveAndSync(ctx);
        ctx.getSource().sendSuccess(() -> Component.literal("Bedrock Particle Mode set to: "
                + value + " (" + particleModeName(value) + ")"), false);
        return 1;
    }

    private static String particleModeName(int value) {
        return value == 1 ? "Detailed" : "Corner";
    }

    private static void saveAndSync(CommandContext<CommandSourceStack> ctx) {
        Config.save();
        NetworkHandler.broadcastMiningConfig(ctx.getSource().getServer());
    }
}
