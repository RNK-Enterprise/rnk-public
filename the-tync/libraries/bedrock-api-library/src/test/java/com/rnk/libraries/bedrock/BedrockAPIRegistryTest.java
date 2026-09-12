/*
 * RNK Bedrock API Library - Test Suite
 * Copyright (c) 2026 RNK Studios. Licensed under GPL-3.0-only.
 */

package com.rnk.libraries.bedrock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.Collection;
import static org.junit.jupiter.api.Assertions.*;

public class BedrockAPIRegistryTest {
    
    private BedrockAPIRegistry registry;
    
    @BeforeEach
    public void setUp() {
        registry = new BedrockAPIRegistry();
    }
    
    @Test
    public void testRegistryInitialization() {
        assertNotNull(registry);
    }
    
    @Test
    public void testGetServerModule() {
        BedrockAPIRegistry.BedrockModule module = registry.getModule("@minecraft/server");
        assertNotNull(module);
        assertEquals("@minecraft/server", module.name);
        assertNotNull(module.description);
    }
    
    @Test
    public void testGetServerUIModule() {
        BedrockAPIRegistry.BedrockModule module = registry.getModule("@minecraft/server-ui");
        assertNotNull(module);
        assertEquals("@minecraft/server-ui", module.name);
    }
    
    @Test
    public void testGetGameTestModule() {
        BedrockAPIRegistry.BedrockModule module = registry.getModule("@minecraft/server-gametest");
        assertNotNull(module);
        assertEquals("@minecraft/server-gametest", module.name);
    }
    
    @Test
    public void testGetAllModules() {
        Collection<BedrockAPIRegistry.BedrockModule> modules = registry.getAllModules();
        assertNotNull(modules);
        assertTrue(modules.size() > 0);
        assertTrue(modules.size() >= 5);
    }
    
    @Test
    public void testGetHealthComponent() {
        BedrockAPIRegistry.BedrockComponent health = registry.getComponent("minecraft:health");
        assertNotNull(health);
        assertEquals("minecraft:health", health.id);
        assertEquals("Entity", health.targetType);
    }
    
    @Test
    public void testGetMovementComponent() {
        BedrockAPIRegistry.BedrockComponent movement = registry.getComponent("minecraft:movement");
        assertNotNull(movement);
        assertEquals("minecraft:movement", movement.id);
        assertEquals("Entity", movement.targetType);
    }
    
    @Test
    public void testGetInventoryComponent() {
        BedrockAPIRegistry.BedrockComponent inventory = registry.getComponent("minecraft:inventory");
        assertNotNull(inventory);
        assertEquals("minecraft:inventory", inventory.id);
        assertEquals("Entity", inventory.targetType);
    }
    
    @Test
    public void testGetBlockComponentRedstone() {
        BedrockAPIRegistry.BedrockComponent redstone = registry.getComponent("minecraft:redstone");
        assertNotNull(redstone);
        assertEquals("minecraft:redstone", redstone.id);
        assertEquals("Block", redstone.targetType);
    }
    
    @Test
    public void testGetAllComponents() {
        Collection<BedrockAPIRegistry.BedrockComponent> components = registry.getAllComponents();
        assertNotNull(components);
        assertTrue(components.size() > 0);
        assertTrue(components.size() >= 8);
    }
    
    @Test
    public void testGetServerInitializeEvent() {
        BedrockAPIRegistry.BedrockEvent event = registry.getEvent("minecraft:server_initialize");
        assertNotNull(event);
        assertEquals("minecraft:server_initialize", event.name);
        assertEquals("SystemEvent", event.category);
    }
    
    @Test
    public void testGetTickEvent() {
        BedrockAPIRegistry.BedrockEvent event = registry.getEvent("minecraft:tick");
        assertNotNull(event);
        assertEquals("minecraft:tick", event.name);
        assertEquals("SystemEvent", event.category);
    }
    
    @Test
    public void testGetPlayerSpawnEvent() {
        BedrockAPIRegistry.BedrockEvent event = registry.getEvent("minecraft:player_spawn");
        assertNotNull(event);
        assertEquals("minecraft:player_spawn", event.name);
        assertEquals("PlayerEvent", event.category);
    }
    
    @Test
    public void testGetEntityDeathEvent() {
        BedrockAPIRegistry.BedrockEvent event = registry.getEvent("minecraft:entity_die");
        assertNotNull(event);
        assertEquals("minecraft:entity_die", event.name);
        assertEquals("EntityEvent", event.category);
    }
    
    @Test
    public void testGetBlockBreakEvent() {
        BedrockAPIRegistry.BedrockEvent event = registry.getEvent("minecraft:block_break");
        assertNotNull(event);
        assertEquals("minecraft:block_break", event.name);
        assertEquals("BlockEvent", event.category);
    }
    
    @Test
    public void testGetAllEvents() {
        Collection<BedrockAPIRegistry.BedrockEvent> events = registry.getAllEvents();
        assertNotNull(events);
        assertTrue(events.size() > 0);
        assertTrue(events.size() >= 8);
    }
    
    @Test
    public void testVersionString() {
        String version = BedrockAPIRegistry.getVersion();
        assertNotNull(version);
        assertEquals("1.0.0", version);
    }
    
    @Test
    public void testComponentDescription() {
        BedrockAPIRegistry.BedrockComponent health = registry.getComponent("minecraft:health");
        assertNotNull(health.description);
        assertFalse(health.description.isEmpty());
    }
}
