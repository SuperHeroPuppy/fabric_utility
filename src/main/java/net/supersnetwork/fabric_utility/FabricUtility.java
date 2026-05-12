package net.supersnetwork.fabric_utility;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabricUtility implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("fabric_utility");

    @Override
    public void onInitialize() {
        FabricUtilityConfig.load();
        FabricUtilityGameRules.register();
        FabricUtilityCommand.register();
        ChunkTagCommand.register();
        InvulnerableChunkProtection.register();
        InvulnerabilityEvents.register();
        PettingHandler.register();
        NickCommandManager.register();
        BanHammerHandler.register();

        LOGGER.info("Fabric Utility initialized");
    }
}
