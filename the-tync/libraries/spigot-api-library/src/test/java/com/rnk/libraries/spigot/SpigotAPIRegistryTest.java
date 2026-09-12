/*
 * RNK Spigot API Library - Test Suite
 * Copyright (c) 2026 Lisa's Dungeon. All rights reserved.
 */

package com.rnk.libraries.spigot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.Collection;
import static org.junit.jupiter.api.Assertions.*;

public class SpigotAPIRegistryTest {
    
    private SpigotAPIRegistry registry;
    
    @BeforeEach
    public void setUp() {
        registry = new SpigotAPIRegistry();
    }
    
    @Test
    public void testRegistryInitialization() {
        assertNotNull(registry);
    }
    
    @Test
    public void testGetBukkitClass() {
        SpigotAPIRegistry.SpigotClass bukkit = registry.getSpigotClass("org.bukkit.Bukkit");
        assertNotNull(bukkit);
        assertEquals("Bukkit", bukkit.name);
        assertFalse(bukkit.isCraftBukkit);
    }
    
    @Test
    public void testGetSpigotConfigClass() {
        SpigotAPIRegistry.SpigotClass config = registry.getSpigotClass("org.spigotmc.SpigotConfig");
        assertNotNull(config);
        assertEquals("SpigotConfig", config.name);
        assertFalse(config.isCraftBukkit);
    }
    
    @Test
    public void testGetCraftServerClass() {
        SpigotAPIRegistry.SpigotClass craftServer = registry.getSpigotClass("org.bukkit.craftbukkit.CraftServer");
        assertNotNull(craftServer);
        assertEquals("CraftServer", craftServer.name);
        assertTrue(craftServer.isCraftBukkit);
    }
    
    @Test
    public void testGetCraftPlayerClass() {
        SpigotAPIRegistry.SpigotClass craftPlayer = registry.getSpigotClass("org.bukkit.craftbukkit.entity.CraftPlayer");
        assertNotNull(craftPlayer);
        assertEquals("CraftPlayer", craftPlayer.name);
        assertTrue(craftPlayer.isCraftBukkit);
    }
    
    @Test
    public void testGetNMSClass() {
        SpigotAPIRegistry.NMSClass entity = registry.getNMSClass("net.minecraft.world.entity.Entity");
        assertNotNull(entity);
        assertEquals("Entity", entity.name);
        assertNotNull(entity.supportedVersions);
    }
    
    @Test
    public void testGetServerLevelNMS() {
        SpigotAPIRegistry.NMSClass serverLevel = registry.getNMSClass("net.minecraft.server.level.ServerLevel");
        assertNotNull(serverLevel);
        assertEquals("ServerLevel", serverLevel.name);
        assertTrue(serverLevel.supportedVersions.contains("1.17"));
    }
    
    @Test
    public void testGetLivingEntityNMS() {
        SpigotAPIRegistry.NMSClass livingEntity = registry.getNMSClass("net.minecraft.world.entity.LivingEntity");
        assertNotNull(livingEntity);
        assertEquals("LivingEntity", livingEntity.name);
    }
    
    @Test
    public void testGetLegacyNMSClass() {
        SpigotAPIRegistry.NMSClass legacyPlayer = registry.getNMSClass("net.minecraft.server.v_VERSION_.EntityPlayer");
        assertNotNull(legacyPlayer);
        assertEquals("EntityPlayer", legacyPlayer.name);
        assertTrue(legacyPlayer.supportedVersions.contains("1.8"));
    }
    
    @Test
    public void testGetVersion() {
        SpigotAPIRegistry.SpigotVersion version = registry.getVersion("1.21");
        assertNotNull(version);
        assertEquals("1.21.x", version.version);
        assertFalse(version.isLegacy);
    }
    
    @Test
    public void testGetLegacyVersion() {
        SpigotAPIRegistry.SpigotVersion version = registry.getVersion("1.16");
        assertNotNull(version);
        assertTrue(version.isLegacy);
    }
    
    @Test
    public void testGetAllSpigotClasses() {
        Collection<SpigotAPIRegistry.SpigotClass> classes = registry.getAllSpigotClasses();
        assertNotNull(classes);
        assertTrue(classes.size() > 0);
        assertTrue(classes.size() >= 7);
    }
    
    @Test
    public void testGetAllNMSClasses() {
        Collection<SpigotAPIRegistry.NMSClass> nmsClasses = registry.getAllNMSClasses();
        assertNotNull(nmsClasses);
        assertTrue(nmsClasses.size() > 0);
        assertTrue(nmsClasses.size() >= 10);
    }
    
    @Test
    public void testVersionString() {
        String version = SpigotAPIRegistry.getVersion();
        assertNotNull(version);
        assertEquals("1.0.0", version);
    }
    
    @Test
    public void testNMSClassDescription() {
        SpigotAPIRegistry.NMSClass entity = registry.getNMSClass("net.minecraft.world.entity.Entity");
        assertNotNull(entity.description);
        assertFalse(entity.description.isEmpty());
    }
}
