package net.supersnetwork.fabric_utility;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ChunkTagCommand {
    private ChunkTagCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("tagchunk")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("tag", StringArgumentType.string())
                                        .executes(context -> add(context.getSource(), StringArgumentType.getString(context, "tag"), "", Optional.empty()))
                                        .then(CommandManager.argument("value", StringArgumentType.greedyString())
                                                .executes(context -> add(context.getSource(), StringArgumentType.getString(context, "tag"), StringArgumentType.getString(context, "value"), Optional.empty())))))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("tag", StringArgumentType.string())
                                        .executes(context -> remove(context.getSource(), StringArgumentType.getString(context, "tag"), Optional.empty()))))
                        .then(CommandManager.literal("get")
                                .executes(context -> get(context.getSource(), Optional.empty())))
                        .then(CommandManager.literal("check")
                                .executes(context -> check(context.getSource(), Optional.empty())))
                        .then(CommandManager.literal("subchunk")
                                .then(CommandManager.literal("add")
                                        .then(CommandManager.argument("tag", StringArgumentType.string())
                                                .executes(context -> add(context.getSource(), StringArgumentType.getString(context, "tag"), "", currentSubChunk(context.getSource())))
                                                .then(CommandManager.argument("value", StringArgumentType.greedyString())
                                                        .executes(context -> add(context.getSource(), StringArgumentType.getString(context, "tag"), StringArgumentType.getString(context, "value"), currentSubChunk(context.getSource()))))))
                                .then(CommandManager.literal("remove")
                                        .then(CommandManager.argument("tag", StringArgumentType.string())
                                                .executes(context -> remove(context.getSource(), StringArgumentType.getString(context, "tag"), currentSubChunk(context.getSource())))))
                                .then(CommandManager.literal("get")
                                        .executes(context -> get(context.getSource(), currentSubChunk(context.getSource()))))
                                .then(CommandManager.literal("check")
                                        .executes(context -> check(context.getSource(), currentSubChunk(context.getSource())))))));
    }

    private static int add(ServerCommandSource source, String tag, String rawValue, Optional<Integer> subChunkY) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld world = player.getServerWorld();
        ChunkPosition position = position(player);
        List<String> values = parseValues(rawValue);
        boolean added = TaggedChunksSavedData.get(world).addTag(position.dimension, position.chunkX, position.chunkZ, subChunkY, tag, values);

        if (!added) {
            source.sendError(Text.literal(areaName(subChunkY) + " already has tag: " + tag));
            return 0;
        }

        source.sendFeedback(() -> Text.literal(areaName(subChunkY) + " tagged with: " + describe(tag, values)), true);
        return 1;
    }

    private static int remove(ServerCommandSource source, String tag, Optional<Integer> subChunkY) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld world = player.getServerWorld();
        ChunkPosition position = position(player);
        boolean removed = TaggedChunksSavedData.get(world).removeTag(position.dimension, position.chunkX, position.chunkZ, subChunkY, tag);

        if (!removed) {
            source.sendError(Text.literal(areaName(subChunkY) + " was never tagged with: " + tag));
            return 0;
        }

        source.sendFeedback(() -> Text.literal("Removed tag from " + areaName(subChunkY).toLowerCase() + ": " + tag), true);
        return 1;
    }

    private static int get(ServerCommandSource source, Optional<Integer> subChunkY) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ChunkPosition position = position(player);
        Map<String, List<String>> tags = TaggedChunksSavedData.get(player.getServerWorld()).getTags(position.dimension, position.chunkX, position.chunkZ, subChunkY);

        source.sendFeedback(() -> Text.literal(areaName(subChunkY) + " tags: " + describe(tags)), false);
        return 1;
    }

    private static int check(ServerCommandSource source, Optional<Integer> subChunkY) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ChunkPosition position = position(player);
        boolean tagged = TaggedChunksSavedData.get(player.getServerWorld()).isTagged(position.dimension, position.chunkX, position.chunkZ, subChunkY);

        source.sendFeedback(() -> Text.literal(areaName(subChunkY) + " tagged: " + tagged), false);
        return 1;
    }

    private static Optional<Integer> currentSubChunk(ServerCommandSource source) throws CommandSyntaxException {
        return Optional.of(TaggedChunksSavedData.subChunkY(source.getPlayerOrThrow().getBlockPos()));
    }

    private static ChunkPosition position(ServerPlayerEntity player) {
        return new ChunkPosition(
                player.getServerWorld().getRegistryKey().getValue(),
                player.getBlockPos().getX() >> 4,
                player.getBlockPos().getZ() >> 4
        );
    }

    private static List<String> parseValues(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }

        return Arrays.stream(rawValue.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    private static String describe(Map<String, List<String>> tags) {
        if (tags.isEmpty()) {
            return "None";
        }

        return tags.entrySet().stream()
                .map(entry -> describe(entry.getKey(), entry.getValue()))
                .toList()
                .toString();
    }

    private static String describe(String tag, List<String> values) {
        return values.isEmpty() ? tag : tag + "=" + values;
    }

    private static String areaName(Optional<Integer> subChunkY) {
        return subChunkY.map(integer -> "Subchunk " + integer).orElse("Chunk");
    }

    private record ChunkPosition(Identifier dimension, int chunkX, int chunkZ) {
    }
}
