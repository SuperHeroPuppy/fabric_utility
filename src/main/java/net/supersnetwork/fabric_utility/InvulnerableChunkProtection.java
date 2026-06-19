package net.supersnetwork.fabric_utility;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.List;
import java.util.Optional;

public final class InvulnerableChunkProtection {
    private static final String TAG = "invulnerability";

    private InvulnerableChunkProtection() {
    }

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (isAboveConfiguredHeight(world, pos)) {
                warnHeight(player);
                return false;
            }

            if (bypass(player, world, pos) || !isProtected(world, pos)) {
                return true;
            }

            warn(player);
            return false;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (isAboveConfiguredHeight(world, pos)) {
                warnHeight(player);
                return ActionResult.FAIL;
            }

            if (bypass(player, world, pos) || !isProtected(world, pos)) {
                return ActionResult.PASS;
            }

            warn(player);
            return ActionResult.FAIL;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            BlockPos hitPos = hitResult.getBlockPos();
            BlockPos placementPos = hitPos.offset(hitResult.getSide());

            if (isAboveConfiguredHeight(world, placementPos)) {
                warnHeight(player);
                return ActionResult.FAIL;
            }

            if (bypass(player, world, hitPos) || (!isProtected(world, hitPos) && !isProtected(world, placementPos))) {
                return ActionResult.PASS;
            }

            warn(player);
            return ActionResult.FAIL;
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if ((entity instanceof FallingBlockEntity || entity instanceof TntEntity)
                    && isProtected(world, entity.getBlockPos())) {
                entity.discard();
            }
        });
    }

    public static boolean isProtected(WorldAccess world, BlockPos pos) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return false;
        }

        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        Identifier dimension = serverWorld.getRegistryKey().getValue();
        return TaggedChunksSavedData.get(serverWorld).hasTagAt(dimension, pos, TAG);
    }

    public static boolean isProtected(World world, BlockPos pos) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return false;
        }

        return isProtected((WorldAccess) serverWorld, pos);
    }

    private static boolean bypass(PlayerEntity player, WorldAccess world, BlockPos pos) {
        if (player == null) {
            return false;
        }

        if (player.isCreative()) {
            return true;
        }

        if (!(world instanceof ServerWorld serverWorld)) {
            return false;
        }

        Optional<List<String>> bypassValues = TaggedChunksSavedData.get(serverWorld).getValuesAt(serverWorld.getRegistryKey().getValue(), pos, TAG);
        String playerName = player.getName().getString();

        return bypassValues
                .map(values -> values.stream().anyMatch(value -> value.equalsIgnoreCase(playerName)))
                .orElse(false);
    }

    private static void warn(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(Text.literal("This chunk is protected").formatted(Formatting.RED), true);
        }
    }

    private static boolean isAboveConfiguredHeight(WorldAccess world, BlockPos pos) {
        if (!(world instanceof World realWorld)) {
            return false;
        }

        return pos.getY() >= realWorld.getTopY();
    }

    private static void warnHeight(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(Text.literal("This world is height-limited here").formatted(Formatting.RED), true);
        }
    }
}
