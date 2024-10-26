package net.idothehax.rarays.item;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.idothehax.rarays.RaRays;
import net.idothehax.rarays.laser.Laser;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class RaRaySpawnerItem extends SimplePolymerItem {

    public RaRaySpawnerItem(Settings settings, Item polymerItem) {
        super(settings, polymerItem);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient) {
            Laser laser = new Laser(world, user);
            laser.spawnLaser();
            RaRays.lasers.add(laser);
            return TypedActionResult.success(user.getStackInHand(hand));
        }
        return TypedActionResult.pass(user.getStackInHand(hand));
    }
}
