package net.supersnetwork.fabric_utility.compat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.supersnetwork.fabric_utility.FabricUtility;
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
        registration.registerEntityComponent(NicknameEntityComponentProvider.INSTANCE, PlayerEntity.class);
    }
}
