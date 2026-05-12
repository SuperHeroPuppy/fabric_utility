package net.supersnetwork.fabric_utility;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public final class FabricUtilityConfig {
    private static final Set<Identifier> BLOCKED_PETTABLE_ENTITIES = new HashSet<>();

    private FabricUtilityConfig() {
    }

    public static void load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("fabric_utility.properties");
        Properties properties = new Properties();

        if (Files.notExists(configPath)) {
            properties.setProperty("blockedPettableEntities", "minecraft:armor_stand,minecraft:item_frame,minecraft:painting,scrimblos:scrimblo");
            try {
                Files.createDirectories(configPath.getParent());
                try (OutputStream output = Files.newOutputStream(configPath)) {
                    properties.store(output, "Fabric Utility config");
                }
            } catch (IOException exception) {
                FabricUtility.LOGGER.warn("Failed to write default config", exception);
            }
        }

        try (InputStream input = Files.newInputStream(configPath)) {
            properties.load(input);
        } catch (IOException exception) {
            FabricUtility.LOGGER.warn("Failed to read config, using defaults", exception);
        }

        BLOCKED_PETTABLE_ENTITIES.clear();
        Arrays.stream(properties.getProperty("blockedPettableEntities", "").split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Identifier::tryParse)
                .filter(identifier -> identifier != null)
                .forEach(BLOCKED_PETTABLE_ENTITIES::add);
    }

    public static boolean isPettingBlocked(Identifier entityId) {
        return BLOCKED_PETTABLE_ENTITIES.contains(entityId);
    }
}
