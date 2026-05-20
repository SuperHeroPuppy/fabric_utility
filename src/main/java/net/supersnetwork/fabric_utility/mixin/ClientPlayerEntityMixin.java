package net.supersnetwork.fabric_utility.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class ClientPlayerEntityMixin {
    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void fabricUtility$usePlayerListNameForClientNameplate(CallbackInfoReturnable<Text> cir) {
        if (!((Object) this instanceof AbstractClientPlayerEntity player)) {
            return;
        }

        PlayerListEntry entry = ((AbstractClientPlayerEntityAccessor) player).fabricUtility$getPlayerListEntry();
        if (entry != null && entry.getDisplayName() != null) {
            cir.setReturnValue(entry.getDisplayName());
        }
    }
}
