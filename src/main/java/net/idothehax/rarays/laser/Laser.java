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

    private final List<BlockDisplayElement> glassElements = new ArrayList<>();
    private final List<BlockDisplayElement> laserElements = new ArrayList<>();
    private final List<BlockDisplayElement> activeGlassElements = new ArrayList<>();
    private final List<BlockDisplayElement> activeLaserElements = new ArrayList<>();

    private List<ElementHolder> rayElements = new ArrayList<ElementHolder>();

    private ElementHolder holder;
    private float oscillationTime = 0.0f;
    private int spawnIndex = 0;
    private boolean isSpawning = false;
    private boolean isDespawning = false;
    private int despawnIndex = 0;
    private Vec3d targetPosition;
    private Vec3d lastElementPosition;
    private boolean isFinished = false;

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
        spawnIndex = 0;

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
        if (!isDespawning && !glassElements.isEmpty() && lastElementPosition != null) {
            if (lastElementPosition.y <= targetPosition.y && spawnIndex >= laserElements.size()) {

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
                createFlashBurnEffect(lastElementPosition);
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

    private void createFlashBurnEffect(Vec3d center) {
        int rayCount = 12; // Number of rays to cast in different directions
        double rayLength = 5.0; // Length of each ray
        for (int i = 0; i < rayCount; i++) {
            // Calculate the angle for each ray
            double angle = (2 * Math.PI / rayCount) * i;
            Vec3d rayDirection = new Vec3d(Math.cos(angle), 0, Math.sin(angle)).normalize().multiply(rayLength);

            // Calculate the end position of the ray
            Vec3d endPosition = center.add(rayDirection);

            // Cast a ray from the center to the end position
            BlockHitResult result = world.raycast(new RaycastContext(center, endPosition, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));

            // Spawn particles to visualize the ray
            spawnRayParticles(center, endPosition);

            // If a block was hit, change its state
            if (result.getType() == HitResult.Type.BLOCK) {
                BlockPos blockPos = result.getBlockPos();
                changeBlockState(blockPos);
            }
        }
    }

    private void spawnRayParticles(Vec3d start, Vec3d end) {
        // Clear previous ray elements if necessary
        for (ElementHolder holder : rayElements) {
            holder.destroy(); // Remove each holder
        }
        rayElements.clear();

        // Calculate the distance and direction
        double distance = start.distanceTo(end);
        Vec3d direction = end.subtract(start).normalize();

        // Adjust these values for the number of blocks and their size
        int blockCount = (int) Math.ceil(distance / 0.5); // 0.5 block intervals

        for (int i = 0; i < blockCount; i++) {
            // Calculate the position for each block
            Vec3d blockPos = start.add(direction.multiply(i * 0.5));

            // Create the BlockDisplayElement for the ray
            BlockDisplayElement blockDisplayElement = new BlockDisplayElement(Blocks.GLASS.getDefaultState());

            // Set the translation and scale to make it stretched and visible
            blockDisplayElement.setTranslation(new Vector3f((float) blockPos.x, (float) blockPos.y, (float) blockPos.z));
            blockDisplayElement.setScale(new Vector3f(0.1f, 0.1f, 0.5f)); // Adjusted for line visibility

            // Create an ElementHolder to manage this BlockDisplayElement
            ElementHolder holder = new ElementHolder(); // Pass world context
            holder.addElement(blockDisplayElement); // Add element to the holder
            rayElements.add(holder); // Add holder to list
        }
    }



    private void changeBlockState(BlockPos pos) {
        // Example: Change the block at the position to a different block type (e.g., Netherrack)
        if (world.getBlockState(pos).getBlock() != Blocks.AIR) {
            world.setBlockState(pos, Blocks.NETHERRACK.getDefaultState(), 3); // Use a different block as needed
        }
    }

}