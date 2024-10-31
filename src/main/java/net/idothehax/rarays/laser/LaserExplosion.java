package net.idothehax.rarays.laser;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.util.math.random.Random;

import java.util.*;

public class LaserExplosion {
    private final List<Particle> explosionParticles = new ArrayList<>();
    private final List<Particle> burnParticles = new ArrayList<>();
    private final Vec3d center;
    private final World world;
    private float expansionProgress = 0;
    private static final float EXPANSION_RATE = 0.8f; // Faster expansion rate
    private static final float MAX_RADIUS = 120.0f;
    private static final int PARTICLES_PER_RING = 96;
    private static final int NUMBER_OF_RINGS = 3;
    private final Random random;
    private final ElementHolder holder;
    private boolean isExpanding = true;

    public LaserExplosion(World world, Vec3d center, ElementHolder holder) {
        this.world = world;
        this.center = center.add(0, 3, 0); // Adjusting to spawn 3 blocks above
        this.holder = holder;
        this.random = Random.create();
        createExplosionParticles();
        createBurnParticles();
    }

    private void createExplosionParticles() {
        var explosionBlocks = new net.minecraft.block.Block[]{
                Blocks.ORANGE_STAINED_GLASS,
                Blocks.TERRACOTTA,
                Blocks.ORANGE_CONCRETE,
                Blocks.HONEYCOMB_BLOCK,
                Blocks.SHROOMLIGHT
        };



        // Create multiple rings of particles
        for (int ring = 0; ring < NUMBER_OF_RINGS; ring++) {
            float radius = MAX_RADIUS * (ring + 1) / NUMBER_OF_RINGS;
            for (int i = 0; i < PARTICLES_PER_RING; i++) {
                BlockDisplayElement element = new BlockDisplayElement();
                element.setBlockState(explosionBlocks[random.nextInt(explosionBlocks.length)].getDefaultState());

                // Randomly set scale for each particle within a larger range
                float scale = 5.5f + random.nextFloat();
                element.setScale(new Vector3f(scale, scale, scale));

                // Set random rotation for each particle
                Quaternionf rotation = new Quaternionf().rotateXYZ(
                        random.nextFloat() * (float) Math.PI * 3,
                        random.nextFloat() * (float) Math.PI * 3,
                        random.nextFloat() * (float) Math.PI * 3
                );

                // Apply rotation with setRightRotation or setLeftRotation randomly
                if (random.nextBoolean()) {
                    element.setRightRotation(rotation);
                } else {
                    element.setLeftRotation(rotation);
                }

                // Calculate initial position in a circle
                double angle = (2 * Math.PI / PARTICLES_PER_RING) * i;
                Vec3d initialOffset = new Vec3d(
                        Math.cos(angle),
                        0,
                        Math.sin(angle)
                ).multiply(radius); // Set initial offset based on radius

                element.setOverridePos(center.add(initialOffset)); // Apply initial position
                explosionParticles.add(new Particle(element, initialOffset));
                holder.addElement(element);
            }
        }
    }

    private void createBurnParticles() {
        var burnBlocks = new net.minecraft.block.Block[]{
                Blocks.ORANGE_STAINED_GLASS, Blocks.NETHERRACK, Blocks.TINTED_GLASS, Blocks.BLACK_STAINED_GLASS
        };

        int burnParticleCount = 9; // Control the number of burn particles
        float burnRadius = 15.0f; // Smaller radius for burn particles

        // Get the ground level at the explosion center using the appropriate heightmap
        int groundLevel = world.getTopY(Heightmap.Type.MOTION_BLOCKING,
                (int) center.getX(),
                (int) center.getZ());

        for (int i = 0; i < burnParticleCount; i++) {
            BlockDisplayElement element = new BlockDisplayElement();
            element.setBlockState(burnBlocks[random.nextInt(burnBlocks.length)].getDefaultState());

            // Set smaller scale for burn particles
            float scale = 0.5f + random.nextFloat() * 0.5f;
            element.setScale(new Vector3f(scale, scale, scale));

            // Random rotation
            Quaternionf rotation = new Quaternionf().rotateXYZ(
                    random.nextFloat() * (float) Math.PI * 3,
                    random.nextFloat() * (float) Math.PI * 3,
                    random.nextFloat() * (float) Math.PI * 3
            );
            element.setRightRotation(rotation);

            // Spread out by 5 blocks in all directions
            Vec3d initialOffset = new Vec3d(
                    random.nextDouble() * 15 - 7.5, // Random X offset between -5 and 5
                    0, // Keep vertical position as ground level plus offset
                    random.nextDouble() * 15 - 7.5  // Random Z offset between -5 and 5
            );

            // Set the initial position at ground level with a slight vertical offset
            element.setOverridePos(center.add(initialOffset.add(new Vec3d(0, groundLevel + 0.5, 0))));
            burnParticles.add(new Particle(element, initialOffset));
            holder.addElement(element);
        }
    }

    private void fadeBlocks() {
        List<Block> fadeSequence = Arrays.asList(
                Blocks.ORANGE_STAINED_GLASS,
                Blocks.BLACK_STAINED_GLASS,
                Blocks.GRAY_STAINED_GLASS,
                Blocks.WHITE_STAINED_GLASS
        );

        // Iterate over a square area defined by the explosion radius
        int radius = (int) MAX_RADIUS;
        BlockPos centerPos = new BlockPos((int) center.x, (int) center.y, (int) center.z);

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos targetPos = centerPos.add(x, y, z);
                    if (targetPos.isWithinDistance(centerPos, radius)) {
                        Block currentBlock = world.getBlockState(targetPos).getBlock();

                        // Check if the current block is in the fade sequence
                        int currentIndex = fadeSequence.indexOf(currentBlock);
                        if (currentIndex != -1 && currentIndex < fadeSequence.size() - 1) {
                            // Replace with the next block in the fade sequence
                            Block nextBlock = fadeSequence.get(currentIndex + 1);
                            world.setBlockState(targetPos, nextBlock.getDefaultState(), 3);
                        }
                    }
                }
            }
        }
    }

    public void update() {
        if (isExpanding) {
            expansionProgress += EXPANSION_RATE;
            float currentRadius = Math.min(MAX_RADIUS, expansionProgress);

            // Update each particle's scale and position
            for (Particle particle : explosionParticles) {
                BlockDisplayElement element = particle.element;


                // Scale should increase as it expands
                float scale = 2.3f + (currentRadius / MAX_RADIUS); // Adjusted scale for smaller blocks

                // Calculate new position
                Vec3d newPosition = center.add(particle.initialOffset.normalize().multiply(currentRadius));

                // Update particle position and scale
                element.setOverridePos(newPosition);
                element.setScale(new Vector3f(scale, scale, scale));
            }

            // Occasional random burns in the center
            if (random.nextInt(5) == 0) { // Adjust frequency by changing this probability
                for (Particle particle : burnParticles) {
                    BlockDisplayElement element = particle.element;

                    // Ascend particles
                    Vec3d newPosition = center.add(particle.initialOffset);
                    newPosition = newPosition.add(0, expansionProgress / 10.0, 0); // Adjust the ascent speed

                    // Update particle position
                    element.setOverridePos(newPosition);
                }
            }


            // Stop expanding when maximum size is reached
            if (currentRadius >= MAX_RADIUS) {
                fadeBlocks();
                isExpanding = false;
            }
        }
    }

    public boolean isFinished() {
        return !isExpanding && expansionProgress >= MAX_RADIUS;
    }

    public void cleanup() {
        for (Particle particle : explosionParticles) {
            particle.element.setScale(new Vector3f(0, 0, 0)); // Hide elements on cleanup
        }
        holder.destroy();
        explosionParticles.clear();
        burnParticles.clear();
    }

    private static class Particle {
        final BlockDisplayElement element;
        final Vec3d initialOffset; // Fixed initial offset
        final float initialRadius;

        Particle(BlockDisplayElement element, Vec3d initialOffset) {
            this.element = element;
            this.initialOffset = initialOffset;
            this.initialRadius = (float) initialOffset.length();
        }
    }
}
