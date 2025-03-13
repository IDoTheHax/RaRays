package net.idothehax.rarays.laser;

import net.idothehax.rarays.RaRays;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
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
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Laser {
    private final World world;
    private final PlayerEntity player;
    private final Random random;
    private static final double MAX_DISTANCE = 100.0;
    private static final double MAX_HEIGHT = 200.0;
    private Vec3d direction;
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

    private static final double SATELLITE_SPEED = 0.08; // Speed of the satellite's motion
    private double elapsedTime = 0.0; // Time to track the movement

    public Laser(World world, PlayerEntity player) {
        this.world = world;
        this.player = player;
        this.random = Random.create();
        this.direction = new Vec3d(1, 0, 0); // Example: Move in the +X direction
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void spawnLaser() {
        // Increment elapsed time
        elapsedTime += SATELLITE_SPEED;

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

        // Calculate the moving start position
        double height = MAX_HEIGHT - (Math.sin(elapsedTime) * 50); // Simulate movement in height
        double randomAngle = random.nextDouble() * Math.PI * 2;
        double radius = random.nextDouble() * 20.0;

        Vec3d startPos = new Vec3d(
                hitPos.x + Math.cos(randomAngle) * radius,
                height,
                hitPos.z + Math.sin(randomAngle) * radius
        );

        double dipDepth = 1.5;
        targetPosition = hitPos.add(0, -dipDepth, 0);

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

        world.playSound((PlayerEntity)null, startPos.getX() + 0.5, startPos.getY() + 0.5, startPos.getZ() + 0.5, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.BLOCKS, 10.0F, 0.8F);

        LaserTicker.addLaser(this);
    }

    public void createBlocks(Vec3d startPos, int numberOfGlassBlocks, int numberOfBlocks) {
        // Calculate the vector from start to target
        Vec3d laserVector = targetPosition.subtract(startPos);
        double distance = laserVector.length();

        // Normalize the vector to get direction
        Vec3d directionVec = laserVector.normalize();

        // Create outer beam (glass)
        BlockDisplayElement outerBeam = new BlockDisplayElement();
        outerBeam.setBlockState(Blocks.WHITE_STAINED_GLASS.getDefaultState());

        // Position at midpoint between start and target
        Vec3d midpoint = startPos.add(laserVector.multiply(0.5));
        outerBeam.setOverridePos(midpoint);

        // Create transformation to rotate and scale the beam along the direction vector
        Quaternionf rotation = createRotationFromVectors(new Vec3d(0, 1, 0), directionVec);

        // Set rotation first (before setting scale to zero)
        outerBeam.setLeftRotation(rotation);

        // Initially invisible
        outerBeam.setScale(new Vector3f(0, 0, 0));

        glassElements.add(outerBeam);
        holder.addElement(outerBeam);

        // Create inner beam (wool) - slightly thinner and brighter
        BlockDisplayElement innerBeam = new BlockDisplayElement();
        innerBeam.setBlockState(Blocks.LIGHT_BLUE_WOOL.getDefaultState());
        innerBeam.setOverridePos(midpoint);

        // Set rotation first
        innerBeam.setLeftRotation(rotation);

        // Initially invisible
        innerBeam.setScale(new Vector3f(0, 0, 0));

        laserElements.add(innerBeam);
        holder.addElement(innerBeam);

        // Set last element position for collision detection
        lastElementPosition = targetPosition;
    }

    private Quaternionf createRotationFromVectors(Vec3d from, Vec3d to) {
        // Convert Vec3d to Vector3f
        Vector3f fromVec = new Vector3f((float)from.x, (float)from.y, (float)from.z);
        Vector3f toVec = new Vector3f((float)to.x, (float)to.y, (float)to.z);

        // Normalize vectors
        fromVec.normalize();
        toVec.normalize();

        // Find rotation axis (cross product)
        Vector3f axis = new Vector3f();
        axis.set(fromVec).cross(toVec);

        // If vectors are parallel, we need a different approach
        if (axis.length() < 0.01f) {
            // Check if they point in same or opposite directions
            if (fromVec.dot(toVec) > 0) {
                // Same direction, no rotation needed
                return new Quaternionf().identity();
            } else {
                // Opposite direction, rotate 180° around any perpendicular axis
                Vector3f perpendicular = new Vector3f(1, 0, 0);
                if (Math.abs(fromVec.dot(perpendicular)) > 0.9f) {
                    perpendicular.set(0, 1, 0);
                }

                // Make perpendicular
                Vector3f tempVec = new Vector3f(perpendicular);
                tempVec.cross(fromVec);
                perpendicular = tempVec.normalize();

                // Create quaternion for 180-degree rotation
                return new Quaternionf().fromAxisAngleRad(
                        perpendicular.x(), perpendicular.y(), perpendicular.z(), (float)Math.PI
                );
            }
        }

        // Normalize rotation axis
        axis.normalize();

        // Calculate rotation angle
        float angle = (float)Math.acos(fromVec.dot(toVec));

        // Create and return quaternion
        return new Quaternionf().fromAxisAngleRad(
                axis.x(), axis.y(), axis.z(), angle
        );
    }


    public void update() {
        if (isSpawning) {
            updateSpawn();
        }

        if (isDespawning) {
            updateDespawn();
        }

        if (!isSpawning && !isDespawning) {
            moveLaser();
            updateOscillation();
        }

        if (!isDespawning && !glassElements.isEmpty() && lastElementPosition == null) {
        }

        if (!isSpawning) {
            if (lastElementPosition != null) {
                boolean shouldExplode = false;

                for (int dx = -3; dx <= 3; dx++) {
                    for (int dy = -3; dy <= 3; dy++) {
                        for (int dz = -3; dz <= 3; dz++) {
                            BlockPos nearbyPosition = BlockPos.ofFloored(targetPosition.add(dx, dy, dz));

                            // Check if this nearby position meets the condition for an explosion
                            if (lastElementPosition.y <= nearbyPosition.getY() + 1) {
                                shouldExplode = true;
                                break; // Exit the loop if any nearby position satisfies the condition
                            }
                        }
                        if (shouldExplode) break;
                    }
                    if (shouldExplode) break;
                }

                if (shouldExplode) {
                    if (!explosionStarted) {
                        explosion = new LaserExplosion(world, targetPosition, holder);
                        explosionStarted = true;
                    }

                    // Trigger the burn effect at the main target position
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

    private void moveLaser() {
        // Only move the starting position, keeping the target fixed
        if (lastElementPosition != null) {
            Vec3d displacement = direction.multiply(SATELLITE_SPEED);

            // Update only the beam elements' positions, not the target point
            for (BlockDisplayElement glassElement : activeGlassElements) {
                Vec3d currentPos = glassElement.getOverridePos();
                glassElement.setOverridePos(currentPos.add(displacement));

                // Recalculate the beam scale and rotation to keep it pointing at the fixed target
                updateBeamTransformation(glassElement, targetPosition);
            }

            for (BlockDisplayElement laserElement : activeLaserElements) {
                Vec3d currentPos = laserElement.getOverridePos();
                laserElement.setOverridePos(currentPos.add(displacement));

                // Recalculate the beam scale and rotation to keep it pointing at the fixed target
                updateBeamTransformation(laserElement, targetPosition);
            }
        }
    }

    private void updateBeamTransformation(BlockDisplayElement beam, Vec3d target) {
        Vec3d beamPos = beam.getOverridePos();
        Vec3d beamToTarget = target.subtract(beamPos);
        double distance = beamToTarget.length() * 2; // Double the distance for proper scaling
        Vec3d directionVec = beamToTarget.normalize();

        // Create rotation based on the current position to the fixed target
        Quaternionf rotation = createRotationFromVectors(new Vec3d(0, 1, 0), directionVec);

        // Update the beam's rotation
        beam.setLeftRotation(rotation);

        // Update scale - maintain thickness but adjust length
        Vector3f currentScale = (Vector3f) beam.getScale();
        float thickness = currentScale.x(); // Preserve current thickness
        beam.setScale(new Vector3f(thickness, (float)distance, thickness));
    }

    private void updateSpawn() {
        // Since we only have one or a few elements now, just make them visible
        if (!glassElements.isEmpty() && !activeGlassElements.contains(glassElements.get(0))) {
            BlockDisplayElement outerBeam = glassElements.get(0);
            // Make it visible by setting proper scale
            float beamThickness = 1.0f;
            double distance = targetPosition.distanceTo(outerBeam.getOverridePos()) * 2;
            outerBeam.setScale(new Vector3f(beamThickness, (float)distance, beamThickness));
            activeGlassElements.add(outerBeam);
            clearPathAlongBeam(outerBeam);
        }

        if (!laserElements.isEmpty() && !activeLaserElements.contains(laserElements.get(0))) {
            BlockDisplayElement innerBeam = laserElements.get(0);
            // Make it visible by setting proper scale
            float innerThickness = 0.5f;
            double distance = targetPosition.distanceTo(innerBeam.getOverridePos()) * 2;
            innerBeam.setScale(new Vector3f(innerThickness, (float)distance, innerThickness));
            activeLaserElements.add(innerBeam);
        }

        if (!glassElements.isEmpty() && !laserElements.isEmpty() &&
                activeGlassElements.contains(glassElements.get(0)) &&
                activeLaserElements.contains(laserElements.get(0))) {
            isSpawning = false;
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 600, 0, false, false, true));
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

        player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        isFinished = true;  // Mark the laser as finished
    }

    public void updateOscillation() {
        oscillationTime += OSCILLATION_SPEED;
        double oscillationFactor = Math.sin(oscillationTime);

        for (BlockDisplayElement glassElement : activeGlassElements) {
            Vector3f currentScale = new Vector3f(glassElement.getScale());
            // Only oscillate thickness (x and z), keep y (length) constant
            float thickness = 1.0f + (float)(oscillationFactor * OSCILLATION_AMPLITUDE);
            glassElement.setScale(new Vector3f(thickness, currentScale.y(), thickness));
        }

        for (BlockDisplayElement laserElement : activeLaserElements) {
            Vector3f currentScale = new Vector3f(laserElement.getScale());
            // Only oscillate thickness (x and z), keep y (length) constant
            float thickness = 0.5f + (float)(oscillationFactor * OSCILLATION_AMPLITUDE * 0.5f);
            laserElement.setScale(new Vector3f(thickness, currentScale.y(), thickness));
        }
    }

    private void clearPathAlongBeam(BlockDisplayElement element) {
        Vec3d beamDirection = targetPosition.subtract(element.getOverridePos()).normalize();
        double beamLength = element.getOverridePos().distanceTo(targetPosition);

        // Clear blocks along the entire beam path
        int steps = (int)(beamLength / 2.0) + 1;
        double stepSize = beamLength / steps;

        for (int step = 0; step < steps; step++) {
            Vec3d checkPos = element.getOverridePos().add(beamDirection.multiply(step * stepSize));
            BlockPos centerPos = BlockPos.ofFloored(checkPos);

            // Clear blocks in a radius of 2 around this point (thinner beam)
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        BlockPos nearbyPos = centerPos.add(dx, dy, dz);
                        // Only clear if it's not too far from beam center
                        if (dx*dx + dy*dy + dz*dz <= 4 &&
                                world.getBlockState(nearbyPos).isSolidBlock(world, nearbyPos)) {
                            // Set block to air if it is solid
                            world.setBlockState(nearbyPos, Blocks.AIR.getDefaultState());
                        }
                    }
                }
            }
        }
    }
}