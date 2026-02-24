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

/**
 * /omniminer コマンドの実装
 * 統合版プレイヤーでも設定を変更できるようにする
 */
public class OmniMinerCommand {

        public static void register() {
                CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
                        registerCommand(dispatcher);
                });
        }

        private static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
                dispatcher.register(
                                CommandManager.literal("omniminer")
                                                .then(CommandManager.literal("config")
                                                                .executes(OmniMinerCommand::showConfig))
                                                .then(CommandManager.literal("set")
                                                                .then(CommandManager.literal("maxBlocks")
                                                                                .then(CommandManager.argument("value",
                                                                                                IntegerArgumentType
                                                                                                                .integer(1, 512))
                                                                                                .executes(ctx -> setMaxBlocks(
                                                                                                                ctx,
                                                                                                                IntegerArgumentType
                                                                                                                                .getInteger(ctx, "value")))))
                                                                .then(CommandManager.literal("searchDiagonal")
                                                                                .then(CommandManager.argument("value",
                                                                                                BoolArgumentType.bool())
                                                                                                .executes(ctx -> setSearchDiagonal(
                                                                                                                ctx,
                                                                                                                BoolArgumentType.getBool(
                                                                                                                                ctx,
                                                                                                                                "value")))))
                                                                .then(CommandManager.literal("autoCollect")
                                                                                .then(CommandManager.argument("value",
                                                                                                BoolArgumentType.bool())
                                                                                                .executes(ctx -> setAutoCollect(
                                                                                                                ctx,
                                                                                                                BoolArgumentType.getBool(
                                                                                                                                ctx,
                                                                                                                                "value")))))
                                                                .then(CommandManager.literal("autoCollectExp")
                                                                                .then(CommandManager.argument("value",
                                                                                                BoolArgumentType.bool())
                                                                                                .executes(ctx -> setAutoCollectExp(
                                                                                                                ctx,
                                                                                                                BoolArgumentType.getBool(
                                                                                                                                ctx,
                                                                                                                                "value")))))
                                                                .then(CommandManager.literal("breakLeaves")
                                                                                .then(CommandManager.argument("value",
                                                                                                BoolArgumentType.bool())
                                                                                                .executes(ctx -> setBreakLeaves(
                                                                                                                ctx,
                                                                                                                BoolArgumentType.getBool(
                                                                                                                                ctx,
                                                                                                                                "value")))))
                                                                .then(CommandManager.literal("toggleMode")
                                                                                .then(CommandManager.argument("value",
                                                                                                BoolArgumentType.bool())
                                                                                                .executes(ctx -> setToggleMode(
                                                                                                                ctx,
                                                                                                                BoolArgumentType.getBool(
                                                                                                                                ctx,
                                                                                                                                "value")))))
                                                                .then(CommandManager.literal("bedrockSneakEnable")
                                                                                .then(CommandManager.argument("value",
                                                                                                BoolArgumentType.bool())
                                                                                                .executes(ctx -> setBedrockSneakEnable(
                                                                                                                ctx,
                                                                                                                BoolArgumentType.getBool(
                                                                                                                                ctx,
                                                                                                                                "value")))))
                                                                .then(CommandManager.literal("bedrockShowParticles")
                                                                                .then(CommandManager.argument("value",
                                                                                                BoolArgumentType.bool())
                                                                                                .executes(ctx -> setBedrockShowParticles(
                                                                                                                ctx,
                                                                                                                BoolArgumentType.getBool(
                                                                                                                                ctx,
                                                                                                                                "value")))))
                                                                .then(CommandManager.literal("bedrockParticleMode")
                                                                                .then(CommandManager.argument("value",
                                                                                                IntegerArgumentType
                                                                                                                .integer(0, 1))
                                                                                                .executes(ctx -> setBedrockParticleMode(
                                                                                                                ctx,
                                                                                                                IntegerArgumentType
                                                                                                                                .getInteger(ctx, "value"))))))
                                                .then(CommandManager.literal("addBlock")
                                                                .then(CommandManager
                                                                                .argument("blockId", StringArgumentType
                                                                                                .string())
                                                                                .executes(ctx -> addBlock(ctx,
                                                                                                StringArgumentType
                                                                                                                .getString(ctx, "blockId")))))
                                                .then(CommandManager.literal("removeBlock")
                                                                .then(CommandManager
                                                                                .argument("blockId", StringArgumentType
                                                                                                .string())
                                                                                .executes(ctx -> removeBlock(ctx,
                                                                                                StringArgumentType
                                                                                                                .getString(ctx, "blockId")))))
                                                .then(CommandManager.literal("listBlocks")
                                                                .executes(OmniMinerCommand::listBlocks))
                                                .then(CommandManager.literal("help")
                                                                .executes(OmniMinerCommand::showHelp)));
        }

        private static int showConfig(CommandContext<ServerCommandSource> ctx) {
                ServerCommandSource source = ctx.getSource();

                // プレイヤー情報を取得（final変数として）
                final String playerInfo;
                if (source.getEntity() instanceof ServerPlayerEntity player) {
                        boolean isBedrock = BedrockPlayerUtils.isBedrockPlayer(player);
                        playerInfo = " §7[" + (isBedrock ? "Bedrock" : "Java") + " Edition]";
                } else {
                        playerInfo = "";
                }

                // 設定値をfinal変数にコピー
                final int maxBlocks = Config.maxBlocks;
                final boolean searchDiagonal = Config.searchDiagonal;
                final boolean autoCollect = Config.autoCollect;
                final boolean autoCollectExp = Config.autoCollectExp;
                final boolean breakLeaves = Config.breakLeaves;
                final boolean toggleMode = Config.toggleMode;
                final boolean bedrockSneakEnable = Config.bedrockSneakEnable;
                final boolean bedrockAllowKeyBind = Config.bedrockAllowKeyBind;
                final boolean floodgateAvailable = BedrockPlayerUtils.isFloodgateAvailable();

                source.sendFeedback(() -> Text.literal("§6§l=== OmniMiner Config ===" + playerInfo), false);
                source.sendFeedback(() -> Text.literal("§eMax Blocks: §f" + maxBlocks), false);
                source.sendFeedback(() -> Text.literal("§eSearch Diagonal: §f" + searchDiagonal), false);
                source.sendFeedback(() -> Text.literal("§eAuto Collect: §f" + autoCollect), false);
                source.sendFeedback(() -> Text.literal("§eAuto Collect Exp: §f" + autoCollectExp), false);
                source.sendFeedback(() -> Text.literal("§eBreak Leaves: §f" + breakLeaves), false);
                source.sendFeedback(() -> Text.literal("§eToggle Mode: §f" + toggleMode), false);

                if (floodgateAvailable) {
                        source.sendFeedback(() -> Text.literal("§eBedrock Sneak Enable: §f" + bedrockSneakEnable),
                                        false);
                        source.sendFeedback(() -> Text.literal("§eBedrock Allow KeyBind: §f" + bedrockAllowKeyBind),
                                        false);
                }

                source.sendFeedback(() -> Text.literal("§7Use '/omniminer help' for command list"), false);
                return 1;
        }

        private static int showHelp(CommandContext<ServerCommandSource> ctx) {
                ServerCommandSource source = ctx.getSource();

                final boolean floodgateAvailable = BedrockPlayerUtils.isFloodgateAvailable();

                source.sendFeedback(() -> Text.literal("§6§l=== OmniMiner Commands ==="), false);
                source.sendFeedback(() -> Text.literal("§e/omniminer config §7- Show current config"), false);
                source.sendFeedback(() -> Text.literal("§e/omniminer set maxBlocks <1-512> §7- Set max blocks"), false);
                source.sendFeedback(() -> Text.literal("§e/omniminer set searchDiagonal <true|false>"), false);
                source.sendFeedback(() -> Text.literal("§e/omniminer set autoCollect <true|false>"), false);
                source.sendFeedback(() -> Text.literal("§e/omniminer set autoCollectExp <true|false>"), false);
                source.sendFeedback(() -> Text.literal("§e/omniminer set breakLeaves <true|false>"), false);
                source.sendFeedback(() -> Text.literal("§e/omniminer set toggleMode <true|false>"), false);

                if (floodgateAvailable) {
                        source.sendFeedback(() -> Text.literal("§e/omniminer set bedrockSneakEnable <true|false>"),
                                        false);
                }

                return 1;
        }

        private static int setMaxBlocks(CommandContext<ServerCommandSource> ctx, int value) {
                Config.maxBlocks = value;
                Config.save();
                final int finalValue = value;
                ctx.getSource().sendFeedback(() -> Text.literal("§aMax Blocks set to: §f" + finalValue), false);
                return 1;
        }

        private static int setSearchDiagonal(CommandContext<ServerCommandSource> ctx, boolean value) {
                Config.searchDiagonal = value;
                Config.save();
                final boolean finalValue = value;
                ctx.getSource().sendFeedback(() -> Text.literal("§aSearch Diagonal set to: §f" + finalValue), false);
                return 1;
        }

        private static int setAutoCollect(CommandContext<ServerCommandSource> ctx, boolean value) {
                Config.autoCollect = value;
                Config.save();
                final boolean finalValue = value;
                ctx.getSource().sendFeedback(() -> Text.literal("§aAuto Collect set to: §f" + finalValue), false);
                return 1;
        }

        private static int setAutoCollectExp(CommandContext<ServerCommandSource> ctx, boolean value) {
                Config.autoCollectExp = value;
                Config.save();
                final boolean finalValue = value;
                ctx.getSource().sendFeedback(() -> Text.literal("§aAuto Collect Exp set to: §f" + finalValue), false);
                return 1;
        }

        private static int setBreakLeaves(CommandContext<ServerCommandSource> ctx, boolean value) {
                Config.breakLeaves = value;
                Config.save();
                final boolean finalValue = value;
                ctx.getSource().sendFeedback(() -> Text.literal("§aBreak Leaves set to: §f" + finalValue), false);
                return 1;
        }

        private static int setToggleMode(CommandContext<ServerCommandSource> ctx, boolean value) {
                Config.toggleMode = value;
                Config.save();
                final boolean finalValue = value;
                ctx.getSource().sendFeedback(() -> Text.literal("§aToggle Mode set to: §f" + finalValue), false);
                return 1;
        }

        private static int setBedrockSneakEnable(CommandContext<ServerCommandSource> ctx, boolean value) {
                Config.bedrockSneakEnable = value;
                Config.save();
                final boolean finalValue = value;
                ctx.getSource().sendFeedback(() -> Text.literal("§aBedrock Sneak Enable set to: §f" + finalValue),
                                false);
                return 1;
        }

        private static int setBedrockShowParticles(CommandContext<ServerCommandSource> ctx, boolean value) {
                Config.bedrockShowParticles = value;
                Config.save();
                final boolean finalValue = value;
                ctx.getSource().sendFeedback(() -> Text.literal("§aBedrock Show Particles set to: §f" + finalValue),
                                false);
                return 1;
        }

        private static int setBedrockParticleMode(CommandContext<ServerCommandSource> ctx, int value) {
                Config.bedrockParticleMode = value;
                Config.save();
                final int finalValue = value;
                final String modeName = (value == 1) ? "Detailed" : "Corner";
                ctx.getSource().sendFeedback(() -> Text
                                .literal("§aBedrock Particle Mode set to: §f" + finalValue + " (" + modeName + ")"),
                                false);
                return 1;
        }

        private static int addBlock(CommandContext<ServerCommandSource> ctx, String blockId) {
                if (Config.customBlocks.contains(blockId)) {
                        ctx.getSource().sendFeedback(
                                        () -> Text.literal("§e" + blockId + " §fis already in the custom block list."),
                                        false);
                } else {
                        Config.customBlocks.add(blockId);
                        Config.save();
                        ctx.getSource().sendFeedback(
                                        () -> Text.literal("§aAdded §f" + blockId + " §ato custom block list."), false);
                }
                return 1;
        }

        private static int removeBlock(CommandContext<ServerCommandSource> ctx, String blockId) {
                if (Config.customBlocks.remove(blockId)) {
                        Config.save();
                        ctx.getSource().sendFeedback(
                                        () -> Text.literal("§aRemoved §f" + blockId + " §afrom custom block list."),
                                        false);
                } else {
                        ctx.getSource().sendFeedback(
                                        () -> Text.literal("§e" + blockId + " §fwas not in the custom block list."),
                                        false);
                }
                return 1;
        }

        private static int listBlocks(CommandContext<ServerCommandSource> ctx) {
                if (Config.customBlocks.isEmpty()) {
                        ctx.getSource().sendFeedback(() -> Text.literal(
                                        "§7Custom block list is empty. Use §f/omniminer addBlock <blockId> §7to add blocks."),
                                        false);
                } else {
                        ctx.getSource().sendFeedback(() -> Text
                                        .literal("§aCustom block list (" + Config.customBlocks.size() + " entries):"),
                                        false);
                        for (String id : Config.customBlocks) {
                                ctx.getSource().sendFeedback(() -> Text.literal("  §f- " + id), false);
                        }
                }
                return 1;
        }
}