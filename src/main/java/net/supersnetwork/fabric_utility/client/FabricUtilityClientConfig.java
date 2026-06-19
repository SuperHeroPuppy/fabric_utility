package net.supersnetwork.fabric_utility.client;

import net.supersnetwork.fabric_utility.FabricUtilityConfig;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FabricUtilityClientConfig {
    private static final Map<String, String> SERVER_VALUES = new LinkedHashMap<>();
    private static boolean connectedToModdedServer;
    private static boolean canEditServer;

    private FabricUtilityClientConfig() {
    }

    public static synchronized void update(Map<String, String> values, boolean canEdit) {
        SERVER_VALUES.clear();
        SERVER_VALUES.putAll(values);
        connectedToModdedServer = true;
        canEditServer = canEdit;
    }

    public static synchronized void clearServer() {
        SERVER_VALUES.clear();
        connectedToModdedServer = false;
        canEditServer = false;
    }

    public static synchronized String getValue(String key) {
        return connectedToModdedServer ? SERVER_VALUES.getOrDefault(key, "") : FabricUtilityConfig.getValue(key);
    }

    public static synchronized boolean isConnectedToModdedServer() {
        return connectedToModdedServer;
    }

    public static synchronized boolean canEditServer() {
        return canEditServer;
    }
}
