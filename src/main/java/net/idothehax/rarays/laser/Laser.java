package net.idothehax.rarays.laser;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import net.minecraft.block.Blocks;
import org.joml.Vector3f;

public class Laser {
    private final World world;
    private final PlayerEntity player;

    public Laser(World world, PlayerEntity player) {
        this.world = world;
        this.player = player;
    }

    public void spawnLaser() {
        // Set the starting height for the laser
        double startY = 200.0; // Fixed height
        Vec3d playerPos = player.getPos();
        Vec3d startPos = new Vec3d(playerPos.x, startY, playerPos.z); // Start position at the fixed height

        // Perform a raycast straight downward from the starting position
        Vec3d endPos = new Vec3d(startPos.x, 0.0, startPos.z); // Aim straight down to the ground level
        BlockHitResult hitResult = world.raycast(new RaycastContext(startPos, endPos, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));

        // Determine the target position based on the raycast result
        Vec3d targetPosition;
        double dipDepth = 1.5; // Extend the laser into the ground by this depth
        if (hitResult.getType() == BlockHitResult.Type.BLOCK) {
            targetPosition = hitResult.getPos().subtract(0, dipDepth, 0); // Offset the target downwards
        } else {
            targetPosition = new Vec3d(startPos.x, 0.0, startPos.z);
        }

        // Calculate total vertical distance from start to target
        double totalDistance = startY - targetPosition.y;
        int numberOfBlocks = Math.max((int) (totalDistance), 50); // Dynamic block count with minimum of 5 blocks
        double blockSpacing = totalDistance / (numberOfBlocks - 1);
        double glassSpacingOffset = blockSpacing * 0.5;

        // Create an ElementHolder for the laser
        ElementHolder holder = new ElementHolder();

        // Create the laser blocks
        for (int i = 0; i < numberOfBlocks; i++) {
            BlockDisplayElement laserElement = new BlockDisplayElement();
            laserElement.setBlockState(Blocks.LIGHT_BLUE_WOOL.getDefaultState()); // Replace with your desired laser block state

            // Calculate the position for the current laser block
            double laserY = startY - i * blockSpacing; // Position the laser block
            Vec3d laserPos = new Vec3d(startPos.x+0.25, Math.max(laserY+0.25, targetPosition.y), startPos.z+0.25); // Ensure it does not exceed target Y
            laserElement.setOverridePos(laserPos); // Set position of laser block

            holder.addElement(laserElement);
        }

        // Create the wrapping glass blocks
        for (int i = 0; i < numberOfBlocks; i++) {
            BlockDisplayElement glassElement = new BlockDisplayElement();
            glassElement.setBlockState(Blocks.WHITE_STAINED_GLASS.getDefaultState()); // Use glass block

            double glassY = startY - i * (blockSpacing + glassSpacingOffset);
            Vec3d glassPos = new Vec3d(startPos.x, Math.max(glassY, targetPosition.y) + 0.5, startPos.z);

            glassElement.setOverridePos(glassPos); // Set position of glass block

            // Scale the glass to be slightly larger than the laser
            glassElement.setScale(new Vector3f(1.5f, 1.5f, 1.5f)); // Slightly larger scale in X and Z

            holder.addElement(glassElement);
        }

        // Attach the holder to the world at the starting position
        BlockPos holderPos = new BlockPos((int) startPos.x, (int) startPos.y, (int) startPos.z);
        ChunkAttachment.ofTicking(holder, (ServerWorld) world, holderPos);
    }
}
