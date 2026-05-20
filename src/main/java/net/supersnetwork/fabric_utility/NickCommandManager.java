package net.supersnetwork.fabric_utility;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NickCommandManager {
    private static final AtomicBoolean SENDING_REPLACEMENT_MESSAGE = new AtomicBoolean(false);
    private static final Map<String, String> KNOWN_NAME_REPLACEMENTS = new LinkedHashMap<>();

    private NickCommandManager() {
    }

    public static void register() {
        registerCommands();
        registerMessages();
        registerPlayerListNames();
    }

    public static Optional<String> getNickname(ServerPlayerEntity player) {
        return NicknameSavedData.get(player.getServer()).getNickname(player);
    }

    public static String getEffectiveName(ServerPlayerEntity player) {
        return getNickname(player).orElse(player.getName().getString());
    }

    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("nick")
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("nickname", StringArgumentType.string())
                                        .executes(context -> {
                                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                            String nickname = StringArgumentType.getString(context, "nickname");
                                            NicknameSavedData.get(context.getSource().getServer()).addNickname(player, nickname);
                                            updateNicknameSurfaces(player);
                                            context.getSource().sendFeedback(() -> Text.literal("Nickname added and set: " + nickname), true);
                                            return 1;
                                        })))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("nickname", StringArgumentType.string())
                                        .suggests(NickCommandManager::suggestHistory)
                                        .executes(context -> {
                                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                            String nickname = StringArgumentType.getString(context, "nickname");
                                            NicknameSavedData.get(context.getSource().getServer()).removeFromHistory(player, nickname);
                                            updateNicknameSurfaces(player);
                                            context.getSource().sendFeedback(() -> Text.literal("Removed nickname from history: " + nickname), true);
                                            return 1;
                                        })))
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument("nickname", StringArgumentType.string())
                                        .suggests(NickCommandManager::suggestHistory)
                                        .executes(context -> {
                                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                            String nickname = StringArgumentType.getString(context, "nickname");
                                            NicknameSavedData data = NicknameSavedData.get(context.getSource().getServer());

                                            if (!data.getHistory(player).contains(nickname)) {
                                                context.getSource().sendError(Text.literal("Nickname not in your history!"));
                                                return 0;
                                            }

                                            data.setNickname(player, nickname);
                                            updateNicknameSurfaces(player);
                                            context.getSource().sendFeedback(() -> Text.literal("Nickname switched to: " + nickname), true);
                                            return 1;
                                        })))
                        .then(CommandManager.literal("list")
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                    List<String> history = NicknameSavedData.get(context.getSource().getServer()).getHistory(player);
                                    context.getSource().sendFeedback(() -> Text.literal("Your nicknames: " + (history.isEmpty() ? "None" : String.join(", ", history))), true);
                                    return 1;
                                }))
                        .then(CommandManager.literal("clear")
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                    NicknameSavedData.get(context.getSource().getServer()).clearNickname(player);
                                    updateNicknameSurfaces(player);
                                    context.getSource().sendFeedback(() -> Text.literal("Current nickname cleared."), true);
                                    return 1;
                                }))
                        .then(CommandManager.literal("admin")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("nickname", StringArgumentType.string())
                                                        .executes(context -> {
                                                            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                                                            String nickname = StringArgumentType.getString(context, "nickname");
                                                            String targetName = getEffectiveName(target);
                                                            NicknameSavedData data = NicknameSavedData.get(context.getSource().getServer());
                                                            if (target.getWorld().getGameRules().getBoolean(FabricUtilityGameRules.ADMIN_NICKNAME_CHANGES_AFFECT_HISTORY)) {
                                                                data.addNickname(target, nickname);
                                                            } else {
                                                                data.setNickname(target, nickname);
                                                            }
                                                            updateNicknameSurfaces(target);
                                                            context.getSource().sendFeedback(() -> Text.literal("Set nickname for " + targetName + " to " + nickname), true);
                                                            return 1;
                                                        }))))
                                .then(CommandManager.literal("clear")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> {
                                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                                                    String targetName = getEffectiveName(target);
                                                    NicknameSavedData.get(context.getSource().getServer()).clearNickname(target);
                                                    updateNicknameSurfaces(target);
                                                    context.getSource().sendFeedback(() -> Text.literal("Cleared nickname for " + targetName), true);
                                                    return 1;
                                                })))
                                .then(CommandManager.literal("discover")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> {
                                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                                                    Optional<String> nickname = getNickname(target);

                                                    if (nickname.isPresent()) {
                                                        context.getSource().sendFeedback(() -> Text.literal(getEffectiveName(target) + "'s current nickname: " + nickname.get()), true);
                                                    } else {
                                                        context.getSource().sendError(Text.literal(target.getName().getString() + " has no nickname."));
                                                    }

                                                    return 1;
                                                })))
                                .then(CommandManager.literal("history")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> {
                                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                                                    List<String> history = NicknameSavedData.get(context.getSource().getServer()).getHistory(target);
                                                    context.getSource().sendFeedback(() -> Text.literal(getEffectiveName(target) + "'s nicknames: " + (history.isEmpty() ? "None" : String.join(", ", history))), true);
                                                    return 1;
                                                })))
                                .then(CommandManager.literal("list")
                                        .executes(NickCommandManager::listAdmin)))));
    }

    private static void registerMessages() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            // Handle proxy chat system
            if (FabricUtilityConfig.proxyChatEnabled()) {
                String text = message.getContent().getString().trim();
                if (!text.isEmpty()) {
                    // Route through proxy chat based on player's pinned mode
                    ChatStateSavedData chatData = ChatStateSavedData.get(sender.getServer());
                    ChatStateSavedData.ChatPinMode pinMode = chatData.getPinMode(sender);
                    
                    ProxyChatManager.sendChatForMode(sender, text, true, pinMode, chatData.getPinnedArea(sender));
                    return false;
                }
            }

            if (!FabricUtilityConfig.nicknameSystemEnabled()) {
                return true;
            }

            Optional<String> nickname = getNickname(sender);

            if (nickname.isEmpty() || SENDING_REPLACEMENT_MESSAGE.get()) {
                return true;
            }

            try {
                SENDING_REPLACEMENT_MESSAGE.set(true);
                sender.getServer().getPlayerManager().broadcast(Text.literal("<" + nickname.get() + "> " + message.getContent().getString()), false);
            } finally {
                SENDING_REPLACEMENT_MESSAGE.set(false);
            }

            return false;
        });

        ServerMessageEvents.ALLOW_GAME_MESSAGE.register((server, message, overlay) -> {
            if (!FabricUtilityConfig.nicknameSystemEnabled()) {
                return true;
            }

            if (overlay || SENDING_REPLACEMENT_MESSAGE.get()) {
                return true;
            }

            String original = message.getString();
            Text replaced = replaceKnownPlayerNames(server.getPlayerManager().getPlayerList(), message);

            if (replaced.getString().equals(original)) {
                return true;
            }

            try {
                SENDING_REPLACEMENT_MESSAGE.set(true);
                server.getPlayerManager().broadcast(replaced, false);
            } finally {
                SENDING_REPLACEMENT_MESSAGE.set(false);
            }

            return false;
        });
    }

    private static void registerPlayerListNames() {
        ServerPlayConnectionEvents.INIT.register((handler, server) -> {
            rememberNameReplacement(handler.player);
            updateNameplate(handler.player);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> updateNicknameSurfaces(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> rememberNameReplacement(handler.player));
    }

    private static void updatePlayerListName(ServerPlayerEntity player) {
        player.getServer().getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME, player));
    }

    private static void updateNicknameSurfaces(ServerPlayerEntity player) {
        rememberNameReplacement(player);
        updateNameplate(player);
        updatePlayerListName(player);
    }

    private static void updateNameplate(ServerPlayerEntity player) {
        Optional<String> nickname = FabricUtilityConfig.nicknameSystemEnabled() ? getNickname(player) : Optional.empty();
        if (nickname.isPresent()) {
            player.setCustomName(Text.literal(nickname.get()));
            player.setCustomNameVisible(true);
        } else {
            player.setCustomName(null);
            player.setCustomNameVisible(false);
        }
    }

    private static void rememberNameReplacement(ServerPlayerEntity player) {
        if (!FabricUtilityConfig.nicknameSystemEnabled()) {
            KNOWN_NAME_REPLACEMENTS.remove(player.getName().getString());
            return;
        }

        Optional<String> nickname = getNickname(player);
        if (nickname.isPresent()) {
            KNOWN_NAME_REPLACEMENTS.put(player.getName().getString(), nickname.get());
        } else {
            KNOWN_NAME_REPLACEMENTS.remove(player.getName().getString());
        }
    }

    private static Text replaceKnownPlayerNames(List<ServerPlayerEntity> onlinePlayers, Text message) {
        for (ServerPlayerEntity player : onlinePlayers) {
            rememberNameReplacement(player);
        }

        return replaceNamesInText(message);
    }

    private static Text replaceNamesInText(Text text) {
        MutableText replaced;

        if (text.getContent() instanceof LiteralTextContent literal) {
            replaced = Text.literal(replaceKnownPlayerNames(literal.string()));
        } else if (text.getContent() instanceof TranslatableTextContent translatable) {
            Object[] args = translatable.getArgs();
            Object[] replacedArgs = new Object[args.length];

            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg instanceof Text argText) {
                    replacedArgs[i] = replaceNamesInText(argText);
                } else if (arg instanceof String argString) {
                    replacedArgs[i] = replaceKnownPlayerNames(argString);
                } else {
                    replacedArgs[i] = arg;
                }
            }

            String fallback = translatable.getFallback();
            replaced = Text.translatableWithFallback(translatable.getKey(), fallback == null ? null : replaceKnownPlayerNames(fallback), replacedArgs);
        } else {
            replaced = text.copyContentOnly();
        }

        replaced.setStyle(text.getStyle());
        for (Text sibling : text.getSiblings()) {
            replaced.append(replaceNamesInText(sibling));
        }
        return replaced;
    }

    private static String replaceKnownPlayerNames(String message) {
        String replaced = message;
        for (Map.Entry<String, String> entry : KNOWN_NAME_REPLACEMENTS.entrySet()) {
            replaced = replaced.replace(entry.getKey(), entry.getValue());
        }
        return replaced;
    }

    private static CompletableFuture<Suggestions> suggestHistory(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            for (String nickname : NicknameSavedData.get(context.getSource().getServer()).getHistory(player)) {
                if (nickname.toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                    builder.suggest(nickname);
                }
            }
        } catch (Exception ignored) {
        }

        return builder.buildFuture();
    }

    private static int listAdmin(CommandContext<ServerCommandSource> context) {
        StringBuilder result = new StringBuilder();

        for (ServerPlayerEntity player : context.getSource().getServer().getPlayerManager().getPlayerList()) {
            result.append(getEffectiveName(player))
                    .append(" -> ")
                    .append(getNickname(player).orElse("None"))
                    .append(", ");
        }

        if (result.length() >= 2) {
            result.setLength(result.length() - 2);
        }

        context.getSource().sendFeedback(() -> Text.literal("Players and nicknames: " + result), true);
        return 1;
    }
}
