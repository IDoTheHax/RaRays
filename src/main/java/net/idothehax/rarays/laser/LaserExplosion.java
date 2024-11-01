package net.idothehax.rarays.laser;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.util.math.random.Random;

import java.util.*;

import static net.idothehax.rarays.laser.FlashBurn.BURN_TRANSFORMATIONS;

public class LaserExplosion {
    private final List<Particle> explosionParticles = new ArrayList<>();
    private final List<Particle> burnParticles = new ArrayList<>();
    private final List<Particle> shockwaveParticles = new ArrayList<>();

    private final Vec3d center;
    private final World world;
    private float expansionProgress = 0;
    private static final float EXPANSION_RATE = 0.8f; // Faster expansion rate
    private static final float MAX_RADIUS = 120.0f;
    private static final int PARTICLES_PER_RING = 96;
    private static final int NUMBER_OF_RINGS = 3;
    private static final int SHOCKWAVE_PARTICLES_PER_RING = 256;
    private static final float SHOCKWAVE_EXPANSION_RATE = 3.0f; // Faster rate for shockwave
    private static final float MAX_SHOCKWAVE_RADIUS = 150.0f;
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
        createShockwaveParticles();
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

    // Shockwave particle setup
    private void createShockwaveParticles() {
        var shockwaveBlocks = new Block[]{Blocks.LIGHT_GRAY_CONCRETE_POWDER, Blocks.LIGHT_GRAY_CONCRETE};
        float shockwaveRadius = MAX_RADIUS / 4;
        for (int i = 0; i < SHOCKWAVE_PARTICLES_PER_RING; i++) {
            BlockDisplayElement element = new BlockDisplayElement();
            element.setBlockState(shockwaveBlocks[random.nextInt(shockwaveBlocks.length)].getDefaultState());
            element.setScale(new Vector3f(6.0f, 6.0f, 64.0f));

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
            double angle = (2 * Math.PI / SHOCKWAVE_PARTICLES_PER_RING * 2) * i;
            Vec3d initialOffset = new Vec3d(
                    Math.cos(angle),
                    0,
                    Math.sin(angle)
            ).multiply(shockwaveRadius); // Set initial offset based on radius

            //Vec3d initialOffset = new Vec3d(Math.cos((2 * Math.PI / PARTICLES_PER_RING) * i), 0, Math.sin((2 * Math.PI / PARTICLES_PER_RING) * i)).multiply(shockwaveRadius);
            element.setOverridePos(center.add(initialOffset)); // Apply initial position
            shockwaveParticles.add(new Particle(element, initialOffset));
            holder.addElement(element);
        }
    }

    private void createBurnParticles() {
        var burnBlocks = new net.minecraft.block.Block[]{
                Blocks.ORANGE_STAINED_GLASS, Blocks.NETHERRACK, Blocks.TINTED_GLASS, Blocks.BLACK_STAINED_GLASS
        };

        int burnParticleCount = 9; // Control the number of burn particles

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

    private void burnBlocksInRadius(BlockPos blockPos, float particleBurnRadius) {
        // Iterate over a small area around the particle position
        int radius = (int) Math.ceil(particleBurnRadius);

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos targetPos = blockPos.add(x, y, z);

                    // Check if the block is within the particleBurnRadius from the particle's position
                    if (targetPos.isWithinDistance(blockPos, particleBurnRadius)) {
                        Block currentBlock = world.getBlockState(targetPos).getBlock();

                        BlockState[] transformations = BURN_TRANSFORMATIONS.get(currentBlock);
                        // Burn only if the current block is in the burnable list
                        if (transformations != null) {
                            BlockState newState = transformations[random.nextInt(transformations.length)];
                            world.setBlockState(targetPos, newState);
                        }
                    }
                }
            }
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

            // Set the detection radius around each particle for burn checks
            float particleBurnRadius = 1.5f;

            // Update each particle's scale and position
            for (Particle particle : explosionParticles) {
                BlockDisplayElement element = particle.element;


                // Scale should increase as it expands
                float scale = 2.3f + (currentRadius / MAX_RADIUS); // Adjusted scale for smaller blocks

                // Calculate new position
                Vec3d newPosition = center.add(particle.initialOffset.normalize().multiply(currentRadius));

                // Check for burning blocks in the particle's path
                BlockPos blockPos = new BlockPos((int) newPosition.x, (int) newPosition.y, (int) newPosition.z);
                if (world.isChunkLoaded(blockPos) && !world.isAir(blockPos)) {
                    // Transform the block to its burned variant if possible
                    burnBlocksInRadius(blockPos, particleBurnRadius);
                }

                // Update particle position and scale
                element.setOverridePos(newPosition);
                element.setScale(new Vector3f(scale, scale, scale));
            }

            for (Particle particle : shockwaveParticles) {
                BlockDisplayElement element = particle.element;

                float scale = 2.3f + (currentRadius / MAX_RADIUS);
                Vec3d newPosition = center.add(particle.initialOffset.normalize().multiply(currentRadius * 2.5f));
                element.setOverridePos(newPosition);
                element.setScale(new Vector3f(scale, scale, scale));

                // Check for entities within the shockwave's radius
                applyKnockbackToEntities(newPosition, scale); // new method for knockback
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

    private void applyKnockbackToEntities(Vec3d shockwavePosition, float shockwaveRadius) {
        // Check for entities near the shockwave particle
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class,
                new Box(shockwavePosition.subtract(1, 1, 1), shockwavePosition.add(1, 1, 1)),
                entity -> entity != null && entity.isAlive());

        for (LivingEntity entity : entities) {
            Vec3d knockbackDirection = entity.getPos().subtract(shockwavePosition).normalize();
            entity.setVelocity(entity.getVelocity().add(knockbackDirection.multiply(1.5).add(0, 5.0, 0))); // Adjust knockback strength
            entity.velocityModified = true; // Mark velocity as modified
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
        shockwaveParticles.clear();
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
