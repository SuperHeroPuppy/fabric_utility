package net.supersnetwork.fabric_utility;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public final class BanHammerHandler {
    public static final String BAN_HAMMER_TAG = "BanHammer";
    private static final String DEFAULT_REASON = "the ban hammer has spoken";

    private BanHammerHandler() {
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer) || !(entity instanceof ServerPlayerEntity target)) {
                return ActionResult.PASS;
            }

            ItemStack stack = player.getStackInHand(hand);
            if (!isBanHammer(stack)) {
                return ActionResult.PASS;
            }

            if (!serverPlayer.hasPermissionLevel(2)) {
                serverPlayer.sendMessage(Text.literal("Only operators can use the ban hammer.").formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            }

            String reason = getReason(stack);
            GameProfile profile = target.getGameProfile();
            Date expires = Date.from(Instant.now().plus(7, ChronoUnit.DAYS));
            serverPlayer.getServer().getPlayerManager().getUserBanList().add(new BannedPlayerEntry(profile, new Date(), serverPlayer.getName().getString(), expires, reason));
            target.networkHandler.disconnect(Text.literal("Banned for 7 days: " + reason));
            serverPlayer.sendMessage(Text.literal(target.getName().getString() + " was banned for 7 days."), true);
            return ActionResult.SUCCESS;
        });
    }

    private static boolean isBanHammer(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        return customData != null && customData.contains(BAN_HAMMER_TAG);
    }

    private static String getReason(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound nbt = customData == null ? null : customData.getNbt();
        if (nbt != null && nbt.contains(BAN_HAMMER_TAG, NbtElement.STRING_TYPE)) {
            String reason = nbt.getString(BAN_HAMMER_TAG);
            return reason.isBlank() ? DEFAULT_REASON : reason;
        }

        return DEFAULT_REASON;
    }
}
