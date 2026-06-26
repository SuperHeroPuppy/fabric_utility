package net.supersnetwork.fabric_utility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
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
    public static final int CONFIG_VERSION = 2;
    public static final List<String> CONFIG_KEYS = List.of(
            "blockedPettableEntities",
            "pettingSoundSuffixes",
            "maxPlayerPetParticles",
            "defaultPlayerPetSound",
            "defaultPlayerPetVolume",
            "defaultPlayerPetPitch",
            "nicknameSystemEnabled",
            "nicknameCharacterLimit",
            "proxyChatEnabled",
            "proxyChatRangeChunks",
            "customPetSounds"
    );

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("fabric_utility.json");
    private static final Path LEGACY_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("fabric_utility.properties");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, String> VALUES = new LinkedHashMap<>();
    private static final Set<Identifier> BLOCKED_PETTABLE_ENTITIES = new HashSet<>();
    private static final Map<String, PetSound> CUSTOM_PET_SOUNDS = new LinkedHashMap<>();
    private static List<String> pettingSoundSuffixes = List.of("ambient", "step", "hurt", "death");
    private static int maxPlayerPetParticles = 5;
    private static PetSound defaultPlayerPetSound = new PetSound(Identifier.of("minecraft", "item.brush.brushing.generic"), 0.1F, 1.8F);
    private static boolean nicknameSystemEnabled = true;
    private static int nicknameCharacterLimit = 35;
    private static boolean proxyChatEnabled = true;
    private static int proxyChatRangeChunks = 3;

    private FabricUtilityConfig() {
    }

    public static synchronized void load() {
        VALUES.clear();
        VALUES.putAll(defaultValues());

        if (Files.exists(CONFIG_PATH)) {
            loadJson();
        } else if (Files.exists(LEGACY_CONFIG_PATH)) {
            migrateLegacyProperties();
            save();
        } else {
            save();
        }

        parseLoadedValues();
    }

    public static synchronized void save() {
        JsonObject root = new JsonObject();
        root.addProperty("version", CONFIG_VERSION);
        JsonObject values = new JsonObject();
        CONFIG_KEYS.forEach(key -> values.addProperty(key, VALUES.getOrDefault(key, "")));
        root.add("values", values);

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException exception) {
            FabricUtility.LOGGER.warn("Failed to write config", exception);
        }
    }

    public static synchronized boolean setValue(String key, String value) {
        if (!CONFIG_KEYS.contains(key)) {
            return false;
        }

        VALUES.put(key, normalize(key, value));
        parseLoadedValues();
        save();
        return true;
    }

    public static synchronized boolean setValues(Map<String, String> values) {
        if (!values.keySet().stream().allMatch(CONFIG_KEYS::contains)) {
            return false;
        }

        values.forEach((key, value) -> VALUES.put(key, normalize(key, value)));
        parseLoadedValues();
        save();
        return true;
    }

    public static synchronized String getValue(String key) {
        return VALUES.getOrDefault(key, "");
    }

    public static synchronized Map<String, String> values() {
        Map<String, String> result = new LinkedHashMap<>();
        CONFIG_KEYS.forEach(key -> result.put(key, getValue(key)));
        return result;
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

    public static int nicknameCharacterLimit() {
        return nicknameCharacterLimit;
    }

    public static boolean proxyChatEnabled() {
        return proxyChatEnabled;
    }

    public static int proxyChatRangeChunks() {
        return proxyChatRangeChunks;
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

    private static Map<String, String> defaultValues() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("blockedPettableEntities", "minecraft:armor_stand,minecraft:item_frame,minecraft:painting,scrimblos:scrimblo");
        defaults.put("pettingSoundSuffixes", "ambient,step,hurt,death");
        defaults.put("maxPlayerPetParticles", "5");
        defaults.put("defaultPlayerPetSound", "minecraft:item.brush.brushing.generic");
        defaults.put("defaultPlayerPetVolume", "0.1");
        defaults.put("defaultPlayerPetPitch", "1.8");
        defaults.put("nicknameSystemEnabled", "true");
        defaults.put("nicknameCharacterLimit", "35");
        defaults.put("proxyChatEnabled", "true");
        defaults.put("proxyChatRangeChunks", "3");
        defaults.put("customPetSounds", "petting_purr=minecraft:entity.cat.purr:0.7:1.0");
        return defaults;
    }

    private static void loadJson() {
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject values = root.has("values") && root.get("values").isJsonObject()
                    ? root.getAsJsonObject("values")
                    : root;
            for (String key : CONFIG_KEYS) {
                JsonElement element = values.get(key);
                if (element != null && element.isJsonPrimitive()) {
                    VALUES.put(key, normalize(key, element.getAsString()));
                }
            }
        } catch (Exception exception) {
            FabricUtility.LOGGER.warn("Failed to read config, using defaults", exception);
        }
    }

    private static void migrateLegacyProperties() {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(LEGACY_CONFIG_PATH)) {
            properties.load(reader);
            for (String key : CONFIG_KEYS) {
                if (properties.containsKey(key)) {
                    VALUES.put(key, normalize(key, properties.getProperty(key)));
                }
            }
            FabricUtility.LOGGER.info("Migrated legacy Fabric Utility properties config to {}", CONFIG_PATH);
        } catch (IOException exception) {
            FabricUtility.LOGGER.warn("Failed to migrate legacy config, using defaults", exception);
        }
    }

    private static String normalize(String key, String value) {
        String input = value == null ? "" : value.trim();
        return switch (key) {
            case "nicknameSystemEnabled", "proxyChatEnabled" -> Boolean.toString(Boolean.parseBoolean(input));
            case "maxPlayerPetParticles" -> Integer.toString(Math.max(0, parseInt(input, 5)));
            case "nicknameCharacterLimit" -> Integer.toString(Math.max(1, parseInt(input, 35)));
            case "proxyChatRangeChunks" -> Integer.toString(Math.max(1, Math.min(16, parseInt(input, 3))));
            case "defaultPlayerPetVolume" -> Float.toString(Math.max(0.0F, parseFloat(input, 0.1F)));
            case "defaultPlayerPetPitch" -> Float.toString(Math.max(0.0F, parseFloat(input, 1.8F)));
            case "defaultPlayerPetSound" -> Identifier.tryParse(input) == null ? "minecraft:item.brush.brushing.generic" : input;
            default -> input;
        };
    }

    private static void parseLoadedValues() {
        BLOCKED_PETTABLE_ENTITIES.clear();
        Arrays.stream(getValue("blockedPettableEntities").split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Identifier::tryParse)
                .filter(java.util.Objects::nonNull)
                .forEach(BLOCKED_PETTABLE_ENTITIES::add);

        pettingSoundSuffixes = Arrays.stream(getValue("pettingSoundSuffixes").split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
        maxPlayerPetParticles = parseInt(getValue("maxPlayerPetParticles"), 5);
        defaultPlayerPetSound = new PetSound(
                parseIdentifier(getValue("defaultPlayerPetSound"), Identifier.of("minecraft", "item.brush.brushing.generic")),
                parseFloat(getValue("defaultPlayerPetVolume"), 0.1F),
                parseFloat(getValue("defaultPlayerPetPitch"), 1.8F)
        );
        nicknameSystemEnabled = Boolean.parseBoolean(getValue("nicknameSystemEnabled"));
        nicknameCharacterLimit = Math.max(1, parseInt(getValue("nicknameCharacterLimit"), 35));
        proxyChatEnabled = Boolean.parseBoolean(getValue("proxyChatEnabled"));
        proxyChatRangeChunks = Math.max(1, Math.min(16, parseInt(getValue("proxyChatRangeChunks"), 3)));

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
