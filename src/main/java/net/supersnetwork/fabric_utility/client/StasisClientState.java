package net.supersnetwork.fabric_utility.client;

import net.minecraft.client.MinecraftClient;

public final class StasisClientState {
    private static volatile boolean locked;

    private StasisClientState() {
    }

    public static boolean isLocked() {
        return locked;
    }

    public static void setLocked(MinecraftClient client, boolean value) {
        locked = value;
        if (value) {
            client.setScreen(null);
            client.options.forwardKey.setPressed(false);
            client.options.backKey.setPressed(false);
            client.options.leftKey.setPressed(false);
            client.options.rightKey.setPressed(false);
            client.options.jumpKey.setPressed(false);
            client.options.sneakKey.setPressed(false);
            client.options.sprintKey.setPressed(false);
            client.options.attackKey.setPressed(false);
            client.options.useKey.setPressed(false);
            client.options.inventoryKey.setPressed(false);
            client.options.dropKey.setPressed(false);
            client.options.swapHandsKey.setPressed(false);
        }
    }

    public static void clear() {
        locked = false;
    }
}
