package net.misemise.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.misemise.BedrockPlayerUtils;
import net.misemise.ClothConfig.Config;

public class OmniMinerCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerCommand(dispatcher));
    }

    private static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("omniminer")
                        .then(CommandManager.literal("config")
                                .executes(OmniMinerCommand::showConfig))
                        .then(CommandManager.literal("set")
                                .then(CommandManager.literal("maxBlocks")
                                        .then(CommandManager.argument("value", IntegerArgumentType.integer(1, 512))
                                                .executes(ctx -> setMaxBlocks(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "value")))))
                                .then(CommandManager.literal("searchDiagonal")
                                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> setSearchDiagonal(ctx,
                                                        BoolArgumentType.getBool(ctx, "value")))))
                                .then(CommandManager.literal("autoCollect")
                                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> setAutoCollect(ctx,
                                                        BoolArgumentType.getBool(ctx, "value")))))
                                .then(CommandManager.literal("autoCollectExp")
                                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> setAutoCollectExp(ctx,
                                                        BoolArgumentType.getBool(ctx, "value")))))
                                .then(CommandManager.literal("breakLeaves")
                                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> setBreakLeaves(ctx,
                                                        BoolArgumentType.getBool(ctx, "value")))))
                                .then(CommandManager.literal("includeBlockEntities")
                                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> setIncludeBlockEntities(ctx,
                                                        BoolArgumentType.getBool(ctx, "value")))))
                                .then(CommandManager.literal("toggleMode")
                                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> setToggleMode(ctx,
                                                        BoolArgumentType.getBool(ctx, "value")))))
                                .then(CommandManager.literal("bedrockSneakEnable")
                                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> setBedrockSneakEnable(ctx,
                                                        BoolArgumentType.getBool(ctx, "value")))))
                                .then(CommandManager.literal("bedrockAllowKeyBind")
                                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> setBedrockAllowKeyBind(ctx,
                                                        BoolArgumentType.getBool(ctx, "value")))))
                                .then(CommandManager.literal("bedrockShowParticles")
                                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> setBedrockShowParticles(ctx,
                                                        BoolArgumentType.getBool(ctx, "value")))))
                                .then(CommandManager.literal("bedrockParticleMode")
                                        .then(CommandManager.argument("value", IntegerArgumentType.integer(0, 1))
                                                .executes(ctx -> setBedrockParticleMode(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "value"))))))
                        .then(CommandManager.literal("addBlock")
                                .then(CommandManager.argument("blockId", StringArgumentType.string())
                                        .executes(ctx -> addBlock(ctx,
                                                StringArgumentType.getString(ctx, "blockId")))))
                        .then(CommandManager.literal("removeBlock")
                                .then(CommandManager.argument("blockId", StringArgumentType.string())
                                        .executes(ctx -> removeBlock(ctx,
                                                StringArgumentType.getString(ctx, "blockId")))))
                        .then(CommandManager.literal("listBlocks")
                                .executes(OmniMinerCommand::listBlocks))
                        .then(CommandManager.literal("help")
                                .executes(OmniMinerCommand::showHelp)));
    }

    private static int showConfig(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        final String playerInfo;

        if (source.getEntity() instanceof ServerPlayerEntity player) {
            boolean isBedrock = BedrockPlayerUtils.isBedrockPlayer(player);
            playerInfo = " [" + (isBedrock ? "Bedrock" : "Java") + " Edition]";
        } else {
            playerInfo = "";
        }

        source.sendFeedback(() -> Text.literal("=== OmniMiner Config ===" + playerInfo), false);
        source.sendFeedback(() -> Text.literal("Max Blocks: " + Config.maxBlocks), false);
        source.sendFeedback(() -> Text.literal("Search Diagonal: " + Config.searchDiagonal), false);
        source.sendFeedback(() -> Text.literal("Auto Collect: " + Config.autoCollect), false);
        source.sendFeedback(() -> Text.literal("Auto Collect Exp: " + Config.autoCollectExp), false);
        source.sendFeedback(() -> Text.literal("Break Leaves: " + Config.breakLeaves), false);
        source.sendFeedback(() -> Text.literal("Include Block Entities: " + Config.includeBlockEntities), false);
        source.sendFeedback(() -> Text.literal("Toggle Mode: " + Config.toggleMode), false);

        if (BedrockPlayerUtils.isFloodgateAvailable()) {
            source.sendFeedback(() -> Text.literal("Bedrock Sneak Enable: " + Config.bedrockSneakEnable), false);
            source.sendFeedback(() -> Text.literal("Bedrock Allow KeyBind: " + Config.bedrockAllowKeyBind), false);
            source.sendFeedback(() -> Text.literal("Bedrock Show Particles: " + Config.bedrockShowParticles), false);
            source.sendFeedback(() -> Text.literal("Bedrock Particle Mode: "
                    + Config.bedrockParticleMode + " (" + particleModeName(Config.bedrockParticleMode) + ")"), false);
        }

        source.sendFeedback(() -> Text.literal("Use '/omniminer help' for command list."), false);
        return 1;
    }

    private static int showHelp(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();

        source.sendFeedback(() -> Text.literal("=== OmniMiner Commands ==="), false);
        source.sendFeedback(() -> Text.literal("/omniminer config - Show current config"), false);
        source.sendFeedback(() -> Text.literal("/omniminer set maxBlocks <1-512> - Set max blocks"), false);
        source.sendFeedback(() -> Text.literal("/omniminer set searchDiagonal <true|false>"), false);
        source.sendFeedback(() -> Text.literal("/omniminer set autoCollect <true|false>"), false);
        source.sendFeedback(() -> Text.literal("/omniminer set autoCollectExp <true|false>"), false);
        source.sendFeedback(() -> Text.literal("/omniminer set breakLeaves <true|false>"), false);
        source.sendFeedback(() -> Text.literal("/omniminer set includeBlockEntities <true|false>"), false);
        source.sendFeedback(() -> Text.literal("/omniminer set toggleMode <true|false>"), false);
        source.sendFeedback(() -> Text.literal("/omniminer addBlock <blockId> - Add a custom block"), false);
        source.sendFeedback(() -> Text.literal("/omniminer removeBlock <blockId> - Remove a custom block"), false);
        source.sendFeedback(() -> Text.literal("/omniminer listBlocks - List custom blocks"), false);

        if (BedrockPlayerUtils.isFloodgateAvailable()) {
            source.sendFeedback(() -> Text.literal("/omniminer set bedrockSneakEnable <true|false>"), false);
            source.sendFeedback(() -> Text.literal("/omniminer set bedrockAllowKeyBind <true|false>"), false);
            source.sendFeedback(() -> Text.literal("/omniminer set bedrockShowParticles <true|false>"), false);
            source.sendFeedback(() -> Text.literal("/omniminer set bedrockParticleMode <0|1>"), false);
        }

        return 1;
    }

    private static int setMaxBlocks(CommandContext<ServerCommandSource> ctx, int value) {
        Config.maxBlocks = value;
        Config.save();
        ctx.getSource().sendFeedback(() -> Text.literal("Max Blocks set to: " + value), false);
        return 1;
    }

    private static int setSearchDiagonal(CommandContext<ServerCommandSource> ctx, boolean value) {
        Config.searchDiagonal = value;
        Config.save();
        ctx.getSource().sendFeedback(() -> Text.literal("Search Diagonal set to: " + value), false);
        return 1;
    }

    private static int setAutoCollect(CommandContext<ServerCommandSource> ctx, boolean value) {
        Config.autoCollect = value;
        Config.save();
        ctx.getSource().sendFeedback(() -> Text.literal("Auto Collect set to: " + value), false);
        return 1;
    }

    private static int setAutoCollectExp(CommandContext<ServerCommandSource> ctx, boolean value) {
        Config.autoCollectExp = value;
        Config.save();
        ctx.getSource().sendFeedback(() -> Text.literal("Auto Collect Exp set to: " + value), false);
        return 1;
    }

    private static int setBreakLeaves(CommandContext<ServerCommandSource> ctx, boolean value) {
        Config.breakLeaves = value;
        Config.save();
        ctx.getSource().sendFeedback(() -> Text.literal("Break Leaves set to: " + value), false);
        return 1;
    }

    private static int setIncludeBlockEntities(CommandContext<ServerCommandSource> ctx, boolean value) {
        Config.includeBlockEntities = value;
        Config.save();
        ctx.getSource().sendFeedback(() -> Text.literal("Include Block Entities set to: " + value), false);
        return 1;
    }

    private static int setToggleMode(CommandContext<ServerCommandSource> ctx, boolean value) {
        Config.toggleMode = value;
        Config.save();
        ctx.getSource().sendFeedback(() -> Text.literal("Toggle Mode set to: " + value), false);
        return 1;
    }

    private static int setBedrockSneakEnable(CommandContext<ServerCommandSource> ctx, boolean value) {
        Config.bedrockSneakEnable = value;
        Config.save();
        ctx.getSource().sendFeedback(() -> Text.literal("Bedrock Sneak Enable set to: " + value), false);
        return 1;
    }

    private static int setBedrockAllowKeyBind(CommandContext<ServerCommandSource> ctx, boolean value) {
        Config.bedrockAllowKeyBind = value;
        Config.save();
        ctx.getSource().sendFeedback(() -> Text.literal("Bedrock Allow KeyBind set to: " + value), false);
        return 1;
    }

    private static int setBedrockShowParticles(CommandContext<ServerCommandSource> ctx, boolean value) {
        Config.bedrockShowParticles = value;
        Config.save();
        ctx.getSource().sendFeedback(() -> Text.literal("Bedrock Show Particles set to: " + value), false);
        return 1;
    }

    private static int setBedrockParticleMode(CommandContext<ServerCommandSource> ctx, int value) {
        Config.bedrockParticleMode = value;
        Config.save();
        ctx.getSource().sendFeedback(() -> Text.literal("Bedrock Particle Mode set to: "
                + value + " (" + particleModeName(value) + ")"), false);
        return 1;
    }

    private static int addBlock(CommandContext<ServerCommandSource> ctx, String blockId) {
        if (Config.customBlocks.contains(blockId)) {
            ctx.getSource().sendFeedback(() -> Text.literal(blockId
                    + " is already in the custom block list."), false);
        } else {
            Config.customBlocks.add(blockId);
            Config.save();
            ctx.getSource().sendFeedback(() -> Text.literal("Added " + blockId
                    + " to custom block list."), false);
        }
        return 1;
    }

    private static int removeBlock(CommandContext<ServerCommandSource> ctx, String blockId) {
        if (Config.customBlocks.remove(blockId)) {
            Config.save();
            ctx.getSource().sendFeedback(() -> Text.literal("Removed " + blockId
                    + " from custom block list."), false);
        } else {
            ctx.getSource().sendFeedback(() -> Text.literal(blockId
                    + " was not in the custom block list."), false);
        }
        return 1;
    }

    private static int listBlocks(CommandContext<ServerCommandSource> ctx) {
        if (Config.customBlocks.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal(
                    "Custom block list is empty. Use /omniminer addBlock <blockId> to add blocks."), false);
        } else {
            ctx.getSource().sendFeedback(() -> Text.literal(
                    "Custom block list (" + Config.customBlocks.size() + " entries):"), false);
            for (String id : Config.customBlocks) {
                ctx.getSource().sendFeedback(() -> Text.literal("  - " + id), false);
            }
        }
        return 1;
    }

    private static String particleModeName(int index) {
        return index == 1 ? "Detailed" : "Corner";
    }
}
