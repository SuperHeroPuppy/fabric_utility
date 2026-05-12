package net.supersnetwork.fabric_utility;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class NicknameSavedData extends PersistentState {
    private static final String STORAGE_KEY = "fabric_utility_nicknames";

    private final Map<UUID, PlayerNicknames> nicknames = new HashMap<>();

    public static NicknameSavedData get(MinecraftServer server) {
        return server.getWorld(World.OVERWORLD).getPersistentStateManager().getOrCreate(
                NicknameSavedData::fromNbt,
                NicknameSavedData::new,
                STORAGE_KEY
        );
    }

    public void addNickname(ServerPlayerEntity player, String nickname) {
        PlayerNicknames data = nicknames.computeIfAbsent(player.getUuid(), ignored -> new PlayerNicknames());

        if (!data.history.contains(nickname)) {
            data.history.add(nickname);
        }

        data.current = nickname;
        markDirty();
    }

    public void setNickname(ServerPlayerEntity player, String nickname) {
        PlayerNicknames data = nicknames.computeIfAbsent(player.getUuid(), ignored -> new PlayerNicknames());
        data.current = nickname;
        markDirty();
    }

    public Optional<String> getNickname(ServerPlayerEntity player) {
        PlayerNicknames data = nicknames.get(player.getUuid());
        return data == null || data.current.isBlank() ? Optional.empty() : Optional.of(data.current);
    }

    public List<String> getHistory(ServerPlayerEntity player) {
        PlayerNicknames data = nicknames.get(player.getUuid());
        return data == null ? List.of() : List.copyOf(data.history);
    }

    public void clearNickname(ServerPlayerEntity player) {
        PlayerNicknames data = nicknames.computeIfAbsent(player.getUuid(), ignored -> new PlayerNicknames());
        data.current = "";
        markDirty();
    }

    public void removeFromHistory(ServerPlayerEntity player, String nickname) {
        PlayerNicknames data = nicknames.computeIfAbsent(player.getUuid(), ignored -> new PlayerNicknames());
        data.history.remove(nickname);

        if (nickname.equals(data.current)) {
            data.current = "";
        }

        markDirty();
    }

    public static NicknameSavedData fromNbt(NbtCompound nbt) {
        NicknameSavedData savedData = new NicknameSavedData();
        NbtList players = nbt.getList("players", NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < players.size(); i++) {
            NbtCompound playerNbt = players.getCompound(i);
            PlayerNicknames playerNicknames = new PlayerNicknames();
            playerNicknames.current = playerNbt.getString("current");

            NbtList history = playerNbt.getList("history", NbtElement.STRING_TYPE);
            for (int j = 0; j < history.size(); j++) {
                playerNicknames.history.add(history.getString(j));
            }

            savedData.nicknames.put(playerNbt.getUuid("uuid"), playerNicknames);
        }

        return savedData;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList players = new NbtList();

        for (Map.Entry<UUID, PlayerNicknames> entry : nicknames.entrySet()) {
            NbtCompound playerNbt = new NbtCompound();
            playerNbt.putUuid("uuid", entry.getKey());
            playerNbt.putString("current", entry.getValue().current);

            NbtList history = new NbtList();
            for (String nickname : entry.getValue().history) {
                history.add(NbtString.of(nickname));
            }

            playerNbt.put("history", history);
            players.add(playerNbt);
        }

        nbt.put("players", players);
        return nbt;
    }

    private static final class PlayerNicknames {
        private String current = "";
        private final List<String> history = new ArrayList<>();
    }
}
