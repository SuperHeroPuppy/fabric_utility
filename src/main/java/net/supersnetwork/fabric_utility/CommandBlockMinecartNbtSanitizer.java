package net.supersnetwork.fabric_utility;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class CommandBlockMinecartNbtSanitizer {
    private static final Identifier SUPPLEMENTARIES_CAGE = new Identifier("supplementaries", "cage");
    private static final String COMMAND_BLOCK_MINECART = "minecraft:command_block_minecart";
    private static final String PIG = "minecraft:pig";

    private CommandBlockMinecartNbtSanitizer() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (!player.hasPermissionLevel(2)) {
                    sanitizeInventory(player);
                }
            }
        });
    }

    private static void sanitizeInventory(ServerPlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            sanitizeStack(player.getInventory().getStack(slot));
        }

        sanitizeStack(player.currentScreenHandler.getCursorStack());
    }

    private static void sanitizeStack(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasNbt() || !SUPPLEMENTARIES_CAGE.equals(Registries.ITEM.getId(stack.getItem()))) {
            return;
        }

        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("BlockEntityTag", NbtElement.COMPOUND_TYPE)) {
            sanitizeCompound(nbt.getCompound("BlockEntityTag"));
        }
    }

    private static void sanitizeCompound(NbtCompound compound) {
        if (COMMAND_BLOCK_MINECART.equals(compound.getString("id"))) {
            replaceWithPig(compound);
            return;
        }

        for (String key : compound.getKeys()) {
            NbtElement value = compound.get(key);
            if (value instanceof NbtCompound child) {
                sanitizeCompound(child);
            } else if (value instanceof NbtList list) {
                sanitizeList(list);
            }
        }
    }

    private static void sanitizeList(NbtList list) {
        for (int i = 0; i < list.size(); i++) {
            NbtElement value = list.get(i);
            if (value instanceof NbtCompound compound) {
                sanitizeCompound(compound);
            } else if (value instanceof NbtList childList) {
                sanitizeList(childList);
            }
        }
    }

    private static void replaceWithPig(NbtCompound entityData) {
        entityData.putString("id", PIG);
        entityData.remove("Command");
        entityData.remove("LastOutput");
        entityData.remove("SuccessCount");
        entityData.remove("TrackOutput");
        entityData.remove("Passengers");
    }
}
