package net.idothehax.rarays;

import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.idothehax.rarays.item.RaRayCraftingItem;
import net.idothehax.rarays.item.RaRaySpawnerItem;
import net.idothehax.rarays.laser.Laser;
import net.idothehax.rarays.laser.LaserTicker;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RaRays implements ModInitializer {
    public static final String MOD_ID = "rarays";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final List<Laser> lasers = new ArrayList<>();

    public static final RaRaySpawnerItem RA_RAY_SPAWNER_ITEM = registerItem("ra_ray_spawner", new RaRaySpawnerItem(new Item.Settings().fireproof().maxCount(1), Items.AMETHYST_SHARD));
    public static final RaRayCraftingItem SLIGHTLY_HEATED_AMETHYST = registerItem("slight_heated_amethyst", new RaRayCraftingItem(new Item.Settings().fireproof().maxCount(1), "slight_heated_amethyst"));
    public static final RaRayCraftingItem MODERATELY_TEMPERED_AMETHYST = registerItem("moderately_tempered_amethyst", new RaRayCraftingItem(new Item.Settings().fireproof().maxCount(1), "moderately_tempered_amethyst"));
    public static final RaRayCraftingItem ANNEALED_AMETHYST = registerItem("annealed_amethyst", new RaRayCraftingItem(new Item.Settings().fireproof().maxCount(1), "annealed_amethyst"));
    public static final RaRayCraftingItem GLOWING_HOT_AMETHYST = registerItem("glowing_hot_amethyst", new RaRayCraftingItem(new Item.Settings().fireproof().maxCount(1), "glowing_hot_amethyst"));


    public static final ItemGroup ITEM_GROUP = new ItemGroup.Builder(null, -1)
            .displayName(Text.translatable("rarays.itemgroup").formatted(Formatting.AQUA))
            .icon(()-> new ItemStack(RA_RAY_SPAWNER_ITEM))
            .entries((displayContext, entries) -> Registries.ITEM.streamEntries()
                    .filter(itemReference -> itemReference.getKey().map(key -> key.getValue().getNamespace().equals(MOD_ID)).orElse(false))
                    .forEach(item -> entries.add(new ItemStack(item))))
            .build();

    @Override
    public void onInitialize() {
        RaRays.LOGGER.info("Initializing Ra Rays");
        PolymerItemGroupUtils.registerPolymerItemGroup(Identifier.of(RaRays.MOD_ID, "ras_things"), ITEM_GROUP);
        PolymerResourcePackUtils.buildMain();
        LaserTicker.register();
    }

    private static <T extends Item> T registerItem(String id, T item) {
        return Registry.register(Registries.ITEM, Identifier.of(MOD_ID, id), item);
    }
}
