package net.idothehax.rarays.laser;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import net.idothehax.rarays.config.Config;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
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
    Set<LivingEntity> affectedEntities = new HashSet<>(); // Track affected entities

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
    public static final double maxHearingDistance = 500.0;
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

        // Trigger the sound
        playSound((ServerWorld) world, center, maxHearingDistance);

    }

    private void playSound(ServerWorld world, Vec3d sourcePosition, double maxDistance) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            // Calculate distance from source to player
            Vec3d playerPosition = player.getPos();
            double distance = sourcePosition.distanceTo(playerPosition);

            // Normalize volume based on distance
            float volume = (float) Math.max(0, 1 - (distance / maxDistance));

            // Only play if volume > 0
            if (volume > 0) {
                world.playSound(
                        null,
                        player.getBlockPos().getX(),
                        player.getBlockPos().getY(),
                        player.getBlockPos().getZ(),
                        SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
                        SoundCategory.PLAYERS,
                        volume,
                        0f
                );
                world.playSound(
                        null,
                        player.getBlockPos().getX(),
                        player.getBlockPos().getY(),
                        player.getBlockPos().getZ(),
                        SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
                        SoundCategory.PLAYERS,
                        volume,
                        1f
                );
                world.playSound(
                        null,
                        player.getBlockPos().getX(),
                        player.getBlockPos().getY(),
                        player.getBlockPos().getZ(),
                        SoundEvents.ITEM_TOTEM_USE,
                        SoundCategory.PLAYERS,
                        volume,
                        0.5f
                );
            }
        }
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

            clearBlocksAround(currentRadius / 6);

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

                double entityBurnRadius = 3.0; // Adjust this radius as needed
                List<LivingEntity> entities = world.getEntitiesByClass(
                        LivingEntity.class,
                        new Box(newPosition.x - entityBurnRadius, newPosition.y - entityBurnRadius, newPosition.z - entityBurnRadius,
                                newPosition.x + entityBurnRadius, newPosition.y + entityBurnRadius, newPosition.z + entityBurnRadius),
                        entity -> !entity.isFireImmune()
                );

                for (LivingEntity entity : entities) {
                    // Ignite
                    entity.setFireTicks(entity.getFireTicks() + 1);
                    if (entity.getFireTicks() == 0) {
                        entity.setOnFireFor(8);
                    }
                    entity.damage(world.getDamageSources().inFire(), 1.0F);
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

                // Apply knockback only if entity hasn't been affected yet
                applyKnockbackToEntities(newPosition, scale, affectedEntities);
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

    private void clearBlocksAround(float currentRadius) {
        int radius = (int) Math.ceil(currentRadius);
        BlockPos centerPos = new BlockPos((int) center.x, (int) center.y, (int) center.z);
        Set<BlockPos> burningCandidates = new HashSet<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos targetPos = centerPos.add(x, y, z);

                    // Calculate the actual distance from the center
                    double distance = Math.sqrt(centerPos.getSquaredDistance(targetPos));

                    // Normalize the distance relative to the radius
                    double normalizedDistance = distance / currentRadius;

                    // Create a falloff effect - more destruction near the center, less at the edges
                    // Use a quadratic falloff for a more natural crater shape
                    double destructionIntensity = 1.0 - (normalizedDistance * normalizedDistance);

                    // Only process blocks within the actual spherical radius
                    if (distance <= currentRadius) {
                        BlockState currentState = world.getBlockState(targetPos);
                        Block currentBlock = currentState.getBlock();

                        // Randomize destruction based on distance from center
                        if (Math.random() < destructionIntensity) {
                            world.setBlockState(targetPos, Blocks.AIR.getDefaultState(), 3);
                            burningCandidates.add(targetPos);
                        }
                    }
                }
            }
        }

        // Burn blocks exposed to air
        for (BlockPos pos : burningCandidates) {
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = pos.offset(direction);
                if (world.getBlockState(neighborPos).getBlock() != Blocks.AIR) {
                    burnBlocksInRadius(neighborPos, 0.5f);
                }
            }
        }
    }

    private boolean shouldBurn(Block block) {
        // Define a condition to favor darker/hotter blocks for burning
        return block == Blocks.NETHERRACK || block == Blocks.MAGMA_BLOCK || block == Blocks.OBSIDIAN;
    }

    private void applyKnockbackToEntities(Vec3d shockwavePosition, float shockwaveRadius, Set<LivingEntity> affectedEntities) {
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class,
                new Box(shockwavePosition.subtract(1, 1, 1), shockwavePosition.add(1, 1, 1)),
                entity -> entity != null && entity.isAlive() && !affectedEntities.contains(entity)); // Ignore already affected entities

        float knockbackMultiplier = Config.getInstance().getKnockbackStrength(); // Get from config

        for (LivingEntity entity : entities) {
            affectedEntities.add(entity); // Mark as affected

            Vec3d knockbackDirection = entity.getPos().subtract(shockwavePosition).normalize();
            entity.setVelocity(entity.getVelocity().add(knockbackDirection.multiply(knockbackMultiplier).add(0, 5.0, 0)));
            entity.velocityModified = true;
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
