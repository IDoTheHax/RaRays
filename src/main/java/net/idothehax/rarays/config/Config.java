package net.idothehax.rarays.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("rarays.json");
    private static Config INSTANCE;

    private int raRaysCooldown = 5000; // Default cooldown in ticks

    public static Config getInstance() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                    INSTANCE = GSON.fromJson(reader, Config.class);
                }
            } else {
                INSTANCE = new Config();
                save();
            }
        } catch (Exception e) {
            System.err.println("Failed to load config: " + e.getMessage());
            INSTANCE = new Config();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (Exception e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    public int getRaRaysCooldown() {
        return raRaysCooldown;
    }

    public void setRaRaysCooldown(int cooldown) {
        this.raRaysCooldown = cooldown;
        save();
    }
}