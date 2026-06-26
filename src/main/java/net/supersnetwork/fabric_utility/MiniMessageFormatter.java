package net.supersnetwork.fabric_utility;

import com.google.gson.JsonElement;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class MiniMessageFormatter {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private MiniMessageFormatter() {
    }

    public static Component parse(String input) {
        return MINI_MESSAGE.deserialize(input == null ? "" : input);
    }

    public static Text toNative(MinecraftServer server, String input) {
        try {
            JsonElement json = GSON.serializeToTree(parse(input));
            Gson minecraftTextGson = new GsonBuilder()
                    .registerTypeHierarchyAdapter(Text.class, new Text.Serializer(server.getRegistryManager()))
                    .create();
            MutableText text = minecraftTextGson.fromJson(json, MutableText.class);
            return text == null ? Text.empty() : text;
        } catch (RuntimeException exception) {
            FabricUtility.LOGGER.warn("Could not parse MiniMessage text, falling back to literal text", exception);
            return Text.literal(input == null ? "" : input);
        }
    }

    public static String plainText(String input) {
        return PLAIN_TEXT.serialize(parse(input));
    }

    public static String applyLegacyColor(String nickname, int color) {
        String hex = String.format("%06X", color & 0xFFFFFF);
        return "<#" + hex + ">" + MINI_MESSAGE.escapeTags(nickname) + "</#" + hex + ">";
    }
}
