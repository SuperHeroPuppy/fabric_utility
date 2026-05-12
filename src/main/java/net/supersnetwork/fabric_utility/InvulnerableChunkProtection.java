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

public final class InvulnerableChunkProtection {
    private static final String TAG = "invulnerability";

    private InvulnerableChunkProtection() {
    }

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (bypass(player) || !isProtected(world, pos)) {
                return true;
            }

            warn(player);
            return false;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (bypass(player) || !isProtected(world, pos)) {
                return ActionResult.PASS;
            }

            warn(player);
            return ActionResult.FAIL;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            BlockPos hitPos = hitResult.getBlockPos();
            BlockPos placementPos = hitPos.offset(hitResult.getSide());

            if (bypass(player) || (!isProtected(world, hitPos) && !isProtected(world, placementPos))) {
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
        return TaggedChunksSavedData.get(serverWorld).hasTag(dimension, chunkX, chunkZ, TAG);
    }

    public static boolean isProtected(World world, BlockPos pos) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return false;
        }

        return isProtected((WorldAccess) serverWorld, pos);
    }

    private static boolean bypass(PlayerEntity player) {
        return player != null && player.isCreative();
    }

    private static void warn(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(Text.literal("This chunk is protected").formatted(Formatting.RED), true);
        }
    }
}
