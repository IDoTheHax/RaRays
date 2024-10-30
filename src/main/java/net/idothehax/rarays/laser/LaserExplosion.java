package net.idothehax.rarays.laser;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

public class LaserExplosion {
    private final List<Particle> explosionParticles = new ArrayList<>();
    private final Vec3d center;
    private final World world;
    private float expansionProgress = 0;
    private static final float EXPANSION_RATE = 0.15f;
    private static final float MAX_RADIUS = 5.0f;
    private static final int PARTICLES_PER_RING = 12;
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
    }

    private void createExplosionParticles() {
        var explosionBlocks = new net.minecraft.block.Block[]{
                Blocks.ORANGE_CONCRETE_POWDER,
                Blocks.YELLOW_CONCRETE_POWDER,
                Blocks.RED_CONCRETE_POWDER,
                Blocks.WHITE_CONCRETE_POWDER,
                Blocks.LIGHT_GRAY_CONCRETE_POWDER
        };

        // Create multiple rings of particles
        for (int ring = 0; ring < NUMBER_OF_RINGS; ring++) {
            float radius = MAX_RADIUS * (ring + 1) / NUMBER_OF_RINGS;
            for (int i = 0; i < PARTICLES_PER_RING; i++) {
                BlockDisplayElement element = new BlockDisplayElement();
                element.setBlockState(explosionBlocks[random.nextInt(explosionBlocks.length)].getDefaultState());
                element.setScale(new Vector3f(0.5f, 0.5f, 0.5f)); // Initial scale

                // Calculate initial position in a circle
                double angle = (2 * Math.PI / PARTICLES_PER_RING) * i;
                Vec3d initialPosition = new Vec3d(
                        center.x + Math.cos(angle) * radius,
                        center.y, // Use the adjusted height here
                        center.z + Math.sin(angle) * radius
                );

                element.setOverridePos(initialPosition);
                explosionParticles.add(new Particle(element, radius));
                holder.addElement(element);
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
                float scale = 0.5f + (currentRadius / MAX_RADIUS); // This controls the overall scale

                // Calculate new position
                // Here, we will calculate the new position based on the current expansion radius
                Vec3d newPosition = center.add(particle.initialOffset.normalize().multiply(currentRadius));

                // Update particle position and scale
                element.setOverridePos(newPosition);
                element.setScale(new Vector3f(scale, scale, scale));
            }

            // Stop expanding when maximum size is reached
            if (currentRadius >= MAX_RADIUS) {
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
    }

    private static class Particle {
        final BlockDisplayElement element;
        Vec3d initialOffset; // Store initial offset for calculating new position
        final float initialRadius;

        Particle(BlockDisplayElement element, float initialRadius) {
            this.element = element;
            this.initialOffset = initialOffset; // Calculate offset from the center
            this.initialRadius = initialRadius;
        }
    }
}
