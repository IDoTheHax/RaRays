package net.idothehax.rarays.laser;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
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
    private final Random random;
    private static final double MAX_DISTANCE = 100.0; // Maximum distance for the raycast
    private static final double MAX_HEIGHT = 200.0; // Maximum height for laser origin
    private static final double MIN_HEIGHT = 100.0; // Minimum height for laser origin

    public Laser(World world, PlayerEntity player) {
        this.world = world;
        this.player = player;
        this.random = Random.create();
    }

    public void spawnLaser() {
        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d eyePos = player.getEyePos();
        Vec3d targetPos = eyePos.add(lookVec.multiply(MAX_DISTANCE));

        BlockHitResult hitResult = world.raycast(new RaycastContext(
                eyePos,
                targetPos,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        if (hitResult.getType() == HitResult.Type.MISS) {
            return;
        }

        Vec3d hitPos = hitResult.getPos();

        double randomHeight = MIN_HEIGHT + random.nextDouble() * (MAX_HEIGHT - MIN_HEIGHT);
        double randomAngle = random.nextDouble() * Math.PI * 2;
        double radius = random.nextDouble() * 20.0;

        Vec3d startPos = new Vec3d(
                hitPos.x + Math.cos(randomAngle) * radius,
                randomHeight,
                hitPos.z + Math.sin(randomAngle) * radius
        );

        double dipDepth = 1.5;
        Vec3d targetPosition = hitPos.add(0, -dipDepth, 0);

        double totalDistance = startPos.distanceTo(targetPosition);
        int numberOfBlocks = Math.max((int) (totalDistance * 4), 100);
        int numberOfGlassBlocks = numberOfBlocks / 2;

        ElementHolder holder = new ElementHolder();

        // Create glass blocks first
        for (int i = 0; i < numberOfGlassBlocks; i++) {
            BlockDisplayElement glassElement = new BlockDisplayElement();
            glassElement.setBlockState(Blocks.WHITE_STAINED_GLASS.getDefaultState());

            double progress = (double) i / (numberOfGlassBlocks - 1);
            Vec3d pos = startPos.lerp(targetPosition, progress);

            // Minimal offset for glass
            double offsetAmount = 0.02;
            Vec3d offset = new Vec3d(
                    random.nextDouble() * offsetAmount - offsetAmount/2,
                    random.nextDouble() * offsetAmount - offsetAmount/2,
                    random.nextDouble() * offsetAmount - offsetAmount/2
            );

            glassElement.setScale(new Vector3f(1.0f, 1.0f, 1.0f));
            // Center the glass blocks
            glassElement.setOverridePos(pos.add(offset).add(0.5, 0.5, 0.5));

            holder.addElement(glassElement);
        }

        // Create wool blocks
        for (int i = 0; i < numberOfBlocks; i++) {
            BlockDisplayElement laserElement = new BlockDisplayElement();
            laserElement.setBlockState(Blocks.LIGHT_BLUE_WOOL.getDefaultState());

            double progress = (double) i / (numberOfBlocks - 1);
            Vec3d pos = startPos.lerp(targetPosition, progress);

            // Very small offset for tighter beam
            double offsetAmount = 0.03;
            Vec3d offset = new Vec3d(
                    random.nextDouble() * offsetAmount - offsetAmount/2,
                    random.nextDouble() * offsetAmount - offsetAmount/2,
                    random.nextDouble() * offsetAmount - offsetAmount/2
            );

            laserElement.setScale(new Vector3f(0.5f, 0.5f, 0.5f));
            // Center the wool blocks in the glass
            laserElement.setOverridePos(pos.add(offset).add(0.85, 1, 0.85));

            holder.addElement(laserElement);
        }

        BlockPos holderPos = new BlockPos((int) startPos.x, (int) startPos.y, (int) startPos.z);
        ChunkAttachment.ofTicking(holder, (ServerWorld) world, holderPos);
    }
}