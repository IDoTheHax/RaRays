package net.idothehax.rarays.laser;

import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
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
    public static final Map<Block, BlockState[]> BURN_TRANSFORMATIONS = new HashMap<>();

    static {
        // Register transformations for all leaf blocks using tags
        Registries.BLOCK.forEach(block -> {
            if (block.getDefaultState().isIn(BlockTags.LEAVES)) {
                BURN_TRANSFORMATIONS.put(block, new BlockState[]{
                        Blocks.MANGROVE_ROOTS.getDefaultState(),
                        Blocks.AIR.getDefaultState()
                });
            }
        });

        // Register transformations for all log and wood blocks using tags
        Registries.BLOCK.forEach(block -> {
            if (block.getDefaultState().isIn(BlockTags.LOGS) ||
                    block.getDefaultState().isIn(BlockTags.WOODEN_FENCES) ||
                    block.getDefaultState().isIn(BlockTags.PLANKS)) {
                BURN_TRANSFORMATIONS.put(block, new BlockState[]{
                        Blocks.MAGMA_BLOCK.getDefaultState(),
                        Blocks.COAL_BLOCK.getDefaultState()
                });
            }
        });

        // Register transformations for all flowers
        Registries.BLOCK.forEach(block -> {
            if (block.getDefaultState().isIn(BlockTags.FLOWERS) ||
                    block.getDefaultState().isIn(BlockTags.SMALL_FLOWERS) ||
                    block.getDefaultState().isIn(BlockTags.TALL_FLOWERS)) {
                BURN_TRANSFORMATIONS.put(block, new BlockState[]{
                        Blocks.AIR.getDefaultState(),
                        Blocks.DEAD_BUSH.getDefaultState()
                });
            }
        });

        // Other specific block transformations
        BURN_TRANSFORMATIONS.put(Blocks.GRASS_BLOCK, new BlockState[]{
                Blocks.COARSE_DIRT.getDefaultState(),
                Blocks.ROOTED_DIRT.getDefaultState(),
                Blocks.TUFF.getDefaultState(),
                Blocks.MAGMA_BLOCK.getDefaultState()
        });

        BURN_TRANSFORMATIONS.put(Blocks.DIRT, new BlockState[]{
                Blocks.COARSE_DIRT.getDefaultState(),
                Blocks.ROOTED_DIRT.getDefaultState(),
                Blocks.TUFF.getDefaultState(),
                Blocks.MAGMA_BLOCK.getDefaultState()
        });

        BURN_TRANSFORMATIONS.put(Blocks.STONE, new BlockState[]{
                Blocks.TUFF.getDefaultState(),
                Blocks.ANDESITE.getDefaultState(),
                Blocks.DEEPSLATE.getDefaultState(),
                Blocks.OBSIDIAN.getDefaultState(),
                Blocks.MAGMA_BLOCK.getDefaultState()
        });

        BURN_TRANSFORMATIONS.put(Blocks.GRANITE, new BlockState[]{
                Blocks.TUFF.getDefaultState(),
                Blocks.ANDESITE.getDefaultState(),
                Blocks.DEEPSLATE.getDefaultState(),
                Blocks.OBSIDIAN.getDefaultState(),
                Blocks.MAGMA_BLOCK.getDefaultState()
        });

        BURN_TRANSFORMATIONS.put(Blocks.COAL_ORE, new BlockState[]{
                Blocks.DIAMOND_ORE.getDefaultState(),
                Blocks.DEEPSLATE_DIAMOND_ORE.getDefaultState(),
        });

        BURN_TRANSFORMATIONS.put(Blocks.IRON_ORE, new BlockState[]{
                Blocks.RAW_IRON_BLOCK.getDefaultState(),
        });

        BURN_TRANSFORMATIONS.put(Blocks.SHORT_GRASS, new BlockState[]{
                Blocks.AIR.getDefaultState(),
                Blocks.DEAD_BUSH.getDefaultState(),
                Blocks.DEAD_BRAIN_CORAL_FAN.getDefaultState().with(Properties.WATERLOGGED, false),
        });

        BURN_TRANSFORMATIONS.put(Blocks.FERN, new BlockState[]{
                Blocks.AIR.getDefaultState(),
                Blocks.DEAD_BUSH.getDefaultState(),
                Blocks.DEAD_BRAIN_CORAL_FAN.getDefaultState().with(Properties.WATERLOGGED, false),
        });

        BURN_TRANSFORMATIONS.put(Blocks.TALL_GRASS, new BlockState[]{
                Blocks.AIR.getDefaultState(),
                Blocks.DEAD_BUSH.getDefaultState(),
                Blocks.DEAD_BRAIN_CORAL_FAN.getDefaultState().with(Properties.WATERLOGGED, false),
        });

        BURN_TRANSFORMATIONS.put(Blocks.LARGE_FERN, new BlockState[]{
                Blocks.AIR.getDefaultState(),
                Blocks.DEAD_BUSH.getDefaultState(),
                Blocks.DEAD_BRAIN_CORAL_FAN.getDefaultState().with(Properties.WATERLOGGED, false),
        });

        BURN_TRANSFORMATIONS.put(Blocks.VINE, new BlockState[]{
                Blocks.AIR.getDefaultState(),
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