package net.supersnetwork.fabric_utility.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.supersnetwork.fabric_utility.FabricUtilityConfigNetworking;
import net.supersnetwork.fabric_utility.StasisHandler;
import net.supersnetwork.fabric_utility.VersionCompatibility;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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

        ClientPlayNetworking.registerGlobalReceiver(StasisHandler.SYNC_STASIS, (client, handler, buf, responseSender) -> {
            boolean locked = buf.readBoolean();
            client.execute(() -> StasisClientState.setLocked(client, locked));
        });

        ClientPlayNetworking.registerGlobalReceiver(VersionCompatibility.VERSION_INFO, (client, handler, buf, responseSender) -> {
            String serverVersion = buf.readString(128);
            client.execute(() -> {
                if (VersionCompatibility.compareVersions(VersionCompatibility.VERSION, serverVersion) < 0
                        && client.player != null) {
                    client.player.sendMessage(Text.literal(
                            "Your Fabric Utility client (" + VersionCompatibility.VERSION + ") is older than this server ("
                                    + serverVersion + "). Please update your mod."
                    ).formatted(Formatting.RED), false);
                }
            });
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (ClientPlayNetworking.canSend(VersionCompatibility.VERSION_INFO)) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeString(VersionCompatibility.VERSION, 128);
                ClientPlayNetworking.send(VersionCompatibility.VERSION_INFO, buf);
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            FabricUtilityClientConfig.clearServer();
            StasisClientState.clear();
        });
    }
}
