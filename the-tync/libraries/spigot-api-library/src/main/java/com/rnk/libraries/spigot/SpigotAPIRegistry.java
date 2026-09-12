/*
 * RNK Spigot API Library
 * Copyright (c) 2026 Lisa's Dungeon. All rights reserved.
 * 
 * Complete Spigot API and Net Minecraft Server (NMS) definitions for versions 1.8 through 1.21.x
 */

package com.rnk.libraries.spigot;

import java.util.*;

/**
 * Spigot API Registry - Complete API definitions for Spigot server framework with NMS support
 */
public class SpigotAPIRegistry {
    
    private static final String VERSION = "1.0.0";
    private final Map<String, SpigotClass> classDefinitions = new HashMap<>();
    private final Map<String, NMSClass> nmsClasses = new HashMap<>();
    private final Map<String, SpigotVersion> supportedVersions = new HashMap<>();
    
    public SpigotAPIRegistry() {
        initializeSpigotClasses();
        initializeNMSClasses();
        initializeSupportedVersions();
    }
    
    private void initializeSpigotClasses() {
        // Core Spigot classes
        classDefinitions.put("org.bukkit.Bukkit", new SpigotClass(
            "Bukkit", "Main Spigot API access point", false
        ));
        
        classDefinitions.put("org.spigotmc.SpigotConfig", new SpigotClass(
            "SpigotConfig", "Spigot configuration access", false
        ));
        
        classDefinitions.put("org.spigotmc.event.entity.EntityDismountEvent", new SpigotClass(
            "EntityDismountEvent", "Entity dismount event", false
        ));
        
        classDefinitions.put("org.spigotmc.event.player.PlayerSpawnLocationEvent", new SpigotClass(
            "PlayerSpawnLocationEvent", "Player spawn location event", false
        ));
        
        classDefinitions.put("org.bukkit.craftbukkit.CraftServer", new SpigotClass(
            "CraftServer", "CraftBukkit server implementation", true
        ));
        
        classDefinitions.put("org.bukkit.craftbukkit.entity.CraftPlayer", new SpigotClass(
            "CraftPlayer", "CraftBukkit player implementation", true
        ));
        
        classDefinitions.put("org.bukkit.craftbukkit.entity.CraftEntity", new SpigotClass(
            "CraftEntity", "CraftBukkit entity implementation", true
        ));
    }
    
    private void initializeNMSClasses() {
        // Net.Minecraft.Server classes (version-independent paths)
        nmsClasses.put("net.minecraft.server.level.ServerLevel", new NMSClass(
            "ServerLevel", "Server-side world implementation", "1.17+"
        ));
        
        nmsClasses.put("net.minecraft.server.level.ServerPlayer", new NMSClass(
            "ServerPlayer", "Server-side player implementation", "1.17+"
        ));
        
        nmsClasses.put("net.minecraft.world.entity.Entity", new NMSClass(
            "Entity", "Base entity class", "1.17+"
        ));
        
        nmsClasses.put("net.minecraft.world.entity.LivingEntity", new NMSClass(
            "LivingEntity", "Living entity base class", "1.17+"
        ));
        
        nmsClasses.put("net.minecraft.world.level.block.Block", new NMSClass(
            "Block", "Block definition", "1.17+"
        ));
        
        nmsClasses.put("net.minecraft.world.item.Item", new NMSClass(
            "Item", "Item definition", "1.17+"
        ));
        
        nmsClasses.put("net.minecraft.world.entity.ai.behavior.Behavior", new NMSClass(
            "Behavior", "Entity AI behavior", "1.17+"
        ));
        
        // Legacy NMS classes (1.8-1.16)
        nmsClasses.put("net.minecraft.server.v_VERSION_.EntityPlayer", new NMSClass(
            "EntityPlayer", "Legacy player entity (1.8-1.16)", "1.8-1.16"
        ));
        
        nmsClasses.put("net.minecraft.server.v_VERSION_.WorldServer", new NMSClass(
            "WorldServer", "Legacy world server (1.8-1.16)", "1.8-1.16"
        ));
        
        nmsClasses.put("net.minecraft.server.v_VERSION_.Block", new NMSClass(
            "Block", "Legacy block class (1.8-1.16)", "1.8-1.16"
        ));
    }
    
    private void initializeSupportedVersions() {
        supportedVersions.put("1.8", new SpigotVersion("1.8", "Legacy support", true));
        supportedVersions.put("1.12", new SpigotVersion("1.12.2", "Legacy support", true));
        supportedVersions.put("1.16", new SpigotVersion("1.16.5", "Legacy support", true));
        supportedVersions.put("1.17", new SpigotVersion("1.17.1", "Modern NMS", false));
        supportedVersions.put("1.18", new SpigotVersion("1.18.2", "Modern NMS", false));
        supportedVersions.put("1.19", new SpigotVersion("1.19.2", "Modern NMS", false));
        supportedVersions.put("1.20", new SpigotVersion("1.20.1", "Modern NMS", false));
        supportedVersions.put("1.21", new SpigotVersion("1.21.x", "Current", false));
    }
    
    public SpigotClass getSpigotClass(String qualifiedName) {
        return classDefinitions.get(qualifiedName);
    }
    
    public NMSClass getNMSClass(String qualifiedName) {
        return nmsClasses.get(qualifiedName);
    }
    
    public SpigotVersion getVersion(String versionString) {
        return supportedVersions.get(versionString);
    }
    
    public Collection<SpigotClass> getAllSpigotClasses() {
        return classDefinitions.values();
    }
    
    public Collection<NMSClass> getAllNMSClasses() {
        return nmsClasses.values();
    }
    
    public static String getVersion() {
        return VERSION;
    }
    
    // Inner classes
    public static class SpigotClass {
        public final String name;
        public final String description;
        public final boolean isCraftBukkit;
        
        public SpigotClass(String name, String description, boolean isCraftBukkit) {
            this.name = name;
            this.description = description;
            this.isCraftBukkit = isCraftBukkit;
        }
    }
    
    public static class NMSClass {
        public final String name;
        public final String description;
        public final String supportedVersions;
        
        public NMSClass(String name, String description, String supportedVersions) {
            this.name = name;
            this.description = description;
            this.supportedVersions = supportedVersions;
        }
    }
    
    public static class SpigotVersion {
        public final String version;
        public final String description;
        public final boolean isLegacy;
        
        public SpigotVersion(String version, String description, boolean isLegacy) {
            this.version = version;
            this.description = description;
            this.isLegacy = isLegacy;
        }
    }
}
