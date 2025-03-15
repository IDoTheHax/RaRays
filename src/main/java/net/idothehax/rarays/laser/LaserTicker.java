package net.idothehax.rarays.laser;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LaserTicker implements ServerTickEvents.EndTick {
    private static final List<Laser> activeLasers = new ArrayList<>();
    private static final List<LaserExplosion> activeExplosions = new ArrayList<>();
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

    public static void addExplosion(LaserExplosion explosion) {
        activeExplosions.add(explosion);
    }

    @Override
    public void onEndTick(MinecraftServer server) {
        // Update all active lasers
        Iterator<Laser> laserIterator = activeLasers.iterator();
        while (laserIterator.hasNext()) {
            Laser laser = laserIterator.next();
            laser.update();
            if (laser.isFinished()) {
                laserIterator.remove();
            }
        }

        // Update all active explosions
        Iterator<LaserExplosion> explosionIterator = activeExplosions.iterator();
        while (explosionIterator.hasNext()) {
            LaserExplosion explosion = explosionIterator.next();
            explosion.update();
            if (explosion.isFinished()) {
                explosion.cleanup(); // Clean up resources
                explosionIterator.remove(); // Remove finished explosion
            }
        }
    }
}