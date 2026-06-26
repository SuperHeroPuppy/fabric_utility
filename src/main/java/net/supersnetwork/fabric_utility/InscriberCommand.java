package net.supersnetwork.fabric_utility;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class InscriberCommand {
    private static final String MARKER_KEY = "FabricUtilityInscriber";
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
        stack.set(DataComponentTypes.CUSTOM_NAME, MiniMessageFormatter.toNative(source.getServer(), miniMessage));
        NbtCompound customData = customData(stack);
        marker(customData).putBoolean("name", true);
        saveCustomData(stack, customData);
        source.sendFeedback(() -> Text.literal("Inscribed the held item's name."), false);
        return 1;
    }

    private static int clearName(ServerCommandSource source) throws CommandSyntaxException {
        ItemStack stack = heldItem(source);
        stack.remove(DataComponentTypes.CUSTOM_NAME);
        NbtCompound customData = customData(stack);
        marker(customData).remove("name");
        cleanMarker(customData);
        saveCustomData(stack, customData);
        source.sendFeedback(() -> Text.literal("Cleared the held item's inscribed name."), false);
        return 1;
    }

    private static int addDescriptionLine(ServerCommandSource source, String miniMessage) throws CommandSyntaxException {
        ItemStack stack = heldItem(source);
        List<Text> lore = lore(stack);
        lore.add(MiniMessageFormatter.toNative(source.getServer(), miniMessage));
        setLore(stack, lore);
        NbtCompound customData = customData(stack);
        marker(customData).putBoolean("description", true);
        saveCustomData(stack, customData);
        int lineNumber = lore.size();
        source.sendFeedback(() -> Text.literal("Added description line " + lineNumber + "."), false);
        return 1;
    }

    private static int removeDescriptionLine(ServerCommandSource source, int lineNumber) throws CommandSyntaxException {
        ItemStack stack = heldItem(source);
        List<Text> lore = lore(stack);
        if (lore.isEmpty()) {
            source.sendError(Text.literal("The held item has no description lines."));
            return 0;
        }

        int index = lineNumber - 1;
        if (index < 0 || index >= lore.size()) {
            source.sendError(Text.literal("Description line " + lineNumber + " does not exist."));
            return 0;
        }

        lore.remove(index);
        setLore(stack, lore);
        if (lore.isEmpty()) {
            NbtCompound customData = customData(stack);
            marker(customData).remove("description");
            cleanMarker(customData);
            saveCustomData(stack, customData);
        }

        source.sendFeedback(() -> Text.literal("Removed description line " + lineNumber + "."), false);
        return 1;
    }

    private static int setDescriptionLine(ServerCommandSource source, int lineNumber, String miniMessage) throws CommandSyntaxException {
        ItemStack stack = heldItem(source);
        List<Text> lore = lore(stack);
        if (lore.isEmpty()) {
            source.sendError(Text.literal("The held item has no description lines."));
            return 0;
        }

        int index = lineNumber - 1;
        if (index < 0 || index >= lore.size()) {
            source.sendError(Text.literal("Description line " + lineNumber + " does not exist."));
            return 0;
        }

        lore.set(index, MiniMessageFormatter.toNative(source.getServer(), miniMessage));
        setLore(stack, lore);
        NbtCompound customData = customData(stack);
        marker(customData).putBoolean("description", true);
        saveCustomData(stack, customData);
        source.sendFeedback(() -> Text.literal("Updated description line " + lineNumber + "."), false);
        return 1;
    }

    private static int clearDescription(ServerCommandSource source) throws CommandSyntaxException {
        ItemStack stack = heldItem(source);
        stack.remove(DataComponentTypes.LORE);
        NbtCompound customData = customData(stack);
        marker(customData).remove("description");
        cleanMarker(customData);
        saveCustomData(stack, customData);
        source.sendFeedback(() -> Text.literal("Cleared the held item's description."), false);
        return 1;
    }

    private static int clearInscription(ServerCommandSource source) throws CommandSyntaxException {
        ItemStack stack = heldItem(source);
        NbtCompound customData = customData(stack);
        if (!customData.contains(MARKER_KEY, NbtElement.COMPOUND_TYPE)) {
            source.sendError(Text.literal("The held item has not been inscribed."));
            return 0;
        }

        NbtCompound existingMarker = customData.getCompound(MARKER_KEY);
        if (existingMarker.getBoolean("name")) {
            stack.remove(DataComponentTypes.CUSTOM_NAME);
        }
        if (existingMarker.getBoolean("description")) {
            stack.remove(DataComponentTypes.LORE);
        }

        customData.remove(MARKER_KEY);
        saveCustomData(stack, customData);
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

    private static List<Text> lore(ItemStack stack) {
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        return lore == null ? new ArrayList<>() : new ArrayList<>(lore.lines());
    }

    private static void setLore(ItemStack stack, List<Text> lore) {
        if (lore.isEmpty()) {
            stack.remove(DataComponentTypes.LORE);
        } else {
            stack.set(DataComponentTypes.LORE, new LoreComponent(List.copyOf(lore)));
        }
    }

    private static NbtCompound customData(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        return customData == null ? new NbtCompound() : customData.copyNbt();
    }

    private static void saveCustomData(ItemStack stack, NbtCompound customData) {
        if (customData.isEmpty()) {
            stack.remove(DataComponentTypes.CUSTOM_DATA);
        } else {
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
        }
    }

    private static NbtCompound marker(NbtCompound customData) {
        if (!customData.contains(MARKER_KEY, NbtElement.COMPOUND_TYPE)) {
            customData.put(MARKER_KEY, new NbtCompound());
        }
        return customData.getCompound(MARKER_KEY);
    }

    private static void cleanMarker(NbtCompound customData) {
        if (customData.contains(MARKER_KEY, NbtElement.COMPOUND_TYPE)
                && customData.getCompound(MARKER_KEY).isEmpty()) {
            customData.remove(MARKER_KEY);
        }
    }
}
