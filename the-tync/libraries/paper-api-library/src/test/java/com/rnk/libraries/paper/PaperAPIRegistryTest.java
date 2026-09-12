/*
 * RNK Paper API Library - Test Suite
 * Copyright (c) 2026 RNK Studios. Licensed under GPL-3.0-only.
 */

package com.rnk.libraries.paper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.Collection;
import static org.junit.jupiter.api.Assertions.*;

public class PaperAPIRegistryTest {
    
    private PaperAPIRegistry registry;
    
    @BeforeEach
    public void setUp() {
        registry = new PaperAPIRegistry();
    }
    
    @Test
    public void testRegistryInitialization() {
        assertNotNull(registry);
    }
    
    @Test
    public void testGetServerClass() {
        PaperAPIRegistry.PaperClass server = registry.getClass("org.bukkit.Server");
        assertNotNull(server);
        assertEquals("Server", server.name);
        assertTrue(server.isInterface);
    }
    
    @Test
    public void testGetPluginManager() {
        PaperAPIRegistry.PaperClass pluginMgr = registry.getClass("org.bukkit.plugin.PluginManager");
        assertNotNull(pluginMgr);
        assertEquals("PluginManager", pluginMgr.name);
        assertFalse(pluginMgr.isInterface);
    }
    
    @Test
    public void testGetPlayerClass() {
        PaperAPIRegistry.PaperClass player = registry.getClass("org.bukkit.entity.Player");
        assertNotNull(player);
        assertEquals("Player", player.name);
        assertTrue(player.isInterface);
    }
    
    @Test
    public void testGetWorldClass() {
        PaperAPIRegistry.PaperClass world = registry.getClass("org.bukkit.World");
        assertNotNull(world);
        assertEquals("World", world.name);
        assertTrue(world.isInterface);
    }
    
    @Test
    public void testGetBlockClass() {
        PaperAPIRegistry.PaperClass block = registry.getClass("org.bukkit.block.Block");
        assertNotNull(block);
        assertEquals("Block", block.name);
        assertTrue(block.isInterface);
    }
    
    @Test
    public void testGetInventoryClass() {
        PaperAPIRegistry.PaperClass inventory = registry.getClass("org.bukkit.inventory.Inventory");
        assertNotNull(inventory);
        assertEquals("Inventory", inventory.name);
        assertTrue(inventory.isInterface);
    }
    
    @Test
    public void testGetCommandExecutor() {
        PaperAPIRegistry.PaperClass cmdExec = registry.getClass("org.bukkit.command.CommandExecutor");
        assertNotNull(cmdExec);
        assertEquals("CommandExecutor", cmdExec.name);
        assertTrue(cmdExec.isInterface);
    }
    
    @Test
    public void testGetTabCompleter() {
        PaperAPIRegistry.PaperClass tabComp = registry.getClass("org.bukkit.command.TabCompleter");
        assertNotNull(tabComp);
        assertEquals("TabCompleter", tabComp.name);
        assertTrue(tabComp.isInterface);
    }
    
    @Test
    public void testGetServerLoadEvent() {
        PaperAPIRegistry.PaperEvent event = registry.getEvent("org.bukkit.event.server.ServerLoadEvent");
        assertNotNull(event);
        assertEquals("ServerLoadEvent", event.name);
        assertEquals("ServerEvent", event.parentEvent);
    }
    
    @Test
    public void testGetPlayerJoinEvent() {
        PaperAPIRegistry.PaperEvent event = registry.getEvent("org.bukkit.event.player.PlayerJoinEvent");
        assertNotNull(event);
        assertEquals("PlayerJoinEvent", event.name);
        assertEquals("PlayerEvent", event.parentEvent);
    }
    
    @Test
    public void testGetBlockBreakEvent() {
        PaperAPIRegistry.PaperEvent event = registry.getEvent("org.bukkit.event.block.BlockBreakEvent");
        assertNotNull(event);
        assertEquals("BlockBreakEvent", event.name);
        assertEquals("BlockEvent", event.parentEvent);
    }
    
    @Test
    public void testGetAllClasses() {
        Collection<PaperAPIRegistry.PaperClass> classes = registry.getAllClasses();
        assertNotNull(classes);
        assertTrue(classes.size() > 0);
        assertTrue(classes.size() >= 11);
    }
    
    @Test
    public void testGetAllEvents() {
        Collection<PaperAPIRegistry.PaperEvent> events = registry.getAllEvents();
        assertNotNull(events);
        assertTrue(events.size() > 0);
        assertTrue(events.size() >= 8);
    }
    
    @Test
    public void testVersionString() {
        String version = PaperAPIRegistry.getVersion();
        assertNotNull(version);
        assertEquals("1.0.0", version);
    }
    
    @Test
    public void testClassPurpose() {
        PaperAPIRegistry.PaperClass server = registry.getClass("org.bukkit.Server");
        assertNotNull(server.purpose);
        assertFalse(server.purpose.isEmpty());
    }
}
