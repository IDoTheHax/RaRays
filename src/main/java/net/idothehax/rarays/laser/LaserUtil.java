package net.idothehax.rarays.laser;

import net.idothehax.rarays.mixin.BlockDisplayEntityAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.Blocks;

public class LaserUtil {

    public static void createLaser(ServerWorld world, Vec3d position) {
        // Create the beam entity using a quartz block
        DisplayEntity.BlockDisplayEntity beamEntity = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, world);
        setBlockState(beamEntity, Blocks.QUARTZ_BLOCK.getDefaultState());
        beamEntity.setPos(position.x, position.y, position.z);
        world.spawnEntity(beamEntity);

        // Create the bloom entity using light stained glass
        DisplayEntity.BlockDisplayEntity bloomEntity = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, world);
        setBlockState(bloomEntity, Blocks.WHITE_STAINED_GLASS.getDefaultState());
        bloomEntity.setPos(position.x, position.y, position.z);
        world.spawnEntity(bloomEntity);

        // Start oscillating the bloom width
        oscillateBloomWidth(bloomEntity);
    }

    private static void setBlockState(DisplayEntity.BlockDisplayEntity entity, BlockState state) {
        // Use the mixin to access the private method for setting the block state
        ((BlockDisplayEntityAccessor) entity).setBlockState(state);
    }

    private static void oscillateBloomWidth(DisplayEntity.BlockDisplayEntity bloomEntity) {
        // Logic to periodically change the width, you may need a timer or scheduler
    }
}


