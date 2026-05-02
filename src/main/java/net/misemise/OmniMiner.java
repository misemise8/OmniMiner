package net.misemise;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.misemise.ClothConfig.Config;
import net.misemise.network.NetworkHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OmniMiner implements ModInitializer {
	public static final String MOD_ID = "omniminer";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("OmniMiner initialized!");

		// 設定を読み込む
		Config.load();

		// コマンドを登録
		net.misemise.command.OmniMinerCommand.register();

		// 統合版プレイヤー向けプレビューシステムを登録
		if (BedrockPlayerUtils.isFloodgateAvailable()) {
			BedrockPreviewSystem.register();
		}

		// Floodgateの状態をログ出力
		if (BedrockPlayerUtils.isFloodgateAvailable()) {
			LOGGER.info("Floodgate detected - Bedrock Edition player support enabled!");
		} else {
			LOGGER.info("Floodgate not detected - Java Edition only mode");
		}

		// ※ クライアント側は ClientModInitializer で registerClient() を呼ぶ想定
		try {
			NetworkHandler.registerServer();
		} catch (Throwable t) {
			LOGGER.warn("NetworkHandler.registerServer() failed or already registered: {}", t.toString());
		}

		// BEFOREイベント：鉱石または原木を適切なツールで壊す場合、標準処理をキャンセルしてMod側で処理
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
			// クライアント側では何もしない
			if (world.isClientSide())
				return true;

			// サーバー側のチェック
			if (!(world instanceof ServerLevel serverWorld))
				return true;
			if (!(player instanceof ServerPlayer serverPlayer))
				return true;

			ItemStack held = serverPlayer.getMainHandItem();

			// ツールの種類を判定
			boolean isPickaxe = ToolUtils.isPickaxe(held);
			boolean isAxe = ToolUtils.isAxe(held);

			// ブロックの種類を判定
			boolean isOre = isPickaxe && OreUtils.isOre(state);
			boolean isLog = isAxe && LogUtils.isLog(state);

			if (isOre || isLog) {
				// プレイヤーが統合版かどうかをチェック
				boolean isBedrockPlayer = BedrockPlayerUtils.isBedrockPlayer(serverPlayer);
				boolean shouldActivate = false;

				if (isBedrockPlayer) {
					// 統合版プレイヤーの場合
					if (Config.debugLog) {
						LOGGER.info("Bedrock player detected: {}",
								BedrockPlayerUtils.getPlayerPlatform(serverPlayer));
					}

					// スニーク（しゃがみ）状態をチェック
					boolean isSneaking = serverPlayer.isShiftKeyDown();

					// 統合版プレイヤーの有効化条件
					if (Config.bedrockSneakEnable && isSneaking) {
						shouldActivate = true;
						if (Config.debugLog) {
							LOGGER.info("Bedrock player sneaking - vein mining activated");
						}
					} else if (Config.bedrockAllowKeyBind &&
							NetworkHandler.isKeyPressed(serverPlayer.getUUID())) {
						// キーバインドも許可されている場合
						shouldActivate = true;
						if (Config.debugLog) {
							LOGGER.info("Bedrock player using keybind - vein mining activated");
						}
					}

					// 統合版プレイヤーにパーティクルでプレビュー表示（破壊時のみ）
					// 注：しゃがみ中のプレビューは BedrockPreviewSystem が担当
					if (shouldActivate && Config.bedrockShowParticles) {
						try {
							java.util.Set<net.minecraft.core.BlockPos> connectedBlocks = findConnectedBlocks(
									serverWorld, pos, state);

							if (Config.bedrockParticleMode == 1) {
								BedrockVisualHelper.showDetailedParticleOutline(serverWorld, connectedBlocks,
										serverPlayer);
							} else {
								BedrockVisualHelper.showParticleOutline(serverWorld, connectedBlocks, serverPlayer);
							}
						} catch (Exception e) {
							LOGGER.warn("Failed to show particle preview for Bedrock player", e);
						}
					}
				} else {
					// Java版プレイヤーの場合：通常のキー押下チェック
					shouldActivate = NetworkHandler.isKeyPressed(serverPlayer.getUUID());
				}

				if (!shouldActivate) {
					// 有効化条件を満たしていない場合は通常処理
					return true;
				}

				String blockType = isOre ? "ore" : "log";
				String playerType = isBedrockPlayer ? "Bedrock" : "Java";
				LOGGER.info("Vein mining {} triggered at {} by {} player {}",
						blockType, pos, playerType, serverPlayer.getName().getString());

				// Mod側で一括破壊を実行
				OreBreaker.breakConnectedOres(serverWorld, pos, state, serverPlayer, held);

				// 標準のブロック破壊処理をキャンセル
				return false;
			}

			// 通常通り処理
			return true;
		});
	}

	/**
	 * 接続されたブロックを探す（プレビュー用）
	 */
	private static java.util.Set<net.minecraft.core.BlockPos> findConnectedBlocks(
			ServerLevel world, net.minecraft.core.BlockPos startPos, net.minecraft.world.level.block.state.BlockState targetState) {
		java.util.Set<net.minecraft.core.BlockPos> visited = new java.util.HashSet<>();
		dfsPreview(world, startPos, targetState, visited);
		return visited;
	}

	private static void dfsPreview(ServerLevel world, net.minecraft.core.BlockPos pos,
			net.minecraft.world.level.block.state.BlockState targetState, java.util.Set<net.minecraft.core.BlockPos> visited) {
		if (visited.size() >= Config.maxBlocks || visited.contains(pos)) {
			return;
		}

		net.minecraft.world.level.block.state.BlockState currentState = world.getBlockState(pos);
		if (!currentState.is(targetState.getBlock())) {
			return;
		}

		visited.add(pos);

		if (Config.searchDiagonal) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0)
							continue;
						dfsPreview(world, pos.offset(dx, dy, dz), targetState, visited);
					}
				}
			}
		} else {
			net.minecraft.core.BlockPos[] neighbors = {
					pos.above(), pos.below(), pos.north(), pos.south(), pos.east(), pos.west()
			};
			for (net.minecraft.core.BlockPos neighbor : neighbors) {
				dfsPreview(world, neighbor, targetState, visited);
			}
		}
	}
}