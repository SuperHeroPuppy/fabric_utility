package net.supersnetwork.fabric_utility;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TaggedChunksSavedData extends PersistentState {
    private static final String STORAGE_KEY = "tagged_chunks";

    private final Map<String, Set<String>> taggedChunks = new HashMap<>();

    private static String makeKey(Identifier dimension, int x, int z) {
        return dimension + ":" + x + ":" + z;
    }

    public static TaggedChunksSavedData get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                TaggedChunksSavedData::fromNbt,
                TaggedChunksSavedData::new,
                STORAGE_KEY
        );
    }

    public void addTag(Identifier dimension, int x, int z, String tag) {
        String key = makeKey(dimension, x, z);
        taggedChunks.computeIfAbsent(key, ignored -> new HashSet<>()).add(tag);
        markDirty();
    }

    public void removeTag(Identifier dimension, int x, int z, String tag) {
        String key = makeKey(dimension, x, z);
        Set<String> tags = taggedChunks.get(key);

        if (tags == null) {
            return;
        }

        tags.remove(tag);

        if (tags.isEmpty()) {
            taggedChunks.remove(key);
        }

        markDirty();
    }

    public boolean isTagged(Identifier dimension, int x, int z) {
        return taggedChunks.containsKey(makeKey(dimension, x, z));
    }

    public boolean hasTag(Identifier dimension, int x, int z, String tag) {
        Set<String> tags = taggedChunks.get(makeKey(dimension, x, z));
        return tags != null && tags.contains(tag);
    }

    public Set<String> getTags(Identifier dimension, int x, int z) {
        Set<String> tags = taggedChunks.get(makeKey(dimension, x, z));
        return tags == null ? Set.of() : Set.copyOf(tags);
    }

    public static TaggedChunksSavedData fromNbt(NbtCompound nbt) {
        TaggedChunksSavedData data = new TaggedChunksSavedData();
        NbtList chunks = nbt.getList("chunks", NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < chunks.size(); i++) {
            NbtCompound entry = chunks.getCompound(i);
            String key = entry.getString("key");
            NbtList tags = entry.getList("tags", NbtElement.STRING_TYPE);

            Set<String> set = new HashSet<>();
            for (int j = 0; j < tags.size(); j++) {
                set.add(tags.getString(j));
            }

            data.taggedChunks.put(key, set);
        }

        return data;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList chunks = new NbtList();

        for (Map.Entry<String, Set<String>> entry : taggedChunks.entrySet()) {
            NbtCompound entryNbt = new NbtCompound();
            entryNbt.putString("key", entry.getKey());

            NbtList tags = new NbtList();
            for (String tag : entry.getValue()) {
                tags.add(NbtString.of(tag));
            }

            entryNbt.put("tags", tags);
            chunks.add(entryNbt);
        }

        nbt.put("chunks", chunks);
        return nbt;
    }
}
