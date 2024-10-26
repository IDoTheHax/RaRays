package net.idothehax.rarays.laser;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LaserTicker implements ServerTickEvents.EndTick {
    private static final List<Laser> activeLasers = new ArrayList<>();
    private static LaserTicker INSTANCE;

    private LaserTicker() {}

    public static LaserTicker getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LaserTicker();
        }
        return INSTANCE;
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(getInstance());
    }

    public static void addLaser(Laser laser) {
        activeLasers.add(laser);
    }

    @Override
    public void onEndTick(MinecraftServer server) {
        Iterator<Laser> iterator = activeLasers.iterator();
        while (iterator.hasNext()) {
            Laser laser = iterator.next();
            laser.update();
            // Remove the laser if it's finished (you'll need to add this method to Laser class)
            if (laser.isFinished()) {
                iterator.remove();
            }
        }
    }
}