package net.supersnetwork.fabric_utility.compat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.supersnetwork.fabric_utility.FabricUtility;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.Identifiers;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class FabricUtilityJadePlugin implements IWailaPlugin {
    static final Identifier PLAYER_NICKNAME = new Identifier(FabricUtility.MOD_ID, "player_nickname");

    @Override
    public void register(IWailaCommonRegistration registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addTooltipCollectedCallback((tooltip, accessor) -> {
            if (!(accessor instanceof EntityAccessor entityAccessor)
                    || !(entityAccessor.getEntity() instanceof PlayerEntity player)
                    || !player.hasCustomName()
                    || player.getCustomName() == null) {
                return;
            }

            String username = player.getGameProfile().getName();
            Text nickname = player.getCustomName();
            if (nickname.getString().equals(username)) {
                return;
            }

            tooltip.remove(Identifiers.CORE_OBJECT_NAME);
            tooltip.add(0, nickname, Identifiers.CORE_OBJECT_NAME);
            tooltip.add(1, Text.translatable("tooltip.fabric_utility.username", username), PLAYER_NICKNAME);
        });
    }
}
