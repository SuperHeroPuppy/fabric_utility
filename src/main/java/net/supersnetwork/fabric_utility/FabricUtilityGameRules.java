package net.supersnetwork.fabric_utility;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.world.GameRules;

public final class FabricUtilityGameRules {
    public static GameRules.Key<GameRules.BooleanRule> ALLOW_PETTING;
    public static GameRules.Key<GameRules.IntRule> WORLD_HEIGHT_LIMIT;
    public static GameRules.Key<GameRules.BooleanRule> ADMIN_LOG_COMMANDS;

    private FabricUtilityGameRules() {
    }

    public static void register() {
        ALLOW_PETTING = GameRuleRegistry.register("fabricUtilityAllowPetting", GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
        WORLD_HEIGHT_LIMIT = GameRuleRegistry.register("fabricUtilityWorldHeightLimit", GameRules.Category.UPDATES, GameRuleFactory.createIntRule(0, 0));
        ADMIN_LOG_COMMANDS = GameRuleRegistry.register("fabricUtilityAdminLogCommands", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));
    }
}
