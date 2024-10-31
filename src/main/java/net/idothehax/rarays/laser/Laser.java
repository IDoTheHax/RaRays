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

import java.util.ArrayList;
import java.util.List;

public class Laser {
    private final World world;
    private final PlayerEntity player;
    private final Random random;
    private static final double MAX_DISTANCE = 100.0;
    private static final double MAX_HEIGHT = 200.0;
    private static final double MIN_HEIGHT = 100.0;
    private static final float OSCILLATION_AMPLITUDE = 0.3f;
    private static final double OSCILLATION_SPEED = 0.03f;
    private static final int TOTAL_SPAWN_RATE = 16; // Number of blocks to spawn per tick
    private static final int DESPAWN_RATE = 4; // Number of blocks to despawn per tick
    private int glassSpawnIndex = 0; // Index for spawning glass
    private int laserSpawnIndex = 0; // Index for spawning wool

    private LaserExplosion explosion;
    private final List<BlockDisplayElement> glassElements = new ArrayList<>();
    private final List<BlockDisplayElement> laserElements = new ArrayList<>();
    private final List<BlockDisplayElement> activeGlassElements = new ArrayList<>();
    private final List<BlockDisplayElement> activeLaserElements = new ArrayList<>();

    // New flag to track if the laser has fired
    private ElementHolder holder;
    private float oscillationTime = 0.0f;
    private boolean isSpawning = false;
    private boolean isDespawning = false;
    private int despawnIndex = 0;
    private Vec3d targetPosition;
    private Vec3d lastElementPosition;
    private boolean isFinished = false;
    private boolean explosionStarted = false;

    public Laser(World world, PlayerEntity player) {
        this.world = world;
        this.player = player;
        this.random = Random.create();
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void spawnLaser() {
        // Calculate hit position
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

        // Calculate start position
        double height = 200;
        double randomAngle = random.nextDouble() * Math.PI * 2;
        double radius = random.nextDouble() * 20.0;

        Vec3d startPos = new Vec3d(
                hitPos.x + Math.cos(randomAngle) * radius,
                height,
                hitPos.z + Math.sin(randomAngle) * radius
        );

        double dipDepth = 1.5;
        targetPosition = hitPos.add(0, -dipDepth , 0);

        double totalDistance = startPos.distanceTo(targetPosition);
        int numberOfBlocks = Math.max((int) (totalDistance * 4), 100);
        int numberOfGlassBlocks = numberOfBlocks / 2;

        holder = new ElementHolder();

        // Create all blocks but don't show them yet
        createBlocks(startPos, numberOfGlassBlocks, numberOfBlocks);

        BlockPos holderPos = new BlockPos((int) startPos.x, (int) startPos.y, (int) startPos.z);
        ChunkAttachment.ofTicking(holder, (ServerWorld) world, holderPos);

        // Start the spawn animation
        isSpawning = true;

        LaserTicker.addLaser(this);

    }

    private void createBlocks(Vec3d startPos, int numberOfGlassBlocks, int numberOfBlocks) {
        // Create glass blocks
        for (int i = 0; i < numberOfGlassBlocks; i++) {
            BlockDisplayElement glassElement = new BlockDisplayElement();
            glassElement.setBlockState(Blocks.WHITE_STAINED_GLASS.getDefaultState());

            double progress = (double) i / (numberOfGlassBlocks - 1);
            Vec3d pos = startPos.lerp(targetPosition, progress);

            double offsetAmount = 0.02;
            Vec3d offset = new Vec3d(
                    random.nextDouble() * offsetAmount - offsetAmount / 2,
                    random.nextDouble() * offsetAmount - offsetAmount / 2,
                    random.nextDouble() * offsetAmount - offsetAmount / 2
            );

            Vec3d finalPos = pos.add(offset).add(0.5, 0.5, 0.5);
            glassElement.setOverridePos(finalPos);
            glassElement.setScale(new Vector3f(0, 0, 0));  // Initially invisible

            if (i == numberOfGlassBlocks - 1) {
                lastElementPosition = finalPos;
            }

            glassElements.add(glassElement);
            holder.addElement(glassElement);
        }

        // Create wool blocks
        for (int i = 0; i < numberOfBlocks; i++) {
            BlockDisplayElement laserElement = new BlockDisplayElement();
            laserElement.setBlockState(Blocks.LIGHT_BLUE_WOOL.getDefaultState());

            double progress = (double) i / (numberOfBlocks - 1);
            Vec3d pos = startPos.lerp(targetPosition, progress);

            double offsetAmount = 0.02;
            Vec3d offset = new Vec3d(
                    random.nextDouble() * offsetAmount - offsetAmount / 2,
                    random.nextDouble() * offsetAmount - offsetAmount / 2,
                    random.nextDouble() * offsetAmount - offsetAmount / 2
            );

            Vec3d finalPos = pos.add(offset).add(0.75, 1, 0.75);
            laserElement.setOverridePos(finalPos);
            laserElement.setScale(new Vector3f(0, 0, 0));  // Initially invisible

            if (i == numberOfBlocks - 1) {
                lastElementPosition = finalPos;
            }

            laserElements.add(laserElement);
            holder.addElement(laserElement);
        }
    }

    public void update() {
        if (isSpawning) {
            updateSpawn();
        }

        if (isDespawning) {
            updateDespawn();
        }

        if (!isSpawning && !isDespawning) {

            updateOscillation();
        }

        // Check if the laser has reached the ground and wool elements are finished spawning
        if (!isDespawning && !glassElements.isEmpty() && lastElementPosition == null) {
        }

        if (!isSpawning) {
            if (lastElementPosition != null) {
                if (lastElementPosition.y <= targetPosition.y + 1) {
                    if (!explosionStarted) {
                        explosion = new LaserExplosion(world, targetPosition, holder);
                        explosionStarted = true;
                    }

                    FlashBurn.createBurnEffect(world, targetPosition, player);
                }
            }
        }

        if (explosion != null) {
            explosion.update();
            if (explosion.isFinished() && !isDespawning) {
                startDespawn();
            }
        }

    }


    private void updateSpawn() {
        // Total blocks spawned this tick
        int blocksSpawnedThisTick = 0;

        // Spawn blocks gradually
        while (blocksSpawnedThisTick < TOTAL_SPAWN_RATE) {
            // Spawn a glass block if available
            if (glassSpawnIndex < glassElements.size()) {
                BlockDisplayElement glassElement = glassElements.get(glassSpawnIndex);
                glassElement.setScale(new Vector3f(1.0f, 1.0f, 1.0f));
                activeGlassElements.add(glassElement);
                glassSpawnIndex++;
                blocksSpawnedThisTick++;
            }

            // Spawn a wool block if available
            if (laserSpawnIndex < laserElements.size() && blocksSpawnedThisTick < TOTAL_SPAWN_RATE) {
                BlockDisplayElement laserElement = laserElements.get(laserSpawnIndex);
                laserElement.setScale(new Vector3f(0.5f, 0.5f, 0.5f));
                activeLaserElements.add(laserElement);
                laserSpawnIndex++;
                blocksSpawnedThisTick++;
            }

            // If both have reached their limit, stop spawning
            if (glassSpawnIndex >= glassElements.size() && laserSpawnIndex >= laserElements.size()) {
                isSpawning = false;
                // TODO cool explosion stuff here
                break;
            }
        }
    }

    private void startDespawn() {
        isDespawning = true;
        despawnIndex = 0;
    }

    private void updateDespawn() {
        // Despawn blocks gradually from the top
        for (int i = 0; i < DESPAWN_RATE && despawnIndex < activeGlassElements.size(); i++) {
            BlockDisplayElement glassElement = activeGlassElements.get(despawnIndex);
            glassElement.setScale(new Vector3f(0, 0, 0));

            if (despawnIndex < activeLaserElements.size()) {
                BlockDisplayElement laserElement = activeLaserElements.get(despawnIndex);
                laserElement.setScale(new Vector3f(0, 0, 0));
            }

            despawnIndex++;
        }

        if (despawnIndex >= activeGlassElements.size()) {
            isDespawning = false;
            cleanup();
        }
    }

    private void cleanup() {
        if (holder != null) {
            holder.destroy();
        }
        glassElements.clear();
        laserElements.clear();
        activeGlassElements.clear();
        activeLaserElements.clear();
        lastElementPosition = null;

        if (explosion != null) {
            explosion.cleanup();
            explosion = null;
        }

        isFinished = true;  // Mark the laser as finished
    }

    public void updateOscillation() {
        oscillationTime += OSCILLATION_SPEED;
        double oscillationFactor = Math.sin(oscillationTime);
        float scale = 1.0f + (float)(oscillationFactor * OSCILLATION_AMPLITUDE);

        for (BlockDisplayElement glassElement : activeGlassElements) {
            glassElement.setScale(new Vector3f(scale, scale, scale));
        }

        for (BlockDisplayElement laserElement : activeLaserElements) {
            laserElement.setScale(new Vector3f((float) (0.5 * scale), (float) (0.5 * scale), (float) (0.5 * scale)));
        }
    }
}