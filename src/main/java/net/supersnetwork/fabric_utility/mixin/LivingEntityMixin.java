package net.supersnetwork.fabric_utility.mixin;

import net.minecraft.entity.LivingEntity;
import net.supersnetwork.fabric_utility.InvulnerabilityEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "takeKnockback", at = @At("HEAD"), cancellable = true)
    private void fabricUtility$cancelInvulnerableKnockback(double strength, double x, double z, CallbackInfo ci) {
        if (InvulnerabilityEvents.isInvulnerable((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
