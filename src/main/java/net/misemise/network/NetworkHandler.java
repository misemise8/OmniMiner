package net.misemise.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.misemise.OmniMiner;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * クライアント-サーバー間のネットワーク通信を管理
 */
public class NetworkHandler {
    public static final Identifier VEIN_MINER_KEY_STATE_ID = Identifier.of(OmniMiner.MOD_ID, "vein_miner_key_state");

    // プレイヤーごとのキー押下状態を保存
    private static final Map<UUID, Boolean> playerKeyStates = new HashMap<>();

    // 登録済みフラグ
    private static boolean registered = false;

    /**
     * キー状態パケット
     */
    public record VeinMinerKeyStatePayload(boolean isPressed) implements CustomPayload {
        public static final CustomPayload.Id<VeinMinerKeyStatePayload> ID =
                new CustomPayload.Id<>(VEIN_MINER_KEY_STATE_ID);

        public static final PacketCodec<RegistryByteBuf, VeinMinerKeyStatePayload> CODEC =
                PacketCodecs.BOOLEAN.xmap(VeinMinerKeyStatePayload::new, VeinMinerKeyStatePayload::isPressed).cast();

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * ブロック破壊数パケット（ブロックタイプ付き）
     */
    public record BlocksMinedCountPayload(int count, String blockType) implements CustomPayload {
        public static final CustomPayload.Id<BlocksMinedCountPayload> ID =
                new CustomPayload.Id<>(Identifier.of(OmniMiner.MOD_ID, "blocks_mined_count"));

        public static final PacketCodec<RegistryByteBuf, BlocksMinedCountPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, BlocksMinedCountPayload::count,
                PacketCodecs.STRING, BlocksMinedCountPayload::blockType,
                BlocksMinedCountPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * サーバー側のネットワーク登録
     */
    public static void registerServer() {
        if (registered) {
            return;
        }

        try {
            PayloadTypeRegistry.playC2S().register(VeinMinerKeyStatePayload.ID, VeinMinerKeyStatePayload.CODEC);
            PayloadTypeRegistry.playS2C().register(BlocksMinedCountPayload.ID, BlocksMinedCountPayload.CODEC);

            ServerPlayNetworking.registerGlobalReceiver(VeinMinerKeyStatePayload.ID, (payload, context) -> {
                context.server().execute(() -> {
                    UUID playerId = context.player().getUuid();
                    playerKeyStates.put(playerId, payload.isPressed());
                });
            });

            registered = true;
        } catch (IllegalArgumentException e) {
            registered = true;
        }
    }

    /**
     * クライアント側のネットワーク登録
     */
    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(BlocksMinedCountPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                net.misemise.client.VeinMiningHud.setBlocksMinedCount(payload.count(), payload.blockType());
            });
        });
    }

    /**
     * キー状態をサーバーに送信
     */
    public static void sendKeyState(boolean isPressed) {
        if (ClientPlayNetworking.canSend(VeinMinerKeyStatePayload.ID)) {
            ClientPlayNetworking.send(new VeinMinerKeyStatePayload(isPressed));
        }
    }

    /**
     * プレイヤーがキーを押しているかチェック
     */
    public static boolean isKeyPressed(UUID playerId) {
        return playerKeyStates.getOrDefault(playerId, false);
    }

    /**
     * プレイヤーの状態をクリア（ログアウト時など）
     */
    public static void clearPlayerState(UUID playerId) {
        playerKeyStates.remove(playerId);
    }

    /**
     * 破壊したブロック数をクライアントに送信（ブロックタイプ付き）
     */
    public static void sendBlocksMinedCount(ServerPlayerEntity player, int count, String blockType) {
        ServerPlayNetworking.send(player, new BlocksMinedCountPayload(count, blockType));
    }
}