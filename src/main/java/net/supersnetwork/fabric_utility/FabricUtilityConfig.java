package net.supersnetwork.fabric_utility;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

public final class FabricUtilityConfig {
    public static final List<String> CONFIG_KEYS = List.of(
            "blockedPettableEntities",
            "pettingSoundSuffixes",
            "maxPlayerPetParticles",
            "defaultPlayerPetSound",
            "defaultPlayerPetVolume",
            "defaultPlayerPetPitch",
            "nicknameSystemEnabled",
            "customPetSounds"
    );

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("fabric_utility.properties");
    private static final Properties PROPERTIES = new Properties();
    private static final Set<Identifier> BLOCKED_PETTABLE_ENTITIES = new HashSet<>();
    private static final Map<String, PetSound> CUSTOM_PET_SOUNDS = new LinkedHashMap<>();
    private static List<String> pettingSoundSuffixes = List.of("ambient", "step", "hurt", "death");
    private static int maxPlayerPetParticles = 5;
    private static PetSound defaultPlayerPetSound = new PetSound(new Identifier("minecraft", "item.brush.brushing.generic"), 0.1F, 1.8F);
    private static boolean nicknameSystemEnabled = true;

    private FabricUtilityConfig() {
    }

    public static void load() {
        applyDefaults(PROPERTIES);

        if (Files.notExists(CONFIG_PATH)) {
            save();
        }

        try (InputStream input = Files.newInputStream(CONFIG_PATH)) {
            PROPERTIES.load(input);
        } catch (IOException exception) {
            FabricUtility.LOGGER.warn("Failed to read config, using defaults", exception);
        }

        parseLoadedProperties();
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream output = Files.newOutputStream(CONFIG_PATH)) {
                PROPERTIES.store(output, "Fabric Utility config");
            }
        } catch (IOException exception) {
            FabricUtility.LOGGER.warn("Failed to write config", exception);
        }
    }

    public static boolean setValue(String key, String value) {
        if (!CONFIG_KEYS.contains(key)) {
            return false;
        }

        PROPERTIES.setProperty(key, value);
        parseLoadedProperties();
        save();
        return true;
    }

    public static String getValue(String key) {
        return PROPERTIES.getProperty(key, "");
    }

    public static Map<String, String> values() {
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : CONFIG_KEYS) {
            values.put(key, getValue(key));
        }
        return values;
    }

    public static boolean isPettingBlocked(Identifier entityId) {
        return BLOCKED_PETTABLE_ENTITIES.contains(entityId);
    }

    public static List<String> pettingSoundSuffixes() {
        return pettingSoundSuffixes;
    }

    public static int maxPlayerPetParticles() {
        return maxPlayerPetParticles;
    }

    public static PetSound defaultPlayerPetSound() {
        return defaultPlayerPetSound;
    }

    public static boolean nicknameSystemEnabled() {
        return nicknameSystemEnabled;
    }

    public static Optional<PetSound> petSoundForTags(Set<String> tags) {
        for (Map.Entry<String, PetSound> entry : CUSTOM_PET_SOUNDS.entrySet()) {
            if (tags.contains(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }

        return Optional.empty();
    }

    public static Optional<SoundEvent> soundEvent(Identifier id) {
        return Registries.SOUND_EVENT.containsId(id) ? Optional.of(Registries.SOUND_EVENT.get(id)) : Optional.empty();
    }

    private static void applyDefaults(Properties properties) {
        properties.putIfAbsent("blockedPettableEntities", "minecraft:armor_stand,minecraft:item_frame,minecraft:painting,scrimblos:scrimblo");
        properties.putIfAbsent("pettingSoundSuffixes", "ambient,step,hurt,death");
        properties.putIfAbsent("maxPlayerPetParticles", "5");
        properties.putIfAbsent("defaultPlayerPetSound", "minecraft:item.brush.brushing.generic");
        properties.putIfAbsent("defaultPlayerPetVolume", "0.1");
        properties.putIfAbsent("defaultPlayerPetPitch", "1.8");
        properties.putIfAbsent("nicknameSystemEnabled", "true");
        properties.putIfAbsent("customPetSounds", "petting_purr=minecraft:entity.cat.purr:0.7:1.0");
    }

    private static void parseLoadedProperties() {
        BLOCKED_PETTABLE_ENTITIES.clear();
        Arrays.stream(getValue("blockedPettableEntities").split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Identifier::tryParse)
                .filter(identifier -> identifier != null)
                .forEach(BLOCKED_PETTABLE_ENTITIES::add);

        pettingSoundSuffixes = Arrays.stream(getValue("pettingSoundSuffixes").split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
        maxPlayerPetParticles = parseInt(getValue("maxPlayerPetParticles"), 5);
        defaultPlayerPetSound = new PetSound(
                parseIdentifier(getValue("defaultPlayerPetSound"), new Identifier("minecraft", "item.brush.brushing.generic")),
                parseFloat(getValue("defaultPlayerPetVolume"), 0.1F),
                parseFloat(getValue("defaultPlayerPetPitch"), 1.8F)
        );
        nicknameSystemEnabled = Boolean.parseBoolean(getValue("nicknameSystemEnabled"));

        CUSTOM_PET_SOUNDS.clear();
        Arrays.stream(getValue("customPetSounds").split(";"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .forEach(FabricUtilityConfig::parseCustomPetSound);
    }

    private static void parseCustomPetSound(String definition) {
        String[] tagAndSound = definition.split("=", 2);
        if (tagAndSound.length != 2 || tagAndSound[0].isBlank()) {
            return;
        }

        String[] soundParts = tagAndSound[1].split(":");
        if (soundParts.length < 2) {
            return;
        }

        Identifier soundId = Identifier.tryParse(soundParts[0] + ":" + soundParts[1]);
        if (soundId == null) {
            return;
        }

        float volume = soundParts.length >= 3 ? parseFloat(soundParts[2], 0.8F) : 0.8F;
        float pitch = soundParts.length >= 4 ? parseFloat(soundParts[3], 1.0F) : 1.0F;
        CUSTOM_PET_SOUNDS.put(tagAndSound[0].trim(), new PetSound(soundId, volume, pitch));
    }

    private static Identifier parseIdentifier(String value, Identifier fallback) {
        Identifier id = Identifier.tryParse(value);
        return id == null ? fallback : id;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public record PetSound(Identifier soundId, float volume, float pitch) {
    }
}
