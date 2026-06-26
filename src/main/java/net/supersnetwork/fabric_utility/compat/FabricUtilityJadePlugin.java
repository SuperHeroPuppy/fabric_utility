package net.supersnetwork.fabric_utility.compat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.supersnetwork.fabric_utility.FabricUtility;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class FabricUtilityJadePlugin implements IWailaPlugin {
    private static final Identifier PLAYER_NICKNAME = Identifier.of(FabricUtility.MOD_ID, "player_nickname");

    @Override
    public void register(IWailaCommonRegistration registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(PlayerNicknameProvider.INSTANCE, PlayerEntity.class);
    }

    private enum PlayerNicknameProvider implements IEntityComponentProvider {
        INSTANCE;

        @Override
        public Identifier getUid() {
            return PLAYER_NICKNAME;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (!(accessor.getEntity() instanceof PlayerEntity player)
                    || !player.hasCustomName()
                    || player.getCustomName() == null) {
                return;
            }

            String username = player.getGameProfile().getName();
            Text nickname = player.getCustomName();
            if (nickname.getString().equals(username)) {
                return;
            }

            tooltip.remove(JadeIds.CORE_OBJECT_NAME);
            tooltip.add(0, nickname, JadeIds.CORE_OBJECT_NAME);
            tooltip.add(1, Text.translatable("tooltip.fabric_utility.username", username), PLAYER_NICKNAME);
        }
    }
}
