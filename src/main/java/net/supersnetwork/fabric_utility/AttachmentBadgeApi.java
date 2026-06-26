package net.supersnetwork.fabric_utility;

import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * Independent API for appending a badge to an already-resolved display name.
 *
 * <p>This system never reads, writes, selects, clears, or refreshes nicknames.</p>
 */
public final class AttachmentBadgeApi {
    private static final char BADGE_GLYPH = '\uE000';
    private static final String FALLBACK_BADGE_FONT = "missing_asset";

    private AttachmentBadgeApi() {
    }

    public static boolean hasAttachment(ServerPlayerEntity player) {
        return AttachmentBadgeManager.selectedBadge(player).isPresent();
    }

    public static Text attach(ServerPlayerEntity player, Text resolvedName) {
        Optional<String> selected = AttachmentBadgeManager.selectedBadge(player);
        if (selected.isEmpty()) {
            return resolvedName;
        }

        String badgeId = selected.get();
        String fontBadgeId = AttachmentBadgeManager.isBundledBadge(badgeId) ? badgeId : FALLBACK_BADGE_FONT;
        Style style = Style.EMPTY
                .withFont(new Identifier(FabricUtility.MOD_ID, "badge/" + fontBadgeId))
                .withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        badgeHoverText(badgeId)
                ));

        return resolvedName.copy()
                .append(Text.literal(" "))
                .append(Text.literal(String.valueOf(BADGE_GLYPH)).setStyle(style));
    }

    private static Text badgeHoverText(String badgeId) {
        if (AttachmentBadgeManager.isBundledBadge(badgeId)) {
            return Text.literal(AttachmentBadgeManager.badgeDescription(badgeId));
        }

        return Text.literal("The badge '" + badgeId + "' is on your account, but this mod version does not include its asset yet. Update Fabric Utility to see it.");
    }

    public static void refreshPlayerList(ServerPlayerEntity player) {
        player.getServer().getPlayerManager().sendToAll(
                new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME, player)
        );
    }
}
