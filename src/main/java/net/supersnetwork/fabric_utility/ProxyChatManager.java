package net.supersnetwork.fabric_utility;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.supersnetwork.fabric_utility.api.NicknameApi;
import net.supersnetwork.fabric_utility.mixin.CommandNodeAccessor;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class ProxyChatManager {
    private static final int MAX_AREA_RADIUS = 10;
    private static final String AREA_TAG_PREFIX = "proxy_area:";

    private ProxyChatManager() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
    }

    public static boolean tryHandleChatMessage(SignedMessage message, ServerPlayerEntity sender) {
        if (!FabricUtilityConfig.proxyChatEnabled()) {
            return false;
        }

        String text = message.getContent().getString().trim();
        if (text.isEmpty()) {
            return false;
        }

        ChatStateSavedData chatData = ChatStateSavedData.get(sender.getServer());
        ChatStateSavedData.ChatPinMode pinMode = chatData.getPinMode(sender);
        String pinnedArea = chatData.getPinnedArea(sender);
        sendChatForMode(sender, text, true, pinMode, pinnedArea);
        return true;
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        // /m local/world - messaging commands
        var messageCommand = CommandManager.literal("m");

        messageCommand.then(CommandManager.literal("local")
                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                        .executes(context -> sendMessage(context, ChatChannel.LOCAL, StringArgumentType.getString(context, "message")))));

        messageCommand.then(CommandManager.literal("world")
                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                        .executes(context -> sendMessage(context, ChatChannel.WORLD, StringArgumentType.getString(context, "message")))));

        dispatcher.register(messageCommand);

        // /w or /tell - whisper/private message
        var whisperCommand = CommandManager.literal("w")
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                .executes(context -> sendWhisper(context, EntityArgumentType.getPlayer(context, "target"), StringArgumentType.getString(context, "message")))));

        dispatcher.register(whisperCommand);

        var tellCommand = CommandManager.literal("tell")
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                .executes(context -> sendWhisper(context, EntityArgumentType.getPlayer(context, "target"), StringArgumentType.getString(context, "message")))));

        dispatcher.register(tellCommand);

        // Fully replace vanilla /me so its source name always resolves through NicknameApi.
        removeRootCommand(dispatcher, "me");
        var meCommand = CommandManager.literal("me")
                .then(CommandManager.argument("message", StringArgumentType.greedyString())
                        .executes(context -> sendAction(context, StringArgumentType.getString(context, "message"))));

        dispatcher.register(meCommand);

        // /pin - standalone pinning command
        var pinCommand = CommandManager.literal("pin")
                .then(CommandManager.literal("none")
                        .executes(context -> setPinnedMode(context, ChatStateSavedData.ChatPinMode.NONE, "")))
                .then(CommandManager.literal("world")
                        .executes(context -> setPinnedMode(context, ChatStateSavedData.ChatPinMode.WORLD, "")))
                .then(CommandManager.literal("local")
                        .executes(context -> setPinnedMode(context, ChatStateSavedData.ChatPinMode.LOCAL, "")))
                .then(CommandManager.literal("area")
                        .then(CommandManager.argument("area", StringArgumentType.word())
                                .suggests(ProxyChatManager::suggestAreas)
                                .executes(context -> setPinnedArea(context, StringArgumentType.getString(context, "area")))));

        dispatcher.register(pinCommand);

        // /proxy commands - do not disturb, range, area management
        var proxyCommand = CommandManager.literal("proxy");

        proxyCommand.then(CommandManager.literal("dnd")
                .then(CommandManager.literal("on")
                        .executes(context -> setDoNotDisturb(context, true)))
                .then(CommandManager.literal("off")
                        .executes(context -> setDoNotDisturb(context, false))));

        proxyCommand.then(CommandManager.literal("range")
                .requires(source -> source.hasPermissionLevel(2)) // Admin only
                .then(CommandManager.argument("chunks", IntegerArgumentType.integer(1, 16))
                        .executes(context -> setRange(context, IntegerArgumentType.getInteger(context, "chunks")))));

        proxyCommand.then(CommandManager.literal("area")
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .then(CommandManager.argument("radius", IntegerArgumentType.integer(1, MAX_AREA_RADIUS))
                                        .executes(context -> createArea(context, StringArgumentType.getString(context, "name"), IntegerArgumentType.getInteger(context, "radius"))))))
                .then(CommandManager.literal("delete")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .executes(context -> deleteArea(context, StringArgumentType.getString(context, "name")))))
                .then(CommandManager.literal("list")
                        .executes(ProxyChatManager::listAreas)));

        dispatcher.register(proxyCommand);
    }

    @SuppressWarnings("unchecked")
    private static void removeRootCommand(CommandDispatcher<ServerCommandSource> dispatcher, String name) {
        CommandNodeAccessor<ServerCommandSource> root = (CommandNodeAccessor<ServerCommandSource>) (Object) dispatcher.getRoot();
        root.fabricUtility$getChildren().remove(name);
        root.fabricUtility$getLiterals().remove(name);
        root.fabricUtility$getArguments().remove(name);
    }

    private static int sendMessage(CommandContext<ServerCommandSource> context, ChatChannel channel, String message) throws CommandSyntaxException {
        ServerPlayerEntity sender = context.getSource().getPlayerOrThrow();
        sendChat(sender, message, true, channel);
        return 1;
    }

    private static int sendWhisper(CommandContext<ServerCommandSource> context, ServerPlayerEntity target, String message) throws CommandSyntaxException {
        ServerPlayerEntity sender = context.getSource().getPlayerOrThrow();

        if (ChatStateSavedData.get(context.getSource().getServer()).isDoNotDisturb(target)) {
            context.getSource().sendError(Text.literal(target.getName().getString() + " is in Do Not Disturb."));
            return 0;
        }

        Text displayName = NicknameApi.getDisplayName(sender);
        Text messageBody = MiniMessageFormatter.toNative(sender.getServer(), message);
        Text whisperMessage = Text.literal("[Whisper] <").append(displayName).append(Text.literal("> ")).append(messageBody);
        target.sendMessage(whisperMessage, false);
        sender.sendMessage(Text.literal("[Whisper to ").append(NicknameApi.getDisplayName(target)).append(Text.literal("] ")).append(messageBody), false);
        return 1;
    }

    private static int sendAction(CommandContext<ServerCommandSource> context, String message) throws CommandSyntaxException {
        ServerPlayerEntity sender = context.getSource().getPlayerOrThrow();
        Text actionMessage = Text.literal("* ")
                .append(NicknameApi.getDisplayName(sender))
                .append(Text.literal(" "))
                .append(MiniMessageFormatter.toNative(sender.getServer(), message));

        for (ServerPlayerEntity recipient : sender.getServer().getPlayerManager().getPlayerList()) {
            if (!FabricUtilityConfig.proxyChatEnabled() || shouldReceive(recipient, sender, ChatChannel.WORLD)) {
                recipient.sendMessage(actionMessage, false);
            }
        }
        return 1;
    }

    private static int setPinnedMode(CommandContext<ServerCommandSource> context, ChatStateSavedData.ChatPinMode mode, String areaName) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ChatStateSavedData.get(context.getSource().getServer()).setPinMode(player, mode, areaName);
        context.getSource().sendFeedback(() -> Text.literal("Pinned chat mode set to " + mode.name().toLowerCase()), true);
        return 1;
    }

    private static int setPinnedArea(CommandContext<ServerCommandSource> context, String areaName) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerWorld world = player.getServerWorld();

        if (!TaggedChunksSavedData.get(world).hasProxyAreaAt(world.getRegistryKey().getValue(), player.getChunkPos().x, player.getChunkPos().z, areaName)) {
            context.getSource().sendError(Text.literal("Area '" + areaName + "' does not exist in this world."));
            return 0;
        }

        ChatStateSavedData.get(context.getSource().getServer()).setPinMode(player, ChatStateSavedData.ChatPinMode.AREA, areaName);
        context.getSource().sendFeedback(() -> Text.literal("Pinned chat area to '" + areaName + "'."), true);
        return 1;
    }

    private static int setDoNotDisturb(CommandContext<ServerCommandSource> context, boolean enabled) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ChatStateSavedData.get(context.getSource().getServer()).setDoNotDisturb(player, enabled);
        context.getSource().sendFeedback(() -> Text.literal("Do Not Disturb " + (enabled ? "enabled" : "disabled")), true);
        return 1;
    }

    private static int setRange(CommandContext<ServerCommandSource> context, int chunks) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ChatStateSavedData.get(context.getSource().getServer()).setRangeChunks(player, chunks);
        context.getSource().sendFeedback(() -> Text.literal("Proxy chat range set to " + chunks + " chunks."), true);
        return 1;
    }

    private static int createArea(CommandContext<ServerCommandSource> context, String name, int radius) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerWorld world = player.getServerWorld();
        BlockPos pos = player.getBlockPos();
        int centerX = pos.getX() >> 4;
        int centerZ = pos.getZ() >> 4;
        TaggedChunksSavedData data = TaggedChunksSavedData.get(world);

        if (radius > MAX_AREA_RADIUS) {
            context.getSource().sendError(Text.literal("Maximum area radius is " + MAX_AREA_RADIUS + " chunks."));
            return 0;
        }

        boolean created = data.createProxyArea(world.getRegistryKey().getValue(), centerX, centerZ, radius, name);
        if (!created) {
            context.getSource().sendError(Text.literal("Area '" + name + "' already exists in all selected chunks."));
            return 0;
        }

        context.getSource().sendFeedback(() -> Text.literal("Created area '" + name + "' with radius " + radius + " chunks."), true);
        return 1;
    }

    private static int deleteArea(CommandContext<ServerCommandSource> context, String name) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        ServerWorld world = player.getServerWorld();
        boolean deleted = TaggedChunksSavedData.get(world).deleteProxyArea(name);

        if (!deleted) {
            context.getSource().sendError(Text.literal("Area '" + name + "' was not found."));
            return 0;
        }

        context.getSource().sendFeedback(() -> Text.literal("Removed area '" + name + "'."), true);
        return 1;
    }

    private static int listAreas(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        Set<String> areas = TaggedChunksSavedData.get(player.getServerWorld()).getProxyAreaNames();

        if (areas.isEmpty()) {
            context.getSource().sendFeedback(() -> Text.literal("No proxy areas are defined."), true);
            return 1;
        }

        context.getSource().sendFeedback(() -> Text.literal("Proxy areas: " + String.join(", ", areas)), true);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestAreas(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        for (String area : TaggedChunksSavedData.get(player.getServerWorld()).getProxyAreaNames()) {
            if (area.toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                builder.suggest(area);
            }
        }
        return builder.buildFuture();
    }

    public static void sendChatForMode(ServerPlayerEntity sender, String rawText, boolean useNickname, ChatStateSavedData.ChatPinMode pinMode, String pinnedArea) {
        switch (pinMode) {
            case WORLD:
                sendChat(sender, rawText, useNickname, ChatChannel.WORLD);
                break;
            case AREA:
                sendAreaChat(sender, rawText, useNickname, pinnedArea);
                break;
            case LOCAL:
            default:
                sendChat(sender, rawText, useNickname, ChatChannel.LOCAL);
                break;
        }
    }

    private static void sendAreaChat(ServerPlayerEntity sender, String rawText, boolean useNickname, String areaName) {
        if (areaName == null || areaName.isBlank()) {
            sendChat(sender, rawText, useNickname, ChatChannel.LOCAL);
            return;
        }

        Text displayName = useNickname && FabricUtilityConfig.nicknameSystemEnabled()
                ? NicknameApi.getDisplayName(sender)
                : Text.literal(sender.getGameProfile().getName());
        Text message = formatMessage(sender, ChatChannel.LOCAL, displayName, rawText);

        for (ServerPlayerEntity recipient : sender.getServer().getPlayerManager().getPlayerList()) {
            if (recipient == sender) {
                recipient.sendMessage(message, false);
                continue;
            }

            if (shouldReceive(recipient, sender, ChatChannel.LOCAL)) {
                if (isInSameArea(sender, recipient, areaName)) {
                    recipient.sendMessage(message, false);
                }
            }
        }
    }

    private static void sendChat(ServerPlayerEntity sender, String rawText, boolean useNickname, ChatChannel channel) {
        Text displayName = useNickname && FabricUtilityConfig.nicknameSystemEnabled()
                ? NicknameApi.getDisplayName(sender)
                : Text.literal(sender.getGameProfile().getName());
        Text message = formatMessage(sender, channel, displayName, rawText);

        for (ServerPlayerEntity recipient : sender.getServer().getPlayerManager().getPlayerList()) {
            if (shouldReceive(recipient, sender, channel)) {
                recipient.sendMessage(message, false);
            }
        }
    }

    private static boolean shouldReceive(ServerPlayerEntity recipient, ServerPlayerEntity sender, ChatChannel channel) {
        if (recipient == sender) {
            return true;
        }

        ChatStateSavedData savedData = ChatStateSavedData.get(recipient.getServer());
        if (savedData.isDoNotDisturb(recipient)) {
            return false;
        }

        ChatStateSavedData.ChatPinMode pinMode = savedData.getPinMode(recipient);
        switch (pinMode) {
            case WORLD:
                return channel == ChatChannel.WORLD;
            case LOCAL:
                return channel == ChatChannel.LOCAL && isWithinRange(sender, recipient);
            case AREA:
                return channel == ChatChannel.LOCAL && isInSameArea(sender, recipient, savedData.getPinnedArea(recipient));
            default:
                return channel == ChatChannel.WORLD || isWithinRange(sender, recipient);
        }
    }

    private static boolean isWithinRange(ServerPlayerEntity sender, ServerPlayerEntity recipient) {
        if (!sender.getWorld().getRegistryKey().equals(recipient.getWorld().getRegistryKey())) {
            return false;
        }

        int senderChunkX = sender.getBlockPos().getX() >> 4;
        int senderChunkZ = sender.getBlockPos().getZ() >> 4;
        int recipientChunkX = recipient.getBlockPos().getX() >> 4;
        int recipientChunkZ = recipient.getBlockPos().getZ() >> 4;
        int dx = Math.abs(senderChunkX - recipientChunkX);
        int dz = Math.abs(senderChunkZ - recipientChunkZ);
        int range = ChatStateSavedData.get(sender.getServer()).getRangeChunks(sender);
        return Math.max(dx, dz) <= range;
    }

    private static boolean isInSameArea(ServerPlayerEntity sender, ServerPlayerEntity recipient, String areaName) {
        if (areaName == null || areaName.isBlank()) {
            return false;
        }

        if (!sender.getWorld().getRegistryKey().equals(recipient.getWorld().getRegistryKey())) {
            return false;
        }

        ServerWorld world = sender.getServerWorld();
        return TaggedChunksSavedData.get(world).hasProxyAreaAt(world.getRegistryKey().getValue(),
                sender.getBlockPos().getX() >> 4,
                sender.getBlockPos().getZ() >> 4,
                areaName)
                && TaggedChunksSavedData.get(world).hasProxyAreaAt(world.getRegistryKey().getValue(),
                recipient.getBlockPos().getX() >> 4,
                recipient.getBlockPos().getZ() >> 4,
                areaName);
    }

    private static Text formatMessage(ServerPlayerEntity sender, ChatChannel channel, Text displayName, String rawText) {
        Text messageBody = MiniMessageFormatter.toNative(sender.getServer(), rawText);
        if (channel == ChatChannel.WORLD) {
            return Text.literal("[World] <").append(displayName).append(Text.literal("> ")).append(messageBody);
        }
        return Text.literal("<").append(displayName).append(Text.literal("> ")).append(messageBody);
    }

    private enum ChatChannel {
        LOCAL,
        WORLD
    }
}
