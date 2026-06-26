package net.supersnetwork.fabric_utility.mixin;

import net.minecraft.client.Mouse;
import net.supersnetwork.fabric_utility.client.StasisClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void fabricUtility$blockStasisButtons(long window, int button, int action, int modifiers, CallbackInfo ci) {
        if (StasisClientState.isLocked()) {
            ci.cancel();
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void fabricUtility$blockStasisScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (StasisClientState.isLocked()) {
            ci.cancel();
        }
    }

    @Inject(method = "onCursorPos", at = @At("HEAD"), cancellable = true)
    private void fabricUtility$blockStasisMouseMovement(long window, double x, double y, CallbackInfo ci) {
        if (StasisClientState.isLocked()) {
            ci.cancel();
        }
    }
}
