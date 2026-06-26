package net.supersnetwork.fabric_utility;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VersionCompatibility {
    public static final Identifier VERSION_INFO = Identifier.of(FabricUtility.MOD_ID, "version_info");
    public static final CustomPayload.Id<VersionInfoPayload> VERSION_INFO_ID = new CustomPayload.Id<>(VERSION_INFO);
    public static final String VERSION = FabricLoader.getInstance()
            .getModContainer(FabricUtility.MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");

    private static final Map<UUID, Integer> UNVERIFIED_CLIENTS = new HashMap<>();

    private VersionCompatibility() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(VERSION_INFO_ID, VersionInfoPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(VERSION_INFO_ID, VersionInfoPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(VERSION_INFO_ID, (payload, context) ->
                context.server().execute(() -> receiveClientVersion(context.player(), payload.version())));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UNVERIFIED_CLIENTS.put(handler.player.getUuid(), 100);
            sendServerVersion(handler.player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                UNVERIFIED_CLIENTS.remove(handler.player.getUuid()));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            UNVERIFIED_CLIENTS.replaceAll((uuid, ticks) -> ticks - 1);
            UNVERIFIED_CLIENTS.entrySet().removeIf(entry -> {
                if (entry.getValue() > 0) {
                    return false;
                }

                ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
                if (player != null) {
                    player.sendMessage(Text.literal(
                            "Fabric Utility could not verify your client version. The server is running "
                                    + VERSION + "; update your client if features do not work correctly."
                    ).formatted(Formatting.YELLOW), false);
                }
                return true;
            });
        });
    }

    private static void receiveClientVersion(ServerPlayerEntity player, String clientVersion) {
        UNVERIFIED_CLIENTS.remove(player.getUuid());

        if (compareVersions(clientVersion, VERSION) < 0) {
            player.sendMessage(Text.literal(
                    "Your Fabric Utility client (" + clientVersion + ") is older than this server (" + VERSION
                            + "). Please update your mod."
            ).formatted(Formatting.RED), false);
        }
    }

    private static void sendServerVersion(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, VERSION_INFO)) {
            return;
        }

        ServerPlayNetworking.send(player, new VersionInfoPayload(VERSION));
    }

    public static int compareVersions(String left, String right) {
        String[] leftParts = normalize(left).split("\\.");
        String[] rightParts = normalize(right).split("\\.");
        int length = Math.max(leftParts.length, rightParts.length);

        for (int i = 0; i < length; i++) {
            int leftValue = i < leftParts.length ? parsePart(leftParts[i]) : 0;
            int rightValue = i < rightParts.length ? parsePart(rightParts[i]) : 0;
            int compared = Integer.compare(leftValue, rightValue);
            if (compared != 0) {
                return compared;
            }
        }
        return 0;
    }

    private static String normalize(String version) {
        return version == null ? "0" : version.split("[-+]", 2)[0];
    }

    private static int parsePart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9].*$", ""));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public record VersionInfoPayload(String version) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, VersionInfoPayload> CODEC = PacketCodec.of(
                (payload, buf) -> buf.writeString(payload.version(), 128),
                buf -> new VersionInfoPayload(buf.readString(128))
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return VERSION_INFO_ID;
        }
    }
}
