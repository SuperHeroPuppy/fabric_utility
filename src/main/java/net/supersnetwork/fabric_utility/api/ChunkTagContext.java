package net.supersnetwork.fabric_utility.api;

import net.minecraft.util.Identifier;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;

public record ChunkTagContext(ServerWorld world, Identifier dimension, int chunkX, int chunkZ, Optional<Integer> subChunkY) {
}
