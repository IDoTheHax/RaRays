package net.idothehax.rarays.mixin;

import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DisplayEntity.BlockDisplayEntity.class)
public interface BlockDisplayEntityAccessor {
    @Invoker("setBlockState")
    void setBlockState(BlockState state);
}

