package net.misemise.client;

import net.misemise.ClothConfig.Config;

/**
 * Server-authoritative mining settings used by the client preview.
 */
public final class ClientMiningConfig {
    private static boolean synchronizedWithServer;
    private static int maxBlocks;
    private static boolean searchDiagonal;
    private static boolean breakLeaves;
    private static boolean includeBlockEntities;

    private ClientMiningConfig() {
    }

    public static void apply(
            int serverMaxBlocks,
            boolean serverSearchDiagonal,
            boolean serverBreakLeaves,
            boolean serverIncludeBlockEntities) {
        maxBlocks = Math.max(1, Math.min(512, serverMaxBlocks));
        searchDiagonal = serverSearchDiagonal;
        breakLeaves = serverBreakLeaves;
        includeBlockEntities = serverIncludeBlockEntities;
        synchronizedWithServer = true;
    }

    public static void reset() {
        synchronizedWithServer = false;
    }

    public static boolean isSynchronizedWithServer() {
        return synchronizedWithServer;
    }

    public static int maxBlocks() {
        return synchronizedWithServer ? maxBlocks : Config.maxBlocks;
    }

    public static boolean searchDiagonal() {
        return synchronizedWithServer ? searchDiagonal : Config.searchDiagonal;
    }

    public static boolean breakLeaves() {
        return synchronizedWithServer ? breakLeaves : Config.breakLeaves;
    }

    public static boolean includeBlockEntities() {
        return synchronizedWithServer ? includeBlockEntities : Config.includeBlockEntities;
    }
}
