package net.supersnetwork.fabric_utility.mixin;

import com.mojang.brigadier.tree.CommandNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = CommandNode.class, remap = false)
public interface CommandNodeAccessor<S> {
    @Accessor("children")
    Map<String, CommandNode<S>> fabricUtility$getChildren();

    @Accessor("literals")
    Map<String, CommandNode<S>> fabricUtility$getLiterals();

    @Accessor("arguments")
    Map<String, CommandNode<S>> fabricUtility$getArguments();
}
