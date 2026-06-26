package net.supersnetwork.fabric_utility.mixin;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.supersnetwork.fabric_utility.StasisHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow public ServerPlayerEntity player;

    @Inject(
            method = {
                    "onPlayerInput",
                    "onVehicleMove",
                    "onRecipeBookData",
                    "onRecipeCategoryOptions",
                    "onAdvancementTab",
                    "onRequestCommandCompletions",
                    "onUpdateCommandBlock",
                    "onUpdateCommandBlockMinecart",
                    "onPickFromInventory",
                    "onRenameItem",
                    "onUpdateBeacon",
                    "onUpdateStructureBlock",
                    "onUpdateJigsaw",
                    "onJigsawGenerating",
                    "onSelectMerchantTrade",
                    "onBookUpdate",
                    "onQueryEntityNbt",
                    "onQueryBlockNbt",
                    "onPlayerMove",
                    "onPlayerAction",
                    "onPlayerInteractBlock",
                    "onPlayerInteractItem",
                    "onSpectatorTeleport",
                    "onBoatPaddleState",
                    "onUpdateSelectedSlot",
                    "onChatMessage",
                    "onCommandExecution",
                    "onHandSwing",
                    "onClientCommand",
                    "onPlayerInteractEntity",
                    "onClickSlot",
                    "onCraftRequest",
                    "onButtonClick",
                    "onCreativeInventoryAction",
                    "onUpdateSign",
                    "onUpdatePlayerAbilities"
            },
            at = @At("HEAD"),
            cancellable = true
    )
    private void fabricUtility$blockStasisInput(CallbackInfo ci) {
        if (StasisHandler.isInStasis(player)) {
            ci.cancel();
        }
    }
}
