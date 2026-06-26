package net.supersnetwork.fabric_utility.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.supersnetwork.fabric_utility.FabricUtilityConfigNetworking;
import net.supersnetwork.fabric_utility.StasisHandler;
import net.supersnetwork.fabric_utility.VersionCompatibility;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class FabricUtilityClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FabricUtilityConfigNetworking.SYNC_CONFIG_ID, (payload, context) -> {
            context.client().execute(() -> {
                FabricUtilityClientConfig.update(payload.values(), payload.canEdit());
                if (context.client().currentScreen instanceof FabricUtilityConfigScreen screen) {
                    screen.refreshFromServer();
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(StasisHandler.SYNC_STASIS_ID, (payload, context) ->
                context.client().execute(() -> StasisClientState.setLocked(context.client(), payload.locked())));

        ClientPlayNetworking.registerGlobalReceiver(VersionCompatibility.VERSION_INFO_ID, (payload, context) -> {
            context.client().execute(() -> {
                if (VersionCompatibility.compareVersions(VersionCompatibility.VERSION, payload.version()) < 0
                        && context.client().player != null) {
                    context.client().player.sendMessage(Text.literal(
                            "Your Fabric Utility client (" + VersionCompatibility.VERSION + ") is older than this server ("
                                    + payload.version() + "). Please update your mod."
                    ).formatted(Formatting.RED), false);
                }
            });
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (ClientPlayNetworking.canSend(VersionCompatibility.VERSION_INFO_ID)) {
                ClientPlayNetworking.send(new VersionCompatibility.VersionInfoPayload(VersionCompatibility.VERSION));
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            FabricUtilityClientConfig.clearServer();
            StasisClientState.clear();
        });
    }
}
