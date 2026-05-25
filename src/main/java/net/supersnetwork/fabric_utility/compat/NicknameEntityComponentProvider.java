package net.supersnetwork.fabric_utility.compat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum NicknameEntityComponentProvider implements IEntityComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        Entity entity = accessor.getEntity();
        if (!(entity instanceof PlayerEntity player) || !player.hasCustomName() || player.getCustomName() == null) {
            return;
        }

        String realName = player.getName().getString();
        String nickname = player.getCustomName().getString();
        if (!nickname.equals(realName)) {
            tooltip.add(Text.translatable("tooltip.fabric_utility.nickname", player.getCustomName()));
        }
    }

    @Override
    public Identifier getUid() {
        return FabricUtilityJadePlugin.PLAYER_NICKNAME;
    }
}
