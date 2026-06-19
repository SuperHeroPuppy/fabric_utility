package net.supersnetwork.fabric_utility;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FabricUtilityConfigNetworking {
    public static final Identifier REQUEST_CONFIG = new Identifier(FabricUtility.MOD_ID, "request_config");
    public static final Identifier UPDATE_CONFIG = new Identifier(FabricUtility.MOD_ID, "update_config");
    public static final Identifier SYNC_CONFIG = new Identifier(FabricUtility.MOD_ID, "sync_config");

    private FabricUtilityConfigNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_CONFIG, (server, player, handler, buf, responseSender) ->
                server.execute(() -> sendSnapshot(player)));

        ServerPlayNetworking.registerGlobalReceiver(UPDATE_CONFIG, (server, player, handler, buf, responseSender) -> {
            Map<String, String> requestedValues = readValues(buf);
            server.execute(() -> applyUpdate(server, player, requestedValues));
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendSnapshot(handler.player));
    }

    public static void sendSnapshot(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, SYNC_CONFIG)) {
            return;
        }

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(player.hasPermissionLevel(2));
        writeValues(buf, FabricUtilityConfig.values());
        ServerPlayNetworking.send(player, SYNC_CONFIG, buf);
    }

    public static void broadcast(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendSnapshot(player);
        }
    }

    public static void writeValues(PacketByteBuf buf, Map<String, String> values) {
        buf.writeVarInt(values.size());
        values.forEach((key, value) -> {
            buf.writeString(key, 128);
            buf.writeString(value, 32767);
        });
    }

    public static Map<String, String> readValues(PacketByteBuf buf) {
        int size = Math.min(buf.readVarInt(), FabricUtilityConfig.CONFIG_KEYS.size());
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            values.put(buf.readString(128), buf.readString(32767));
        }
        return values;
    }

    private static void applyUpdate(MinecraftServer server, ServerPlayerEntity player, Map<String, String> values) {
        if (!player.hasPermissionLevel(2)) {
            player.sendMessage(Text.literal("You do not have permission to edit the server's Fabric Utility config."), false);
            sendSnapshot(player);
            return;
        }

        if (!FabricUtilityConfig.setValues(values)) {
            player.sendMessage(Text.literal("The config update contained an unknown key and was rejected."), false);
            sendSnapshot(player);
            return;
        }

        player.sendMessage(Text.literal("Fabric Utility server config updated."), false);
        broadcast(server);
    }
}
