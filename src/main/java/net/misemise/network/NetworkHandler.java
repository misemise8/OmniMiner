package net.misemise.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
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
            Identifier.of(OmniMiner.MOD_ID, "vein_miner_key_state");

    private static final Map<UUID, Boolean> PLAYER_KEY_STATES = new HashMap<>();
    private static boolean payloadsRegistered;
    private static boolean serverRegistered;
    private static boolean clientRegistered;

    private NetworkHandler() {
    }

    public record VeinMinerKeyStatePayload(boolean isPressed) implements CustomPayload {
        public static final CustomPayload.Id<VeinMinerKeyStatePayload> ID =
                new CustomPayload.Id<>(VEIN_MINER_KEY_STATE_ID);
        public static final PacketCodec<RegistryByteBuf, VeinMinerKeyStatePayload> CODEC =
                PacketCodecs.BOOL.xmap(VeinMinerKeyStatePayload::new, VeinMinerKeyStatePayload::isPressed).cast();

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record BlocksMinedCountPayload(
            int count,
            String blockType) implements CustomPayload {
        public static final CustomPayload.Id<BlocksMinedCountPayload> ID =
                new CustomPayload.Id<>(
                        Identifier.of(OmniMiner.MOD_ID, "blocks_mined_count"));
        public static final PacketCodec<RegistryByteBuf, BlocksMinedCountPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.INTEGER, BlocksMinedCountPayload::count,
                        PacketCodecs.STRING, BlocksMinedCountPayload::blockType,
                        BlocksMinedCountPayload::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record MiningConfigPayload(
            int maxBlocks,
            boolean searchDiagonal,
            boolean breakLeaves,
            boolean includeBlockEntities) implements CustomPayload {
        public static final CustomPayload.Id<MiningConfigPayload> ID =
                new CustomPayload.Id<>(
                        Identifier.of(OmniMiner.MOD_ID, "mining_config"));
        public static final PacketCodec<RegistryByteBuf, MiningConfigPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.INTEGER, MiningConfigPayload::maxBlocks,
                        PacketCodecs.BOOL, MiningConfigPayload::searchDiagonal,
                        PacketCodecs.BOOL, MiningConfigPayload::breakLeaves,
                        PacketCodecs.BOOL, MiningConfigPayload::includeBlockEntities,
                        MiningConfigPayload::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record MiningConfigRequestPayload(boolean requested) implements CustomPayload {
        public static final CustomPayload.Id<MiningConfigRequestPayload> ID =
                new CustomPayload.Id<>(
                        Identifier.of(OmniMiner.MOD_ID, "mining_config_request"));
        public static final PacketCodec<RegistryByteBuf, MiningConfigRequestPayload> CODEC =
                PacketCodecs.BOOL
                        .xmap(MiningConfigRequestPayload::new,
                                MiningConfigRequestPayload::requested)
                        .cast();

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void registerServer() {
        registerPayloads();
        if (serverRegistered) {
            return;
        }

        ServerPlayNetworking.registerGlobalReceiver(
                VeinMinerKeyStatePayload.ID,
                (payload, context) -> context.server().execute(() ->
                        PLAYER_KEY_STATES.put(
                                context.player().getUuid(),
                                payload.isPressed())));
        ServerPlayNetworking.registerGlobalReceiver(
                MiningConfigRequestPayload.ID,
                (payload, context) -> context.server().execute(() ->
                        sendMiningConfig(context.player())));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                sendMiningConfig(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerId = handler.getPlayer().getUuid();
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

        ClientPlayNetworking.registerGlobalReceiver(
                BlocksMinedCountPayload.ID,
                (payload, context) -> context.client().execute(() ->
                        VeinMiningHud.setBlocksMinedCount(
                                payload.count(),
                                payload.blockType())));
        ClientPlayNetworking.registerGlobalReceiver(
                MiningConfigPayload.ID,
                (payload, context) -> context.client().execute(() ->
                        ClientMiningConfig.apply(
                                payload.maxBlocks(),
                                payload.searchDiagonal(),
                                payload.breakLeaves(),
                                payload.includeBlockEntities())));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                client.execute(() -> {
                    ClientMiningConfig.reset();
                    KeyStateTracker.resetConnectionState();
                    requestMiningConfig();
                }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                client.execute(() -> {
                    ClientMiningConfig.reset();
                    KeyStateTracker.resetConnectionState();
                }));
        clientRegistered = true;
    }

    private static void registerPayloads() {
        if (payloadsRegistered) {
            return;
        }

        PayloadTypeRegistry.playC2S()
                .register(VeinMinerKeyStatePayload.ID, VeinMinerKeyStatePayload.CODEC);
        PayloadTypeRegistry.playC2S()
                .register(MiningConfigRequestPayload.ID, MiningConfigRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C()
                .register(BlocksMinedCountPayload.ID, BlocksMinedCountPayload.CODEC);
        PayloadTypeRegistry.playS2C()
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

    public static void sendBlocksMinedCount(
            ServerPlayerEntity player,
            int count,
            String blockType) {
        if (ServerPlayNetworking.canSend(player, BlocksMinedCountPayload.ID)) {
            ServerPlayNetworking.send(
                    player,
                    new BlocksMinedCountPayload(count, blockType));
        }
    }
    public static void broadcastMiningConfig(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendMiningConfig(player);
        }
    }

    private static void sendMiningConfig(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, MiningConfigPayload.ID)) {
            return;
        }

        ServerPlayNetworking.send(
                player,
                new MiningConfigPayload(
                        Config.maxBlocks,
                        Config.searchDiagonal,
                        Config.breakLeaves,
                        Config.includeBlockEntities));
    }
}
