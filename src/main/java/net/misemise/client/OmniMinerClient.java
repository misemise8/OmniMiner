package net.misemise.client;

import net.fabricmc.api.ClientModInitializer;
import net.misemise.OmniMiner;
import net.misemise.keybind.KeyBindings;
import net.misemise.keybind.KeyStateTracker;
import net.misemise.network.NetworkHandler;

public class OmniMinerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OmniMiner.LOGGER.info("OmniMinerClient initializing...");

        // クライアント側のネットワーク登録
        NetworkHandler.registerClient();

        // キーバインドを登録
        KeyBindings.register();

        // キー状態トラッカーを登録
        KeyStateTracker.register();

        // HUDを登録
        VeinMiningHud.register();
        OmniMiner.LOGGER.info("VeinMiningHud registered");

        // ブロックハイライトレンダラーを登録
        BlockHighlightRenderer.register();
        OmniMiner.LOGGER.info("BlockHighlightRenderer registered");
    }
}