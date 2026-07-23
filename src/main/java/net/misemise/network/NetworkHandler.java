package net.misemise.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.misemise.BedrockPreviewSystem;
import net.misemise.ClothConfig.Config;
import net.misemise.OmniMiner;
import net.misemise.OreBreaker;
import net.misemise.client.ClientMiningConfig;
import net.misemise.client.VeinMiningHud;
import net.misemise.keybind.KeyStateTracker;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NetworkHandler {
    public static final Identifier VEIN_MINER_KEY_STATE_ID =
            Identifier.fromNamespaceAndPath(OmniMiner.MOD_ID, "vein_miner_key_state");

    private static final Map<UUID, Boolean> PLAYER_KEY_STATES = new HashMap<>();
    private static boolean payloadsRegistered;
    private static boolean serverRegistered;
    private static boolean clientRegistered;

    private NetworkHandler() {
    }

    public record VeinMinerKeyStatePayload(boolean isPressed) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<VeinMinerKeyStatePayload> ID =
                new CustomPacketPayload.Type<>(VEIN_MINER_KEY_STATE_ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, VeinMinerKeyStatePayload> CODEC =
                ByteBufCodecs.BOOL
                        .map(VeinMinerKeyStatePayload::new, VeinMinerKeyStatePayload::isPressed)
                        .cast();

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record BlocksMinedCountPayload(int count, String blockType) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BlocksMinedCountPayload> ID =
                new CustomPacketPayload.Type<>(
                        Identifier.fromNamespaceAndPath(OmniMiner.MOD_ID, "blocks_mined_count"));

        public static final StreamCodec<RegistryFriendlyByteBuf, BlocksMinedCountPayload> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.INT, BlocksMinedCountPayload::count,
                        ByteBufCodecs.STRING_UTF8, BlocksMinedCountPayload::blockType,
                        BlocksMinedCountPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record MiningConfigPayload(
            int maxBlocks,
            boolean searchDiagonal,
            boolean breakLeaves,
            boolean includeBlockEntities) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<MiningConfigPayload> ID =
                new CustomPacketPayload.Type<>(
                        Identifier.fromNamespaceAndPath(OmniMiner.MOD_ID, "mining_config"));

        public static final StreamCodec<RegistryFriendlyByteBuf, MiningConfigPayload> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.INT, MiningConfigPayload::maxBlocks,
                        ByteBufCodecs.BOOL, MiningConfigPayload::searchDiagonal,
                        ByteBufCodecs.BOOL, MiningConfigPayload::breakLeaves,
                        ByteBufCodecs.BOOL, MiningConfigPayload::includeBlockEntities,
                        MiningConfigPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record MiningConfigRequestPayload(boolean requested) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<MiningConfigRequestPayload> ID =
                new CustomPacketPayload.Type<>(
                        Identifier.fromNamespaceAndPath(
                                OmniMiner.MOD_ID,
                                "mining_config_request"));

        public static final StreamCodec<RegistryFriendlyByteBuf, MiningConfigRequestPayload> CODEC =
                ByteBufCodecs.BOOL
                        .map(MiningConfigRequestPayload::new,
                                MiningConfigRequestPayload::requested)
                        .cast();

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public static void registerServer() {
        registerPayloads();
        if (serverRegistered) {
            return;
        }

        ServerPlayNetworking.registerGlobalReceiver(VeinMinerKeyStatePayload.ID, (payload, context) ->
                context.server().execute(() ->
                        PLAYER_KEY_STATES.put(context.player().getUUID(), payload.isPressed())));
        ServerPlayNetworking.registerGlobalReceiver(
                MiningConfigRequestPayload.ID,
                (payload, context) -> context.server().execute(() ->
                        sendMiningConfig(context.player())));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                sendMiningConfig(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerId = handler.getPlayer().getUUID();
            clearPlayerState(playerId);
            OreBreaker.clearPlayerState(playerId);
            BedrockPreviewSystem.clearPlayer(playerId);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PLAYER_KEY_STATES.clear());

        serverRegistered = true;
    }

    public static void registerClient() {
        registerPayloads();
        if (clientRegistered) {
            return;
        }

        ClientPlayNetworking.registerGlobalReceiver(BlocksMinedCountPayload.ID, (payload, context) ->
                context.client().execute(() ->
                        VeinMiningHud.setBlocksMinedCount(payload.count(), payload.blockType())));

        ClientPlayNetworking.registerGlobalReceiver(MiningConfigPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientMiningConfig.apply(
                        payload.maxBlocks(),
                        payload.searchDiagonal(),
                        payload.breakLeaves(),
                        payload.includeBlockEntities())));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
            ClientMiningConfig.reset();
            KeyStateTracker.resetConnectionState();
            requestMiningConfig();
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            ClientMiningConfig.reset();
            KeyStateTracker.resetConnectionState();
        }));

        clientRegistered = true;
    }

    private static void registerPayloads() {
        if (payloadsRegistered) {
            return;
        }

        PayloadTypeRegistry.serverboundPlay()
                .register(VeinMinerKeyStatePayload.ID, VeinMinerKeyStatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay()
                .register(MiningConfigRequestPayload.ID, MiningConfigRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay()
                .register(BlocksMinedCountPayload.ID, BlocksMinedCountPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay()
                .register(MiningConfigPayload.ID, MiningConfigPayload.CODEC);
        payloadsRegistered = true;
    }

    public static void sendKeyState(boolean isPressed) {
        if (ClientPlayNetworking.canSend(VeinMinerKeyStatePayload.ID)) {
            ClientPlayNetworking.send(new VeinMinerKeyStatePayload(isPressed));
        }
    }

    public static boolean canSendKeyState() {
        return ClientPlayNetworking.canSend(VeinMinerKeyStatePayload.ID);
    }

    private static void requestMiningConfig() {
        if (ClientPlayNetworking.canSend(MiningConfigRequestPayload.ID)) {
            ClientPlayNetworking.send(new MiningConfigRequestPayload(true));
        }
    }

    public static boolean isKeyPressed(UUID playerId) {
        return PLAYER_KEY_STATES.getOrDefault(playerId, false);
    }

    public static void clearPlayerState(UUID playerId) {
        PLAYER_KEY_STATES.remove(playerId);
    }

    public static void sendBlocksMinedCount(ServerPlayer player, int count, String blockType) {
        if (ServerPlayNetworking.canSend(player, BlocksMinedCountPayload.ID)) {
            ServerPlayNetworking.send(player, new BlocksMinedCountPayload(count, blockType));
        }
    }

    public static void broadcastMiningConfig(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendMiningConfig(player);
        }
    }

    private static void sendMiningConfig(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, MiningConfigPayload.ID)) {
            return;
        }

        ServerPlayNetworking.send(player, new MiningConfigPayload(
                Config.maxBlocks,
                Config.searchDiagonal,
                Config.breakLeaves,
                Config.includeBlockEntities));
    }
}
