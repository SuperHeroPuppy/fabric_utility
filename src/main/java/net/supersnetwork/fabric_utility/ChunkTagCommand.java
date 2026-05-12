package net.supersnetwork.fabric_utility;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ChunkTagCommand {
    private ChunkTagCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("tagchunk")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("tag", StringArgumentType.string())
                                        .executes(context -> {
                                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                            ServerWorld world = player.getServerWorld();
                                            String tag = StringArgumentType.getString(context, "tag");
                                            int chunkX = player.getBlockPos().getX() >> 4;
                                            int chunkZ = player.getBlockPos().getZ() >> 4;
                                            Identifier dimension = world.getRegistryKey().getValue();

                                            TaggedChunksSavedData.get(world).addTag(dimension, chunkX, chunkZ, tag);
                                            context.getSource().sendFeedback(() -> Text.literal("Chunk tagged with: " + tag), true);
                                            return 1;
                                        })))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("tag", StringArgumentType.string())
                                        .executes(context -> {
                                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                            ServerWorld world = player.getServerWorld();
                                            String tag = StringArgumentType.getString(context, "tag");
                                            int chunkX = player.getBlockPos().getX() >> 4;
                                            int chunkZ = player.getBlockPos().getZ() >> 4;
                                            Identifier dimension = world.getRegistryKey().getValue();
                                            TaggedChunksSavedData data = TaggedChunksSavedData.get(world);

                                            if (!data.hasTag(dimension, chunkX, chunkZ, tag)) {
                                                context.getSource().sendError(Text.literal("This chunk was never tagged with: " + tag));
                                                return 0;
                                            }

                                            data.removeTag(dimension, chunkX, chunkZ, tag);
                                            context.getSource().sendFeedback(() -> Text.literal("Removed tag: " + tag), true);
                                            return 1;
                                        })))
                        .then(CommandManager.literal("get")
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                    ServerWorld world = player.getServerWorld();
                                    int chunkX = player.getBlockPos().getX() >> 4;
                                    int chunkZ = player.getBlockPos().getZ() >> 4;
                                    Identifier dimension = world.getRegistryKey().getValue();

                                    context.getSource().sendFeedback(
                                            () -> Text.literal("Tags: " + TaggedChunksSavedData.get(world).getTags(dimension, chunkX, chunkZ)),
                                            false
                                    );
                                    return 1;
                                }))
                        .then(CommandManager.literal("check")
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                    ServerWorld world = player.getServerWorld();
                                    int chunkX = player.getBlockPos().getX() >> 4;
                                    int chunkZ = player.getBlockPos().getZ() >> 4;
                                    Identifier dimension = world.getRegistryKey().getValue();
                                    boolean tagged = TaggedChunksSavedData.get(world).isTagged(dimension, chunkX, chunkZ);

                                    context.getSource().sendFeedback(() -> Text.literal("Chunk tagged: " + tagged), false);
                                    return 1;
                                }))));
    }
}
