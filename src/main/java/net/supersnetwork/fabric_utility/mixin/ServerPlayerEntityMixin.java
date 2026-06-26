package net.supersnetwork.fabric_utility.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.supersnetwork.fabric_utility.AttachmentBadgeApi;
import net.supersnetwork.fabric_utility.FabricUtilityConfig;
import net.supersnetwork.fabric_utility.NickCommandManager;
import net.supersnetwork.fabric_utility.api.NicknameApi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @Inject(method = "getPlayerListName", at = @At("HEAD"), cancellable = true)
    private void fabricUtility$useNicknameInPlayerList(CallbackInfoReturnable<Text> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        boolean hasNickname = FabricUtilityConfig.nicknameSystemEnabled()
                && NickCommandManager.getNickname(player).isPresent();
        boolean hasAttachment = AttachmentBadgeApi.hasAttachment(player);
        if (!hasNickname && !hasAttachment) {
            return;
        }

        Text resolvedName = hasNickname
                ? NicknameApi.getDisplayName(player)
                : Text.literal(player.getGameProfile().getName());
        cir.setReturnValue(AttachmentBadgeApi.attach(player, resolvedName));
    }
}
