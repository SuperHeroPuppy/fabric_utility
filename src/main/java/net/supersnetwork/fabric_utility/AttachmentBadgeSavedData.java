package net.supersnetwork.fabric_utility;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class AttachmentBadgeSavedData extends PersistentState {
    private static final String STORAGE_KEY = "fabric_utility_cosmetics";
    private final Map<UUID, String> selections = new HashMap<>();

    public static AttachmentBadgeSavedData get(MinecraftServer server) {
        return server.getWorld(World.OVERWORLD).getPersistentStateManager().getOrCreate(
                AttachmentBadgeSavedData::fromNbt,
                AttachmentBadgeSavedData::new,
                STORAGE_KEY
        );
    }

    public boolean hasSelection(UUID uuid) {
        return selections.containsKey(uuid);
    }

    public Optional<String> getSelection(UUID uuid) {
        String badge = selections.get(uuid);
        return badge == null || badge.isBlank() ? Optional.empty() : Optional.of(badge);
    }

    public void setSelection(UUID uuid, String badge) {
        selections.put(uuid, badge == null ? "" : badge);
        markDirty();
    }

    private static AttachmentBadgeSavedData fromNbt(NbtCompound nbt) {
        AttachmentBadgeSavedData data = new AttachmentBadgeSavedData();
        NbtList entries = nbt.getList("selections", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < entries.size(); i++) {
            NbtCompound entry = entries.getCompound(i);
            data.selections.put(entry.getUuid("uuid"), entry.getString("badge"));
        }
        return data;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList entries = new NbtList();
        selections.forEach((uuid, badge) -> {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("uuid", uuid);
            entry.putString("badge", badge);
            entries.add(entry);
        });
        nbt.put("selections", entries);
        return nbt;
    }
}
