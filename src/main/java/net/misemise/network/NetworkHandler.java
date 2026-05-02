package net.misemise.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.misemise.OmniMiner;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NetworkHandler {
    public static final Identifier VEIN_MINER_KEY_STATE_ID =
            Identifier.fromNamespaceAndPath(OmniMiner.MOD_ID, "vein_miner_key_state");

    private static final Map<UUID, Boolean> playerKeyStates = new HashMap<>();
    private static boolean payloadsRegistered = false;
    private static boolean serverRegistered = false;
    private static boolean clientRegistered = false;

    public record VeinMinerKeyStatePayload(boolean isPressed) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<VeinMinerKeyStatePayload> ID =
                new CustomPacketPayload.Type<>(VEIN_MINER_KEY_STATE_ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, VeinMinerKeyStatePayload> CODEC =
                ByteBufCodecs.BOOL.map(VeinMinerKeyStatePayload::new, VeinMinerKeyStatePayload::isPressed).cast();

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }

    public record BlocksMinedCountPayload(int count, String blockType) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<BlocksMinedCountPayload> ID =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(OmniMiner.MOD_ID, "blocks_mined_count"));

        public static final StreamCodec<RegistryFriendlyByteBuf, BlocksMinedCountPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, BlocksMinedCountPayload::count,
                ByteBufCodecs.STRING_UTF8, BlocksMinedCountPayload::blockType,
                BlocksMinedCountPayload::new
        );

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

        try {
            ServerPlayNetworking.registerGlobalReceiver(VeinMinerKeyStatePayload.ID, (payload, context) -> {
                context.server().execute(() ->
                        playerKeyStates.put(context.player().getUUID(), payload.isPressed()));
            });

            ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                    clearPlayerState(handler.getPlayer().getUUID()));

            serverRegistered = true;
        } catch (IllegalArgumentException e) {
            serverRegistered = true;
        }
    }

    public static void registerClient() {
        registerPayloads();

        if (clientRegistered) {
            return;
        }

        ClientPlayNetworking.registerGlobalReceiver(BlocksMinedCountPayload.ID, (payload, context) -> {
            context.client().execute(() ->
                    net.misemise.client.VeinMiningHud.setBlocksMinedCount(payload.count(), payload.blockType()));
        });

        clientRegistered = true;
    }

    private static void registerPayloads() {
        if (payloadsRegistered) {
            return;
        }

        try {
            PayloadTypeRegistry.serverboundPlay().register(VeinMinerKeyStatePayload.ID, VeinMinerKeyStatePayload.CODEC);
            PayloadTypeRegistry.clientboundPlay().register(BlocksMinedCountPayload.ID, BlocksMinedCountPayload.CODEC);
        } catch (IllegalArgumentException ignored) {
            // Another initializer path may already have registered the payload types.
        }

        payloadsRegistered = true;
    }

    public static void sendKeyState(boolean isPressed) {
        if (ClientPlayNetworking.canSend(VeinMinerKeyStatePayload.ID)) {
            ClientPlayNetworking.send(new VeinMinerKeyStatePayload(isPressed));
        }
    }

    public static boolean isKeyPressed(UUID playerId) {
        return playerKeyStates.getOrDefault(playerId, false);
    }

    public static void clearPlayerState(UUID playerId) {
        playerKeyStates.remove(playerId);
    }

    public static void sendBlocksMinedCount(ServerPlayer player, int count, String blockType) {
        ServerPlayNetworking.send(player, new BlocksMinedCountPayload(count, blockType));
    }
}
