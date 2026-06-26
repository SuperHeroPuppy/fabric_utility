package net.supersnetwork.fabric_utility;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StasisHandler {
    public static final String STASIS_TAG = "stasis";
    public static final String STASIS_PROVIDER_TAG = "stasis_provider";
    public static final Identifier SYNC_STASIS = new Identifier(FabricUtility.MOD_ID, "sync_stasis");

    private static final Map<UUID, Anchor> ANCHORS = new HashMap<>();
    private static final Set<UUID> LAST_STASIS_STATE = new HashSet<>();

    private StasisHandler() {
    }

    public static void register() {
        UseEntityCallback.EVENT.register(StasisHandler::useProvider);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clear(handler.player));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                tick(player);
            }
        });
    }

    public static boolean isInStasis(ServerPlayerEntity player) {
        return player.getCommandTags().contains(STASIS_TAG);
    }

    private static ActionResult useProvider(net.minecraft.entity.player.PlayerEntity player, net.minecraft.world.World world,
                                            Hand hand, Entity entity, net.minecraft.util.hit.EntityHitResult hitResult) {
        if (hand != Hand.MAIN_HAND || !(entity instanceof ServerPlayerEntity target)) {
            return ActionResult.PASS;
        }

        ItemStack stack = player.getStackInHand(hand);
        if (!isProvider(stack)) {
            return ActionResult.PASS;
        }

        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayerEntity operator) || !operator.hasPermissionLevel(2)) {
            player.sendMessage(Text.literal("Only operators can use a stasis provider.").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        boolean enabled;
        if (isInStasis(target)) {
            target.removeScoreboardTag(STASIS_TAG);
            enabled = false;
        } else {
            target.addCommandTag(STASIS_TAG);
            enabled = true;
        }

        tick(target);
        operator.sendMessage(Text.literal(target.getGameProfile().getName()
                + (enabled ? " is now in stasis." : " was released from stasis.")), true);
        target.sendMessage(Text.literal(enabled ? "You have been placed in stasis." : "You have been released from stasis.")
                .formatted(enabled ? Formatting.AQUA : Formatting.GREEN), true);
        return ActionResult.SUCCESS;
    }

    private static boolean isProvider(ItemStack stack) {
        return stack.hasNbt()
                && stack.getNbt().contains(STASIS_PROVIDER_TAG, net.minecraft.nbt.NbtElement.NUMBER_TYPE)
                && stack.getNbt().getInt(STASIS_PROVIDER_TAG) == 1;
    }

    private static void tick(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        boolean inStasis = isInStasis(player);
        boolean wasInStasis = LAST_STASIS_STATE.contains(uuid);

        if (inStasis && !wasInStasis) {
            ANCHORS.put(uuid, new Anchor(player.getPos(), player.getYaw(), player.getPitch()));
            LAST_STASIS_STATE.add(uuid);
            sync(player);
        } else if (!inStasis && wasInStasis) {
            ANCHORS.remove(uuid);
            LAST_STASIS_STATE.remove(uuid);
            sync(player);
        }

        if (!inStasis) {
            return;
        }

        Anchor anchor = ANCHORS.computeIfAbsent(uuid, ignored -> new Anchor(player.getPos(), player.getYaw(), player.getPitch()));
        player.setVelocity(Vec3d.ZERO);
        player.velocityModified = true;
        player.fallDistance = 0.0F;

        if (player.squaredDistanceTo(anchor.position) > 0.0001D
                || player.getYaw() != anchor.yaw || player.getPitch() != anchor.pitch
                || player.age % 20 == 0) {
            player.networkHandler.requestTeleport(
                    anchor.position.x, anchor.position.y, anchor.position.z, anchor.yaw, anchor.pitch
            );
        }
    }

    private static void sync(ServerPlayerEntity player) {
        if (!ServerPlayNetworking.canSend(player, SYNC_STASIS)) {
            return;
        }

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(isInStasis(player));
        ServerPlayNetworking.send(player, SYNC_STASIS, buf);
    }

    private static void clear(ServerPlayerEntity player) {
        ANCHORS.remove(player.getUuid());
        LAST_STASIS_STATE.remove(player.getUuid());
    }

    private record Anchor(Vec3d position, float yaw, float pitch) {
    }
}
