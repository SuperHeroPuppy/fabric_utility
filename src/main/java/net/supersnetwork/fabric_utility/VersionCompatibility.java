package net.supersnetwork.fabric_utility;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VersionCompatibility {
    public static final Identifier VERSION_INFO = new Identifier(FabricUtility.MOD_ID, "version_info");
    public static final String VERSION = FabricLoader.getInstance()
            .getModContainer(FabricUtility.MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");

    private static final Map<UUID, Integer> UNVERIFIED_CLIENTS = new HashMap<>();

    private VersionCompatibility() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(VERSION_INFO, (server, player, handler, buf, responseSender) -> {
            String clientVersion = buf.readString(128);
            server.execute(() -> receiveClientVersion(player, clientVersion));
        });

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

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(VERSION, 128);
        ServerPlayNetworking.send(player, VERSION_INFO, buf);
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
}
