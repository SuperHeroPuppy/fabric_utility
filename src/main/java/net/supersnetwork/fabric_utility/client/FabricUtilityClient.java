package net.supersnetwork.fabric_utility.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.supersnetwork.fabric_utility.FabricUtilityConfigNetworking;

import java.util.Map;

public final class FabricUtilityClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FabricUtilityConfigNetworking.SYNC_CONFIG, (client, handler, buf, responseSender) -> {
            boolean canEdit = buf.readBoolean();
            Map<String, String> values = FabricUtilityConfigNetworking.readValues(buf);
            client.execute(() -> {
                FabricUtilityClientConfig.update(values, canEdit);
                if (client.currentScreen instanceof FabricUtilityConfigScreen screen) {
                    screen.refreshFromServer();
                }
            });
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> FabricUtilityClientConfig.clearServer());
    }
}
