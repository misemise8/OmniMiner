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
     * サーバー側のネットワーク登録
     */
    public static void registerServer() {
        if (registered) {
            OmniMiner.LOGGER.info("Network handler already registered (server side)");
            return;
        }

        try {
            PayloadTypeRegistry.playC2S().register(VeinMinerKeyStatePayload.ID, VeinMinerKeyStatePayload.CODEC);

            // ブロック破壊数パケット登録（サーバーからクライアントへ）
            PayloadTypeRegistry.playS2C().register(BlocksMinedCountPayload.ID, BlocksMinedCountPayload.CODEC);

            ServerPlayNetworking.registerGlobalReceiver(VeinMinerKeyStatePayload.ID, (payload, context) -> {
                context.server().execute(() -> {
                    UUID playerId = context.player().getUuid();
                    playerKeyStates.put(playerId, payload.isPressed());
                    OmniMiner.LOGGER.info("Received key state from player {}: {}",
                            context.player().getName().getString(), payload.isPressed());
                });
            });

            registered = true;
            OmniMiner.LOGGER.info("Server network handler registered");
        } catch (IllegalArgumentException e) {
            OmniMiner.LOGGER.warn("Packet type already registered, skipping: {}", e.getMessage());
            registered = true;
        }


    }

    /**
     * クライアント側のネットワーク登録
     */
    /**
     * クライアント側のネットワーク登録
     */
    public static void registerClient() {
        OmniMiner.LOGGER.info("Client network handler initializing...");

        // ブロック破壊数パケットの受信処理を登録
        ClientPlayNetworking.registerGlobalReceiver(BlocksMinedCountPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                OmniMiner.LOGGER.info("Received blocks mined count: {}", payload.count());
                // VeinMiningHudに破壊数を設定
                net.misemise.client.VeinMiningHud.setBlocksMinedCount(payload.count());
                OmniMiner.LOGGER.info("Set HUD blocks mined count to: {}", payload.count());
            });
        });

        OmniMiner.LOGGER.info("Client network handler registered");
    }

    /**
     * キー状態をサーバーに送信
     */
    public static void sendKeyState(boolean isPressed) {
        if (ClientPlayNetworking.canSend(VeinMinerKeyStatePayload.ID)) {
            ClientPlayNetworking.send(new VeinMinerKeyStatePayload(isPressed));
            OmniMiner.LOGGER.info("Sent key state to server: {}", isPressed);
        }
    }

    /**
     * プレイヤーがキーを押しているかチェック
     */
    public static boolean isKeyPressed(UUID playerId) {
        boolean pressed = playerKeyStates.getOrDefault(playerId, false);
        OmniMiner.LOGGER.info("Checking key state for player {}: {}", playerId, pressed);
        return pressed;
    }

    /**
     * プレイヤーの状態をクリア（ログアウト時など）
     */
    public static void clearPlayerState(UUID playerId) {
        playerKeyStates.remove(playerId);
    }

    public record BlocksMinedCountPayload(int count) implements CustomPayload {
        public static final CustomPayload.Id<BlocksMinedCountPayload> ID =
                new CustomPayload.Id<>(Identifier.of(OmniMiner.MOD_ID, "blocks_mined_count"));

        public static final PacketCodec<RegistryByteBuf, BlocksMinedCountPayload> CODEC =
                PacketCodecs.INTEGER.xmap(BlocksMinedCountPayload::new, BlocksMinedCountPayload::count).cast();

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * 破壊したブロック数をクライアントに送信
     */
    public static void sendBlocksMinedCount(ServerPlayerEntity player, int count) {
        ServerPlayNetworking.send(player, new BlocksMinedCountPayload(count));
        OmniMiner.LOGGER.info("Sent blocks mined count to client: {}", count);
    }
}