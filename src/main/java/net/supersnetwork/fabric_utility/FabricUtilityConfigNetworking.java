package net.supersnetwork.fabric_utility;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FabricUtilityConfigNetworking {
    public static final Identifier REQUEST_CONFIG = Identifier.of(FabricUtility.MOD_ID, "request_config");
    public static final Identifier UPDATE_CONFIG = Identifier.of(FabricUtility.MOD_ID, "update_config");
    public static final Identifier SYNC_CONFIG = Identifier.of(FabricUtility.MOD_ID, "sync_config");
    public static final CustomPayload.Id<RequestConfigPayload> REQUEST_CONFIG_ID = new CustomPayload.Id<>(REQUEST_CONFIG);
    public static final CustomPayload.Id<UpdateConfigPayload> UPDATE_CONFIG_ID = new CustomPayload.Id<>(UPDATE_CONFIG);
    public static final CustomPayload.Id<SyncConfigPayload> SYNC_CONFIG_ID = new CustomPayload.Id<>(SYNC_CONFIG);

    private FabricUtilityConfigNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(REQUEST_CONFIG_ID, RequestConfigPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UPDATE_CONFIG_ID, UpdateConfigPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SYNC_CONFIG_ID, SyncConfigPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(REQUEST_CONFIG_ID, (payload, context) ->
                context.server().execute(() -> sendSnapshot(context.player())));

        ServerPlayNetworking.registerGlobalReceiver(UPDATE_CONFIG_ID, (payload, context) ->
                context.server().execute(() -> applyUpdate(context.server(), context.player(), payload.values())));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendSnapshot(handler.player));
    }

    public static void sendSnapshot(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, SYNC_CONFIG)) {
            return;
        }

        ServerPlayNetworking.send(player, new SyncConfigPayload(player.hasPermissionLevel(2), FabricUtilityConfig.values()));
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

    public record RequestConfigPayload() implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, RequestConfigPayload> CODEC = PacketCodec.unit(new RequestConfigPayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return REQUEST_CONFIG_ID;
        }
    }

    public record UpdateConfigPayload(Map<String, String> values) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, UpdateConfigPayload> CODEC = PacketCodec.of(
                (payload, buf) -> writeValues(buf, payload.values()),
                buf -> new UpdateConfigPayload(readValues(buf))
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return UPDATE_CONFIG_ID;
        }
    }

    public record SyncConfigPayload(boolean canEdit, Map<String, String> values) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, SyncConfigPayload> CODEC = PacketCodec.of(
                (payload, buf) -> {
                    buf.writeBoolean(payload.canEdit());
                    writeValues(buf, payload.values());
                },
                buf -> new SyncConfigPayload(buf.readBoolean(), readValues(buf))
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return SYNC_CONFIG_ID;
        }
    }
}
