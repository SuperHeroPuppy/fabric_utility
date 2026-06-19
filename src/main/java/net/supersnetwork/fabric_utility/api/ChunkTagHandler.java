package net.supersnetwork.fabric_utility.api;

import java.util.List;

public interface ChunkTagHandler {
    default void onAdded(ChunkTagContext context, List<String> values) {
    }

    default void onUpdated(ChunkTagContext context, List<String> previousValues, List<String> newValues) {
    }

    default void onRemoved(ChunkTagContext context, List<String> previousValues) {
    }
}
