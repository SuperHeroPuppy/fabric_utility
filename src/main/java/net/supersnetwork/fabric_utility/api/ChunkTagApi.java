package net.supersnetwork.fabric_utility.api;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.supersnetwork.fabric_utility.TaggedChunksSavedData;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Public API for registering behavior for named chunk tags and querying or
 * mutating Fabric Utility's persistent chunk/subchunk tag storage.
 */
public final class ChunkTagApi {
    private static final Map<String, ChunkTagHandler> HANDLERS = new ConcurrentHashMap<>();

    private ChunkTagApi() {
    }

    public static void register(String tag, ChunkTagHandler handler) {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("Tag name cannot be blank");
        }
        ChunkTagHandler previous = HANDLERS.putIfAbsent(tag, handler);
        if (previous != null) {
            throw new IllegalStateException("A handler is already registered for chunk tag '" + tag + "'");
        }
    }

    public static void unregister(String tag, ChunkTagHandler handler) {
        HANDLERS.remove(tag, handler);
    }

    public static boolean hasTag(ServerWorld world, BlockPos pos, String tag) {
        return values(world, pos, tag).isPresent();
    }

    public static Optional<List<String>> values(ServerWorld world, BlockPos pos, String tag) {
        return TaggedChunksSavedData.get(world).getValuesAt(world.getRegistryKey().getValue(), pos, tag);
    }

    public static boolean add(ServerWorld world, int chunkX, int chunkZ, Optional<Integer> subChunkY, String tag, List<String> values) {
        return TaggedChunksSavedData.get(world).addTag(world.getRegistryKey().getValue(), chunkX, chunkZ, subChunkY, tag, values);
    }

    public static boolean set(ServerWorld world, int chunkX, int chunkZ, Optional<Integer> subChunkY, String tag, List<String> values) {
        return TaggedChunksSavedData.get(world).setTag(world.getRegistryKey().getValue(), chunkX, chunkZ, subChunkY, tag, values);
    }

    public static boolean remove(ServerWorld world, int chunkX, int chunkZ, Optional<Integer> subChunkY, String tag) {
        return TaggedChunksSavedData.get(world).removeTag(world.getRegistryKey().getValue(), chunkX, chunkZ, subChunkY, tag);
    }

    public static Map<String, List<String>> tags(ServerWorld world, int chunkX, int chunkZ, Optional<Integer> subChunkY) {
        return TaggedChunksSavedData.get(world).getTags(world.getRegistryKey().getValue(), chunkX, chunkZ, subChunkY);
    }

    public static void fireAdded(ServerWorld world, String tag, Identifier dimension, int chunkX, int chunkZ, Optional<Integer> subChunkY, List<String> values) {
        ChunkTagHandler handler = HANDLERS.get(tag);
        if (handler != null) {
            handler.onAdded(new ChunkTagContext(world, dimension, chunkX, chunkZ, subChunkY), List.copyOf(values));
        }
    }

    public static void fireUpdated(ServerWorld world, String tag, Identifier dimension, int chunkX, int chunkZ, Optional<Integer> subChunkY, List<String> oldValues, List<String> newValues) {
        ChunkTagHandler handler = HANDLERS.get(tag);
        if (handler != null) {
            handler.onUpdated(new ChunkTagContext(world, dimension, chunkX, chunkZ, subChunkY), List.copyOf(oldValues), List.copyOf(newValues));
        }
    }

    public static void fireRemoved(ServerWorld world, String tag, Identifier dimension, int chunkX, int chunkZ, Optional<Integer> subChunkY, List<String> oldValues) {
        ChunkTagHandler handler = HANDLERS.get(tag);
        if (handler != null) {
            handler.onRemoved(new ChunkTagContext(world, dimension, chunkX, chunkZ, subChunkY), List.copyOf(oldValues));
        }
    }
}
