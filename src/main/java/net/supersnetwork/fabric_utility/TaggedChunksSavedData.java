package net.supersnetwork.fabric_utility;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.supersnetwork.fabric_utility.api.ChunkTagApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class TaggedChunksSavedData extends PersistentState {
    private static final String STORAGE_KEY = "tagged_chunks";
    private static final int WHOLE_CHUNK_SECTION = Integer.MIN_VALUE;
    private static final String AREA_TAG_PREFIX = "proxy_area:";

    private final Map<String, Map<String, List<String>>> taggedAreas = new HashMap<>();
    private transient ServerWorld world;

    public static TaggedChunksSavedData get(ServerWorld world) {
        TaggedChunksSavedData data = world.getPersistentStateManager().getOrCreate(
                TaggedChunksSavedData::fromNbt,
                TaggedChunksSavedData::new,
                STORAGE_KEY
        );
        data.world = world;
        return data;
    }

    public static int subChunkY(BlockPos pos) {
        return Math.floorDiv(pos.getY(), 16);
    }

    public boolean addTag(Identifier dimension, int chunkX, int chunkZ, Optional<Integer> subChunkY, String tag, List<String> values) {
        Map<String, List<String>> tags = taggedAreas.computeIfAbsent(makeKey(dimension, chunkX, chunkZ, subChunkY), ignored -> new HashMap<>());

        if (tags.containsKey(tag)) {
            return false;
        }

        tags.put(tag, List.copyOf(values));
        markDirty();
        ChunkTagApi.fireAdded(world, tag, dimension, chunkX, chunkZ, subChunkY, values);
        return true;
    }

    public boolean setTag(Identifier dimension, int chunkX, int chunkZ, Optional<Integer> subChunkY, String tag, List<String> values) {
        Map<String, List<String>> tags = taggedAreas.get(makeKey(dimension, chunkX, chunkZ, subChunkY));

        if (tags == null || !tags.containsKey(tag)) {
            return false;
        }

        List<String> previous = tags.put(tag, List.copyOf(values));
        markDirty();
        ChunkTagApi.fireUpdated(world, tag, dimension, chunkX, chunkZ, subChunkY, previous, values);
        return true;
    }

    public boolean removeTag(Identifier dimension, int chunkX, int chunkZ, Optional<Integer> subChunkY, String tag) {
        String key = makeKey(dimension, chunkX, chunkZ, subChunkY);
        Map<String, List<String>> tags = taggedAreas.get(key);

        if (tags == null || !tags.containsKey(tag)) {
            return false;
        }

        List<String> previous = tags.remove(tag);

        if (tags.isEmpty()) {
            taggedAreas.remove(key);
        }

        markDirty();
        ChunkTagApi.fireRemoved(world, tag, dimension, chunkX, chunkZ, subChunkY, previous);
        return true;
    }

    public boolean isTagged(Identifier dimension, int chunkX, int chunkZ, Optional<Integer> subChunkY) {
        return !getTags(dimension, chunkX, chunkZ, subChunkY).isEmpty();
    }

    public boolean hasTagAt(Identifier dimension, BlockPos pos, String tag) {
        return getValuesAt(dimension, pos, tag).isPresent();
    }

    public Optional<List<String>> getValuesAt(Identifier dimension, BlockPos pos, String tag) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        Optional<List<String>> subChunkValues = getValues(dimension, chunkX, chunkZ, Optional.of(subChunkY(pos)), tag);

        if (subChunkValues.isPresent()) {
            return subChunkValues;
        }

        return getValues(dimension, chunkX, chunkZ, Optional.empty(), tag);
    }

    public Optional<List<String>> getValues(Identifier dimension, int chunkX, int chunkZ, Optional<Integer> subChunkY, String tag) {
        Map<String, List<String>> tags = taggedAreas.get(makeKey(dimension, chunkX, chunkZ, subChunkY));

        if (tags == null || !tags.containsKey(tag)) {
            return Optional.empty();
        }

        return Optional.of(List.copyOf(tags.get(tag)));
    }

    public Map<String, List<String>> getTags(Identifier dimension, int chunkX, int chunkZ, Optional<Integer> subChunkY) {
        Map<String, List<String>> tags = taggedAreas.get(makeKey(dimension, chunkX, chunkZ, subChunkY));
        return tags == null ? Map.of() : Map.copyOf(tags);
    }

    public Set<String> getProxyAreaNames() {
        Set<String> result = new HashSet<>();
        for (Map<String, List<String>> tags : taggedAreas.values()) {
            for (String tag : tags.keySet()) {
                if (tag.startsWith(AREA_TAG_PREFIX)) {
                    result.add(tag.substring(AREA_TAG_PREFIX.length()));
                }
            }
        }
        return result;
    }

    public boolean hasProxyAreaAt(Identifier dimension, int chunkX, int chunkZ, String areaName) {
        String tag = AREA_TAG_PREFIX + areaName;
        return getTags(dimension, chunkX, chunkZ, Optional.empty()).containsKey(tag);
    }

    public boolean createProxyArea(Identifier dimension, int centerChunkX, int centerChunkZ, int radius, String areaName) {
        if (areaName == null || areaName.isBlank()) {
            return false;
        }

        String tag = AREA_TAG_PREFIX + areaName;
        boolean modified = false;

        for (int chunkX = centerChunkX - radius; chunkX <= centerChunkX + radius; chunkX++) {
            for (int chunkZ = centerChunkZ - radius; chunkZ <= centerChunkZ + radius; chunkZ++) {
                if (!hasProxyAreaAt(dimension, chunkX, chunkZ, areaName)) {
                    addTag(dimension, chunkX, chunkZ, Optional.empty(), tag, List.of());
                    modified = true;
                }
            }
        }

        return modified;
    }

    public boolean deleteProxyArea(String areaName) {
        if (areaName == null || areaName.isBlank()) {
            return false;
        }

        String tag = AREA_TAG_PREFIX + areaName;
        boolean modified = false;

        for (String key : new HashSet<>(taggedAreas.keySet())) {
            Map<String, List<String>> tags = taggedAreas.get(key);
            if (tags != null && tags.containsKey(tag)) {
                tags.remove(tag);
                modified = true;

                if (tags.isEmpty()) {
                    taggedAreas.remove(key);
                }
            }
        }

        if (modified) {
            markDirty();
        }

        return modified;
    }

    private static String makeKey(Identifier dimension, int chunkX, int chunkZ, Optional<Integer> subChunkY) {
        return dimension + ":" + chunkX + ":" + chunkZ + ":" + subChunkY.orElse(WHOLE_CHUNK_SECTION);
    }

    public static TaggedChunksSavedData fromNbt(NbtCompound nbt) {
        TaggedChunksSavedData data = new TaggedChunksSavedData();
        NbtList chunks = nbt.getList("chunks", NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < chunks.size(); i++) {
            NbtCompound entry = chunks.getCompound(i);
            String key = migrateKey(entry.getString("key"));
            Map<String, List<String>> tags = new HashMap<>();

            NbtList tagEntries = entry.getList("tag_entries", NbtElement.COMPOUND_TYPE);
            if (!tagEntries.isEmpty()) {
                for (int j = 0; j < tagEntries.size(); j++) {
                    NbtCompound tagEntry = tagEntries.getCompound(j);
                    tags.put(tagEntry.getString("name"), readValues(tagEntry));
                }
            } else {
                NbtList legacyTags = entry.getList("tags", NbtElement.STRING_TYPE);
                for (int j = 0; j < legacyTags.size(); j++) {
                    tags.put(legacyTags.getString(j), List.of());
                }
            }

            data.taggedAreas.put(key, tags);
        }

        return data;
    }

    private static String migrateKey(String key) {
        long colonCount = key.chars().filter(character -> character == ':').count();

        if (colonCount == 3) {
            return key + ":" + WHOLE_CHUNK_SECTION;
        }

        return key;
    }

    private static List<String> readValues(NbtCompound tagEntry) {
        NbtList valueList = tagEntry.getList("values", NbtElement.STRING_TYPE);
        List<String> values = new ArrayList<>();

        for (int i = 0; i < valueList.size(); i++) {
            values.add(valueList.getString(i));
        }

        return values;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList chunks = new NbtList();

        for (Map.Entry<String, Map<String, List<String>>> area : taggedAreas.entrySet()) {
            NbtCompound entryNbt = new NbtCompound();
            entryNbt.putString("key", area.getKey());

            NbtList tagEntries = new NbtList();
            for (Map.Entry<String, List<String>> tag : area.getValue().entrySet()) {
                NbtCompound tagNbt = new NbtCompound();
                tagNbt.putString("name", tag.getKey());

                NbtList values = new NbtList();
                for (String value : tag.getValue()) {
                    values.add(NbtString.of(value));
                }

                tagNbt.put("values", values);
                tagEntries.add(tagNbt);
            }

            entryNbt.put("tag_entries", tagEntries);
            chunks.add(entryNbt);
        }

        nbt.put("chunks", chunks);
        return nbt;
    }
}
