package net.supersnetwork.fabric_utility;

import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

public final class MiniMessageFormatter {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private MiniMessageFormatter() {
    }

    public static Component parse(String input) {
        return MINI_MESSAGE.deserialize(input == null ? "" : input);
    }

    public static Text toNative(MinecraftServer server, String input) {
        return FabricServerAudiences.of(server).toNative(parse(input));
    }

    public static String plainText(String input) {
        return PLAIN_TEXT.serialize(parse(input));
    }

    public static String applyLegacyColor(String nickname, int color) {
        String hex = String.format("%06X", color & 0xFFFFFF);
        return "<#" + hex + ">" + MINI_MESSAGE.escapeTags(nickname) + "</#" + hex + ">";
    }
}
