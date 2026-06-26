package net.supersnetwork.fabric_utility;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ChatStateSavedData extends PersistentState {
    private static final String STORAGE_KEY = "fabric_utility_chat_state";
    private static final Type<ChatStateSavedData> TYPE = new Type<>(
            ChatStateSavedData::new,
            (nbt, registryLookup) -> fromNbt(nbt),
            null
    );

    private final Map<UUID, PlayerChatState> states = new HashMap<>();

    public static ChatStateSavedData get(MinecraftServer server) {
        return server.getWorld(World.OVERWORLD).getPersistentStateManager().getOrCreate(
                TYPE,
                STORAGE_KEY
        );
    }

    public int getRangeChunks(ServerPlayerEntity player) {
        return getState(player).rangeChunks;
    }

    public void setRangeChunks(ServerPlayerEntity player, int rangeChunks) {
        PlayerChatState state = getState(player);
        state.rangeChunks = Math.max(1, rangeChunks);
        markDirty();
    }

    public boolean isDoNotDisturb(ServerPlayerEntity player) {
        return getState(player).doNotDisturb;
    }

    public void setDoNotDisturb(ServerPlayerEntity player, boolean doNotDisturb) {
        PlayerChatState state = getState(player);
        state.doNotDisturb = doNotDisturb;
        markDirty();
    }

    public ChatPinMode getPinMode(ServerPlayerEntity player) {
        return getState(player).pinMode;
    }

    public String getPinnedArea(ServerPlayerEntity player) {
        return getState(player).pinnedArea;
    }

    public void setPinMode(ServerPlayerEntity player, ChatPinMode pinMode, String pinnedArea) {
        PlayerChatState state = getState(player);
        state.pinMode = pinMode;
        state.pinnedArea = pinnedArea == null ? "" : pinnedArea;
        markDirty();
    }

    private PlayerChatState getState(ServerPlayerEntity player) {
        return states.computeIfAbsent(player.getUuid(), ignored -> new PlayerChatState());
    }

    public static ChatStateSavedData fromNbt(NbtCompound nbt) {
        ChatStateSavedData savedData = new ChatStateSavedData();
        NbtList players = nbt.getList("players", NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < players.size(); i++) {
            NbtCompound playerNbt = players.getCompound(i);
            PlayerChatState state = new PlayerChatState();
            state.rangeChunks = playerNbt.getInt("rangeChunks");
            state.doNotDisturb = playerNbt.getBoolean("doNotDisturb");
            state.pinMode = parsePinMode(playerNbt.getString("pinMode"));
            state.pinnedArea = playerNbt.getString("pinnedArea");
            savedData.states.put(playerNbt.getUuid("uuid"), state);
        }

        return savedData;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList players = new NbtList();

        for (Map.Entry<UUID, PlayerChatState> entry : states.entrySet()) {
            NbtCompound playerNbt = new NbtCompound();
            playerNbt.putUuid("uuid", entry.getKey());
            playerNbt.putInt("rangeChunks", entry.getValue().rangeChunks);
            playerNbt.putBoolean("doNotDisturb", entry.getValue().doNotDisturb);
            playerNbt.putString("pinMode", entry.getValue().pinMode.name());
            playerNbt.putString("pinnedArea", entry.getValue().pinnedArea);
            players.add(playerNbt);
        }

        nbt.put("players", players);
        return nbt;
    }

    private static ChatPinMode parsePinMode(String value) {
        try {
            return ChatPinMode.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return ChatPinMode.NONE;
        }
    }

    private static final class PlayerChatState {
        private int rangeChunks = FabricUtilityConfig.proxyChatRangeChunks();
        private boolean doNotDisturb = false;
        private ChatPinMode pinMode = ChatPinMode.NONE;
        private String pinnedArea = "";
    }

    public enum ChatPinMode {
        NONE,
        WORLD,
        LOCAL,
        AREA
    }
}
