package net.idothehax.rarays.laser;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.player.PlayerEntity;
import org.joml.Vector3f; // Import the Vector3f class

public class Laser {
    private final World world;
    private final PlayerEntity player;

    public Laser(World world, PlayerEntity player) {
        this.world = world;
        this.player = player;
    }

    public void spawnLaser() {
        // Get the player's position and look direction
        Vec3d playerPos = player.getPos();
        Vec3d playerDirection = player.getRotationVector().normalize(); // Ensure the direction is normalized

        // Define how far away you want the laser to spawn
        double distance = 10.0; // How far to spawn from the player
        Vec3d startPos = playerPos.add(playerDirection.multiply(distance));

        // Create an ElementHolder for the laser
        ElementHolder holder = new ElementHolder();
        int numberOfBlocks = 100; // Number of blocks to stack
        double blockSpacing = 1.0; // Space between blocks (adjust as needed)

        // Calculate the offset to center the blocks
        double offset = (numberOfBlocks - 1) * blockSpacing / 2.0;

        // Create the laser blocks
        for (int i = 0; i < numberOfBlocks; i++) {
            BlockDisplayElement laserElement = new BlockDisplayElement();
            laserElement.setBlockState(Blocks.LIGHT_BLUE_WOOL.getDefaultState()); // Replace with your desired laser block state

            // Calculate the position for the current laser block
            // Offset the y-position by half a block height (0.5) to center it
            Vec3d laserPos = startPos.add(playerDirection.multiply(i * blockSpacing - offset)).add(0, 0.5, 0);
            laserElement.setOverridePos(laserPos); // Set position of laser block

            holder.addElement(laserElement);
        }

        // Create the wrapping glass blocks
        for (int i = 0; i < numberOfBlocks; i++) {
            BlockDisplayElement glassElement = new BlockDisplayElement();
            glassElement.setBlockState(Blocks.WHITE_STAINED_GLASS.getDefaultState()); // Use glass block

            // Calculate the position for the current glass block
            // Offset the y-position by half a block height (0.5) to center it
            Vec3d glassPos = startPos.add(playerDirection.multiply(i * blockSpacing - offset)).add(-0.25, 0.25, -0.25);
            glassElement.setOverridePos(glassPos); // Set position of glass block

            // Scale the glass to be slightly larger than the laser
            glassElement.setScale(new Vector3f(1.5f, 1.5f, 1.5f)); // Slightly larger scale in X and Z

            holder.addElement(glassElement);
        }

        // Attach the holder to the world at the initial position
        BlockPos holderPos = new BlockPos((int) startPos.x, (int) startPos.y, (int) startPos.z);
        ChunkAttachment.ofTicking(holder, (ServerWorld) world, holderPos);
    }


}
