package net.supersnetwork.fabric_utility.api;

import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface NicknameChangeListener {
    void onNicknameChanged(ServerPlayerEntity player, @Nullable String previousNickname, @Nullable String newNickname);
}
