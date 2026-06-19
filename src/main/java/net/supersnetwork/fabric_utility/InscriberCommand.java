package net.supersnetwork.fabric_utility;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class InscriberCommand {
    private static final String MARKER_KEY = "FabricUtilityInscriber";
    private static final String DISPLAY_KEY = "display";
    private static final String LORE_KEY = "Lore";
    private static final SimpleCommandExceptionType ITEM_REQUIRED =
            new SimpleCommandExceptionType(Text.literal("Hold an item in your main hand."));

    private InscriberCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("inscriber")
                        .then(CommandManager.literal("name")
                                .then(CommandManager.literal("clear")
                                        .executes(context -> clearName(context.getSource())))
                                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                        .executes(context -> setName(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "name")
                                        ))))
                        .then(CommandManager.literal("description")
                                .then(CommandManager.literal("add")
                                        .then(CommandManager.argument("line", StringArgumentType.greedyString())
                                                .executes(context -> addDescriptionLine(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "line")
                                                ))))
                                .then(CommandManager.literal("remove")
                                        .then(CommandManager.argument("line", IntegerArgumentType.integer(1))
                                                .executes(context -> removeDescriptionLine(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "line")
                                                ))))
                                .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("line", IntegerArgumentType.integer(1))
                                                .then(CommandManager.argument("text", StringArgumentType.greedyString())
                                                        .executes(context -> setDescriptionLine(
                                                                context.getSource(),
                                                                IntegerArgumentType.getInteger(context, "line"),
                                                                StringArgumentType.getString(context, "text")
                                                        )))))
                                .then(CommandManager.literal("clear")
                                        .executes(context -> clearDescription(context.getSource()))))
                        .then(CommandManager.literal("clear")
                                .executes(context -> clearInscription(context.getSource())))));
    }

    private static int setName(ServerCommandSource source, String miniMessage) throws CommandSyntaxException {
        ItemStack stack = heldItem(source);
        stack.setCustomName(MiniMessageFormatter.toNative(source.getServer(), miniMessage));
        marker(stack).putBoolean("name", true);
        source.sendFeedback(() -> Text.literal("Inscribed the held item's name."), false);
        return 1;
    }

    private static int clearName(ServerCommandSource source) throws CommandSyntaxException {
        ItemStack stack = heldItem(source);
        stack.removeCustomName();
        marker(stack).remove("name");
        cleanMarker(stack);
        source.sendFeedback(() -> Text.literal("Cleared the held item's inscribed name."), false);
        return 1;
    }

    private static int addDescriptionLine(ServerCommandSource source, String miniMessage) throws CommandSyntaxException {
        ItemStack stack = heldItem(source);
        NbtCompound display = stack.getOrCreateSubNbt(DISPLAY_KEY);
        NbtList lore = display.getList(LORE_KEY, NbtElement.STRING_TYPE);
        lore.add(NbtString.of(Text.Serializer.toJson(MiniMessageFormatter.toNative(source.getServer(), miniMessage))));
        display.put(LORE_KEY, lore);
        marker(stack).putBoolean("description", true);
        int lineNumber = lore.size();
        source.sendFeedback(() -> Text.literal("Added description line " + lineNumber + "."), false);
        return 1;
    }

    private static int removeDescriptionLine(ServerCommandSource source, int lineNumber) throws CommandSyntaxException {
        ItemStack stack = heldItem(source);
        NbtCompound display = stack.getSubNbt(DISPLAY_KEY);
        if (display == null || !display.contains(LORE_KEY, NbtElement.LIST_TYPE)) {
            source.sendError(Text.literal("The held item has no description lines."));
            return 0;
        }

        NbtList lore = display.getList(LORE_KEY, NbtElement.STRING_TYPE);
        int index = lineNumber - 1;
        if (index < 0 || index >= lore.size()) {
            source.sendError(Text.literal("Description line " + lineNumber + " does not exist."));
            return 0;
        }

        lore.remove(index);
        if (lore.isEmpty()) {
            display.remove(LORE_KEY);
            marker(stack).remove("description");
            cleanDisplay(stack, display);
            cleanMarker(stack);
        } else {
            display.put(LORE_KEY, lore);
        }

        source.sendFeedback(() -> Text.literal("Removed description line " + lineNumber + "."), false);
        return 1;
    }

    private static int setDescriptionLine(ServerCommandSource source, int lineNumber, String miniMessage) throws CommandSyntaxException {
        ItemStack stack = heldItem(source);
        NbtCompound display = stack.getSubNbt(DISPLAY_KEY);
        if (display == null || !display.contains(LORE_KEY, NbtElement.LIST_TYPE)) {
            source.sendError(Text.literal("The held item has no description lines."));
            return 0;
        }

        NbtList lore = display.getList(LORE_KEY, NbtElement.STRING_TYPE);
        int index = lineNumber - 1;
        if (index < 0 || index >= lore.size()) {
            source.sendError(Text.literal("Description line " + lineNumber + " does not exist."));
            return 0;
        }

        lore.set(index, NbtString.of(Text.Serializer.toJson(MiniMessageFormatter.toNative(source.getServer(), miniMessage))));
        display.put(LORE_KEY, lore);
        marker(stack).putBoolean("description", true);
        source.sendFeedback(() -> Text.literal("Updated description line " + lineNumber + "."), false);
        return 1;
    }

    private static int clearDescription(ServerCommandSource source) throws CommandSyntaxException {
        ItemStack stack = heldItem(source);
        NbtCompound display = stack.getSubNbt(DISPLAY_KEY);
        if (display != null) {
            display.remove(LORE_KEY);
            cleanDisplay(stack, display);
        }
        marker(stack).remove("description");
        cleanMarker(stack);
        source.sendFeedback(() -> Text.literal("Cleared the held item's description."), false);
        return 1;
    }

    private static int clearInscription(ServerCommandSource source) throws CommandSyntaxException {
        ItemStack stack = heldItem(source);
        NbtCompound existingMarker = stack.getSubNbt(MARKER_KEY);
        if (existingMarker == null) {
            source.sendError(Text.literal("The held item has not been inscribed."));
            return 0;
        }

        if (existingMarker.getBoolean("name")) {
            stack.removeCustomName();
        }
        if (existingMarker.getBoolean("description")) {
            NbtCompound display = stack.getSubNbt(DISPLAY_KEY);
            if (display != null) {
                display.remove(LORE_KEY);
                cleanDisplay(stack, display);
            }
        }

        stack.removeSubNbt(MARKER_KEY);
        source.sendFeedback(() -> Text.literal("Cleared all inscriptions from the held item."), false);
        return 1;
    }

    private static ItemStack heldItem(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty()) {
            throw ITEM_REQUIRED.create();
        }
        return stack;
    }

    private static NbtCompound marker(ItemStack stack) {
        return stack.getOrCreateSubNbt(MARKER_KEY);
    }

    private static void cleanDisplay(ItemStack stack, NbtCompound display) {
        if (display.isEmpty()) {
            stack.removeSubNbt(DISPLAY_KEY);
        }
    }

    private static void cleanMarker(ItemStack stack) {
        NbtCompound marker = stack.getSubNbt(MARKER_KEY);
        if (marker != null && marker.isEmpty()) {
            stack.removeSubNbt(MARKER_KEY);
        }
    }
}
