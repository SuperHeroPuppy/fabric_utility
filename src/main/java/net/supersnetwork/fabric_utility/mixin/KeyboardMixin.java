package net.supersnetwork.fabric_utility.mixin;

import net.minecraft.client.Keyboard;
import net.supersnetwork.fabric_utility.client.StasisClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void fabricUtility$blockStasisKeyboard(long window, int key, int scanCode, int action, int modifiers,
                                                    CallbackInfo ci) {
        if (StasisClientState.isLocked()) {
            ci.cancel();
        }
    }

    @Inject(method = "onChar", at = @At("HEAD"), cancellable = true)
    private void fabricUtility$blockStasisCharacters(long window, int codePoint, int modifiers, CallbackInfo ci) {
        if (StasisClientState.isLocked()) {
            ci.cancel();
        }
    }
}
