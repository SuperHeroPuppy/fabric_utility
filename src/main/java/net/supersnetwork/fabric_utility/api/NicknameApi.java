package net.supersnetwork.fabric_utility.api;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.supersnetwork.fabric_utility.FabricUtilityConfig;
import net.supersnetwork.fabric_utility.NickCommandManager;
import net.supersnetwork.fabric_utility.NicknameSavedData;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Public nickname API for Fabric Utility.
 *
 * <p>All nickname writes should go through this class so display surfaces and
 * registered listeners remain synchronized.</p>
 */
public final class NicknameApi {
    private static final List<NicknameChangeListener> LISTENERS = new CopyOnWriteArrayList<>();

    private NicknameApi() {
    }

    public static Optional<String> getNickname(ServerPlayerEntity player) {
        return NicknameSavedData.get(player.getServer()).getNickname(player);
    }

    public static Optional<String> getNickname(net.minecraft.server.MinecraftServer server, UUID uuid) {
        return NicknameSavedData.get(server).getNickname(uuid);
    }

    public static Optional<Integer> getColor(ServerPlayerEntity player) {
        return NicknameSavedData.get(player.getServer()).getColor(player);
    }

    public static String getEffectiveName(ServerPlayerEntity player) {
        if (!FabricUtilityConfig.nicknameSystemEnabled()) {
            return player.getGameProfile().getName();
        }
        return getNickname(player).orElse(player.getGameProfile().getName());
    }

    public static Text getDisplayName(ServerPlayerEntity player) {
        if (!FabricUtilityConfig.nicknameSystemEnabled()) {
            return Text.literal(player.getGameProfile().getName());
        }

        Optional<String> nickname = getNickname(player);
        if (nickname.isEmpty()) {
            return Text.literal(player.getGameProfile().getName());
        }

        MutableText result = Text.literal(nickname.get());
        getColor(player).ifPresent(color -> result.setStyle(Style.EMPTY.withColor(color)));
        return result;
    }

    public static List<String> getHistory(ServerPlayerEntity player) {
        return NicknameSavedData.get(player.getServer()).getHistory(player);
    }

    public static void setNickname(ServerPlayerEntity player, String nickname) {
        String normalized = nickname == null ? "" : nickname.trim();
        if (normalized.isEmpty()) {
            clearNickname(player);
            return;
        }

        Optional<String> previous = getNickname(player);
        NicknameSavedData.get(player.getServer()).addNickname(player, normalized);
        changed(player, previous.orElse(null), normalized);
    }

    public static void selectNickname(ServerPlayerEntity player, String nickname) {
        Optional<String> previous = getNickname(player);
        NicknameSavedData.get(player.getServer()).setNickname(player, nickname);
        changed(player, previous.orElse(null), nickname);
    }

    public static void clearNickname(ServerPlayerEntity player) {
        Optional<String> previous = getNickname(player);
        NicknameSavedData.get(player.getServer()).clearNickname(player);
        changed(player, previous.orElse(null), null);
    }

    public static void removeFromHistory(ServerPlayerEntity player, String nickname) {
        Optional<String> previous = getNickname(player);
        NicknameSavedData.get(player.getServer()).removeFromHistory(player, nickname);
        String current = getNickname(player).orElse(null);
        if (!java.util.Objects.equals(previous.orElse(null), current)) {
            changed(player, previous.orElse(null), current);
        }
    }

    public static void setColor(ServerPlayerEntity player, @Nullable Integer rgb) {
        NicknameSavedData.get(player.getServer()).setColor(player, rgb);
        NickCommandManager.refreshPlayer(player);
    }

    public static void registerChangeListener(NicknameChangeListener listener) {
        LISTENERS.add(listener);
    }

    public static void unregisterChangeListener(NicknameChangeListener listener) {
        LISTENERS.remove(listener);
    }

    private static void changed(ServerPlayerEntity player, @Nullable String previous, @Nullable String current) {
        NickCommandManager.refreshPlayer(player);
        for (NicknameChangeListener listener : LISTENERS) {
            listener.onNicknameChanged(player, previous, current);
        }
    }
}
