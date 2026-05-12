package net.supersnetwork.fabric_utility;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PettingHandler {
    private static final String PURR_ON_PET_TAG = "petting_purr";
    private static final String[] ENTITY_SOUND_SUFFIXES = {"ambient", "step", "hurt", "death"};
    private static final int MAX_PLAYER_PET_PARTICLES = 5;
    private static final Map<UUID, PetInteractionStamp> RECENT_PETS = new ConcurrentHashMap<>();

    private PettingHandler() {
    }

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> tryPet(player, world, hand, entity));
    }

    private static ActionResult tryPet(PlayerEntity player, World world, Hand hand, Entity target) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }

        if (!world.getGameRules().getBoolean(FabricUtilityGameRules.ALLOW_PETTING)) {
            return ActionResult.PASS;
        }

        if (!player.isSneaking() || !player.getStackInHand(hand).isEmpty()) {
            return ActionResult.PASS;
        }

        if (!(target instanceof LivingEntity living)) {
            return ActionResult.PASS;
        }

        Identifier entityId = EntityType.getId(target.getType());

        if (target instanceof ArmorStandEntity || target instanceof ItemFrameEntity || target.getType() == EntityType.PAINTING) {
            return ActionResult.PASS;
        }

        if (FabricUtilityConfig.isPettingBlocked(entityId)) {
            return ActionResult.PASS;
        }

        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        ServerWorld serverWorld = (ServerWorld) world;
        if (isDuplicateInteraction(player, target, serverWorld.getTime())) {
            return ActionResult.SUCCESS;
        }

        player.swingHand(hand, true);
        playPetSound(serverWorld, living);
        spawnPetParticles(serverWorld, living);
        return ActionResult.SUCCESS;
    }

    private static boolean isDuplicateInteraction(PlayerEntity player, Entity target, long gameTime) {
        PetInteractionStamp previous = RECENT_PETS.put(player.getUuid(), new PetInteractionStamp(gameTime, target.getId()));
        return previous != null && previous.gameTime == gameTime && previous.targetId == target.getId();
    }

    private static void playPetSound(ServerWorld world, LivingEntity living) {
        if (living.getCommandTags().contains(PURR_ON_PET_TAG)) {
            world.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.ENTITY_CAT_PURR, living.getSoundCategory(), 0.7F, living.getSoundPitch());
            return;
        }

        SoundEvent petSound = findPetSound(living);
        if (petSound != null) {
            world.playSound(null, living.getX(), living.getY(), living.getZ(), petSound, living.getSoundCategory(), 0.8F, living.getSoundPitch());
            return;
        }

        if (living instanceof MobEntity mob) {
            mob.playAmbientSound();
            return;
        }

        world.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.ITEM_BRUSH_BRUSHING_GENERIC, SoundCategory.NEUTRAL, 0.1F, 1.8F);
    }

    private static SoundEvent findPetSound(LivingEntity living) {
        if (living instanceof PlayerEntity) {
            return null;
        }

        Identifier entityId = EntityType.getId(living.getType());
        for (String suffix : ENTITY_SOUND_SUFFIXES) {
            Identifier soundId = new Identifier(entityId.getNamespace(), "entity." + entityId.getPath() + "." + suffix);

            if (Registries.SOUND_EVENT.containsId(soundId)) {
                return Registries.SOUND_EVENT.get(soundId);
            }
        }

        return null;
    }

    private static void spawnPetParticles(ServerWorld world, LivingEntity target) {
        double width = target.getWidth();
        double height = target.getHeight();
        int particleCount = Math.max(2, (int) (width * width * height * 20));
        particleCount = Math.min(particleCount, target instanceof PlayerEntity ? MAX_PLAYER_PET_PARTICLES : 15);

        Box box = target.getBoundingBox();
        ServerPlayerEntity excludedViewer = target instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null;

        for (int i = 0; i < particleCount; i++) {
            double x = box.minX + world.random.nextDouble() * width;
            double y = box.minY + world.random.nextDouble() * height;
            double z = box.minZ + world.random.nextDouble() * width;

            for (ServerPlayerEntity viewer : world.getPlayers()) {
                if (viewer != excludedViewer) {
                    world.spawnParticles(viewer, ParticleTypes.HEART, false, x, y, z, 1, 0.0D, 0.02D, 0.0D, 0.0D);
                }
            }
        }
    }

    private record PetInteractionStamp(long gameTime, int targetId) {
    }
}
