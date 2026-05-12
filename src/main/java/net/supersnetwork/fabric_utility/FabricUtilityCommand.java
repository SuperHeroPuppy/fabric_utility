package net.supersnetwork.fabric_utility;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

public final class FabricUtilityCommand {
    private FabricUtilityCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("fabricutility")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("config")
                                .then(CommandManager.literal("reload")
                                        .executes(context -> {
                                            FabricUtilityConfig.load();
                                            context.getSource().sendFeedback(() -> Text.literal("Fabric Utility config reloaded."), true);
                                            return 1;
                                        }))
                                .then(CommandManager.literal("sync")
                                        .executes(context -> {
                                            FabricUtilityConfig.load();
                                            context.getSource().sendFeedback(() -> Text.literal("Fabric Utility config synced from disk to the server."), true);
                                            return 1;
                                        }))
                                .then(CommandManager.literal("list")
                                        .executes(context -> {
                                            context.getSource().sendFeedback(() -> Text.literal("Config: " + FabricUtilityConfig.values()), false);
                                            return 1;
                                        }))
                                .then(CommandManager.literal("get")
                                        .then(CommandManager.argument("key", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    FabricUtilityConfig.CONFIG_KEYS.forEach(builder::suggest);
                                                    return builder.buildFuture();
                                                })
                                                .executes(context -> {
                                                    String key = StringArgumentType.getString(context, "key");
                                                    context.getSource().sendFeedback(() -> Text.literal(key + "=" + FabricUtilityConfig.getValue(key)), false);
                                                    return 1;
                                                })))
                                .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("key", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    FabricUtilityConfig.CONFIG_KEYS.forEach(builder::suggest);
                                                    return builder.buildFuture();
                                                })
                                                .then(CommandManager.argument("value", StringArgumentType.greedyString())
                                                        .executes(context -> {
                                                            String key = StringArgumentType.getString(context, "key");
                                                            String value = StringArgumentType.getString(context, "value");

                                                            if (!FabricUtilityConfig.setValue(key, value)) {
                                                                context.getSource().sendError(Text.literal("Unknown config key: " + key));
                                                                return 0;
                                                            }

                                                            context.getSource().sendFeedback(() -> Text.literal("Set " + key + "=" + value), true);
                                                            return 1;
                                                        })))))));
    }
}
