package net.idothehax.rarays.laser;

import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class FlashBurn {
    private static final int BURN_RADIUS = 14;
    private static final double RAY_DENSITY = 0.5;
    private static final Map<Block, BlockState[]> BURN_TRANSFORMATIONS = new HashMap<>();

    static {
        // blok transformations burning effect
        BURN_TRANSFORMATIONS.put(Blocks.OAK_LEAVES, new BlockState[]{
                Blocks.MANGROVE_ROOTS.getDefaultState(),
                Blocks.AIR.getDefaultState()
        });

        BURN_TRANSFORMATIONS.put(Blocks.GRASS_BLOCK, new BlockState[]{
                Blocks.ROOTED_DIRT.getDefaultState(),
                Blocks.TUFF.getDefaultState()
        });

        BURN_TRANSFORMATIONS.put(Blocks.DIRT, new BlockState[]{
                Blocks.ROOTED_DIRT.getDefaultState(),
                Blocks.TUFF.getDefaultState()
        });

        BURN_TRANSFORMATIONS.put(Blocks.OAK_WOOD, new BlockState[]{
                Blocks.BASALT.getDefaultState()
        });

        BURN_TRANSFORMATIONS.put(Blocks.OAK_LOG, new BlockState[]{
                Blocks.BASALT.getDefaultState()
        });

        BURN_TRANSFORMATIONS.put(Blocks.SHORT_GRASS, new BlockState[]{
                Blocks.DEAD_BUSH.getDefaultState(),
                Blocks.AIR.getDefaultState()
        });
    }

    public static void createBurnEffect(World world, Vec3d impactPos, PlayerEntity player) {
        // Move the burn effect 3 blocks over impact
        Vec3d burnOrigin = impactPos.add(0, 3, 0);
        Random random = new Random();

        // Calculate number of rays
        double surfaceArea = 4 * Math.PI * BURN_RADIUS * BURN_RADIUS;
        int numRays = (int)(surfaceArea * RAY_DENSITY);

        for (int i = 0; i < numRays; i++) {
            // Generate spherical coordinates
            double theta = random.nextDouble() * 2 * Math.PI;
            double phi = random.nextDouble() * Math.PI;

            // Convert to Cartesian coordinates
            double x = Math.sin(phi) * Math.cos(theta);
            double y = Math.sin(phi) * Math.sin(theta);
            double z = Math.cos(phi);
            Vec3d rayDir = new Vec3d(x, y, z);

            castBurnRay((ServerWorld) world, burnOrigin, rayDir, BURN_RADIUS, player);
        }
    }

    public static void castBurnRay(ServerWorld world, Vec3d startPos, Vec3d direction, int maxDistance, Entity entity) {
        Vec3d endPos = startPos.add(direction.multiply(maxDistance));

        ShapeContext shapeContext = ShapeContext.of(entity); // Provide a valid entity here

        // Construct the RaycastContext
        RaycastContext raycastContext = new RaycastContext(
                startPos,
                endPos,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                shapeContext
        );

        // Perform the raycasting
        HitResult hitResult = world.raycast(raycastContext);

        if (hitResult.getType() == HitResult.Type.BLOCK) {

            BlockPos blockPos = new BlockPos(MathHelper.floor(hitResult.getPos().x),
                    MathHelper.floor(hitResult.getPos().y),
                    MathHelper.floor(hitResult.getPos().z));

            transformBlock(world, blockPos);
        }
    }


    private static void transformBlock(World world, BlockPos pos) {
        BlockState currentState = world.getBlockState(pos);
        Block currentBlock = currentState.getBlock();

        BlockState[] transformations = BURN_TRANSFORMATIONS.get(currentBlock);
        if (transformations != null) {
            BlockState newState = transformations[new Random().nextInt(transformations.length)];
            world.setBlockState(pos, newState);
        }
    }
}