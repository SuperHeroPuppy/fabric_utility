package net.supersnetwork.fabric_utility;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class AttachmentBadgeManager {
    private static final String API_BASE = "https://www.supersnetwork.com";
    private static final String SUPPORTERS_API = API_BASE + "/api/supporter";
    private static final String BADGES_API = API_BASE + "/api/supporter/badges";
    private static final Duration CACHE_DURATION = Duration.ofMinutes(10);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static volatile Map<String, Supporter> supportersByUid = Map.of();
    private static volatile Map<String, Badge> badges = Map.of();
    private static volatile Instant lastRefresh = Instant.EPOCH;
    private static volatile boolean refreshing;

    private AttachmentBadgeManager() {
    }

    public static void register() {
        registerCommand();
        ServerLifecycleEvents.SERVER_STARTED.register(AttachmentBadgeManager::refresh);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ensureFresh(server);
            applyDefaultSelection(handler.player);
            AttachmentBadgeApi.refreshPlayerList(handler.player);
        });
    }

    static Optional<String> selectedBadge(ServerPlayerEntity player) {
        return AttachmentBadgeSavedData.get(player.getServer()).getSelection(player.getUuid())
                .filter(badge -> isAllowed(player, badge));
    }

    static String badgeDescription(String badgeId) {
        Badge badge = badges.get(badgeId);
        return badge == null ? badgeId : badge.description;
    }

    private static void registerCommand() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("cosmetic")
                        .executes(context -> showStatus(context.getSource().getPlayerOrThrow()))
                        .then(CommandManager.literal("list")
                                .executes(context -> list(context.getSource().getPlayerOrThrow())))
                        .then(CommandManager.literal("select")
                                .then(CommandManager.argument("badge", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            try {
                                                allowedBadges(context.getSource().getPlayerOrThrow()).forEach(builder::suggest);
                                            } catch (Exception ignored) {
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> select(
                                                context.getSource().getPlayerOrThrow(),
                                                StringArgumentType.getString(context, "badge")
                                        ))))
                        .then(CommandManager.literal("clear")
                                .executes(context -> clear(context.getSource().getPlayerOrThrow())))
                        .then(CommandManager.literal("refresh")
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                    refresh(context.getSource().getServer());
                                    player.sendMessage(Text.literal("Refreshing supporter cosmetics…").formatted(Formatting.GRAY), false);
                                    return 1;
                                }))));
    }

    private static int showStatus(ServerPlayerEntity player) {
        Optional<String> selection = AttachmentBadgeSavedData.get(player.getServer()).getSelection(player.getUuid());
        if (selection.isPresent() && isAllowed(player, selection.get())) {
            player.sendMessage(Text.literal("Selected cosmetic: " + selection.get()), false);
        } else {
            player.sendMessage(Text.literal("No cosmetic selected. Use /cosmetic list and /cosmetic select <badge>."), false);
        }
        return 1;
    }

    private static int list(ServerPlayerEntity player) {
        List<String> allowed = allowedBadges(player);
        if (allowed.isEmpty()) {
            player.sendMessage(Text.literal(
                    supportersByUid.isEmpty()
                            ? "Supporter cosmetics are still loading or unavailable. Try /cosmetic refresh."
                            : "No supporter cosmetics are available for your account."
            ).formatted(Formatting.YELLOW), false);
            return 0;
        }

        MutableText message = Text.literal("Available cosmetics: ");
        for (int i = 0; i < allowed.size(); i++) {
            String id = allowed.get(i);
            Badge badge = badges.get(id);
            if (i > 0) {
                message.append(Text.literal(", "));
            }
            message.append(Text.literal(id).styled(style -> style
                    .withColor(Formatting.AQUA)
                    .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Text.literal(badge == null ? id : badge.description)
                    ))));
        }
        player.sendMessage(message, false);
        return allowed.size();
    }

    private static int select(ServerPlayerEntity player, String badgeId) {
        String normalized = badgeId.toLowerCase(Locale.ROOT);
        if (!badges.containsKey(normalized)) {
            player.sendMessage(Text.literal("Unknown cosmetic: " + badgeId).formatted(Formatting.RED), false);
            return 0;
        }
        if (!isAllowed(player, normalized)) {
            player.sendMessage(Text.literal("That cosmetic is not available for your supporter account.")
                    .formatted(Formatting.RED), false);
            return 0;
        }

        AttachmentBadgeSavedData.get(player.getServer()).setSelection(player.getUuid(), normalized);
        AttachmentBadgeApi.refreshPlayerList(player);
        player.sendMessage(Text.literal("Selected cosmetic: " + normalized).formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int clear(ServerPlayerEntity player) {
        AttachmentBadgeSavedData.get(player.getServer()).setSelection(player.getUuid(), "");
        AttachmentBadgeApi.refreshPlayerList(player);
        player.sendMessage(Text.literal("Cosmetic cleared.").formatted(Formatting.GREEN), false);
        return 1;
    }

    private static List<String> allowedBadges(ServerPlayerEntity player) {
        Supporter supporter = supporter(player);
        if (supporter == null) {
            return List.of();
        }
        Collection<String> result = supporter.allBadges ? badges.keySet() : supporter.allowedBadges;
        return result.stream()
                .filter(badges::containsKey)
                .sorted()
                .toList();
    }

    private static boolean isAllowed(ServerPlayerEntity player, String badge) {
        Supporter supporter = supporter(player);
        return supporter != null
                && badges.containsKey(badge)
                && (supporter.allBadges || supporter.allowedBadges.contains(badge));
    }

    private static Supporter supporter(ServerPlayerEntity player) {
        return supportersByUid.get(normalizeUuid(player.getUuid()));
    }

    private static void applyDefaultSelection(ServerPlayerEntity player) {
        AttachmentBadgeSavedData data = AttachmentBadgeSavedData.get(player.getServer());
        if (!data.hasSelection(player.getUuid()) && isAllowed(player, "default")) {
            data.setSelection(player.getUuid(), "default");
        }
    }

    static boolean isBundledBadge(String badge) {
        return badge.equals("action_deck")
                || badge.equals("colon3")
                || badge.equals("default")
                || badge.equals("super");
    }

    private static void ensureFresh(MinecraftServer server) {
        if (lastRefresh.plus(CACHE_DURATION).isBefore(Instant.now())) {
            refresh(server);
        }
    }

    public static synchronized void refresh(MinecraftServer server) {
        if (refreshing) {
            return;
        }
        refreshing = true;

        CompletableFuture.supplyAsync(AttachmentBadgeManager::fetch)
                .whenComplete((snapshot, error) -> server.execute(() -> {
                    refreshing = false;
                    if (error != null || snapshot == null) {
                        FabricUtility.LOGGER.warn("Failed to refresh supporter cosmetics", error);
                        return;
                    }

                    supportersByUid = Map.copyOf(snapshot.supporters);
                    badges = Map.copyOf(snapshot.badges);
                    lastRefresh = Instant.now();
                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        applyDefaultSelection(player);
                        AttachmentBadgeApi.refreshPlayerList(player);
                    }
                    FabricUtility.LOGGER.info("Loaded {} supporters and {} cosmetics", supportersByUid.size(), badges.size());
                }));
    }

    private static Snapshot fetch() {
        JsonObject supporterRoot = getJson(SUPPORTERS_API);
        JsonObject badgeRoot = getJson(BADGES_API);
        Map<String, Supporter> fetchedSupporters = new LinkedHashMap<>();
        Map<String, Badge> fetchedBadges = new LinkedHashMap<>();

        JsonArray supporterArray = supporterRoot.getAsJsonObject("data").getAsJsonArray("supporters");
        for (JsonElement element : supporterArray) {
            JsonObject object = element.getAsJsonObject();
            String username = string(object, "minecraftUsername", "");
            String uid = string(object, "uid", "");
            String role = string(object, "role", "supporter");
            JsonElement allowedElement = object.get("allowedBadges");
            boolean all = allowedElement != null && allowedElement.isJsonPrimitive()
                    && "*".equals(allowedElement.getAsString());
            Set<String> allowed = new LinkedHashSet<>();
            if (allowedElement != null && allowedElement.isJsonArray()) {
                allowedElement.getAsJsonArray().forEach(value -> allowed.add(value.getAsString().toLowerCase(Locale.ROOT)));
            }
            fetchedSupporters.put(uid.replace("-", "").toLowerCase(Locale.ROOT),
                    new Supporter(username, role, all, Set.copyOf(allowed)));
        }

        for (JsonElement element : badgeRoot.getAsJsonArray("badges")) {
            JsonObject object = element.getAsJsonObject();
            String id = string(object, "id", "").toLowerCase(Locale.ROOT);
            if (id.isBlank()) {
                continue;
            }

            String infoPath = string(object, "info", "/api/supporter/badges/" + id);
            JsonObject info = getJson(URI.create(API_BASE).resolve(infoPath).toString());
            fetchedBadges.put(id, new Badge(
                    id,
                    string(info, "description", id),
                    string(info, "creator", "Unknown")
            ));
        }

        return new Snapshot(fetchedSupporters, fetchedBadges);
    }

    private static JsonObject getJson(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .header("User-Agent", "FabricUtility/" + VersionCompatibility.VERSION)
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("HTTP " + response.statusCode() + " from " + url);
            }
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load " + url, exception);
        }
    }

    private static String string(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    private static String normalizeUuid(UUID uuid) {
        return uuid.toString().replace("-", "").toLowerCase(Locale.ROOT);
    }

    private record Supporter(String username, String role, boolean allBadges, Set<String> allowedBadges) {
    }

    private record Badge(String id, String description, String creator) {
    }

    private record Snapshot(Map<String, Supporter> supporters, Map<String, Badge> badges) {
    }
}
