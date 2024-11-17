package net.idothehax.rarays.item;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.idothehax.rarays.config.Config;
import net.idothehax.rarays.RaRays;
import net.idothehax.rarays.laser.Laser;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class RaRaySpawnerItem extends SimplePolymerItem {
    public RaRaySpawnerItem(Settings settings, Item polymerItem) {
        super(settings, polymerItem);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        // Check if the item is on cooldown, if so do nuthin
        if (user.getItemCooldownManager().isCoolingDown(this)) {
            user.sendMessage(Text.literal("Your Ray is on cooldown!").formatted(Formatting.DARK_RED), false);
            return TypedActionResult.pass(stack);
        }

        if (!world.isClient) {
            if (world.isNight()) {
                // Glowing effect for 60 seconds (1200 ticks)
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 1200, 0, false, false, true));
            }

            world.playSound((PlayerEntity)null, user.getX() + 0.5, user.getY() + 0.5, user.getZ() + 0.5, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 3.0F, 1.0F);

            // Spawn the laser
            Laser laser = new Laser(world, user);
            laser.spawnLaser();
            RaRays.lasers.add(laser);

            // Set the item on cooldown
            user.getItemCooldownManager().set(this, Config.getInstance().getRaRaysCooldown()); // 5 minutes

            return TypedActionResult.success(stack);
        }

        return TypedActionResult.pass(stack);
    }

}
