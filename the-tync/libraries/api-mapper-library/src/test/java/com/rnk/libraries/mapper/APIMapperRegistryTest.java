/*
 * RNK API Mapper Library - Test Suite
 * Copyright (c) 2026 Lisa's Dungeon. All rights reserved.
 */

package com.rnk.libraries.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.Collection;
import static org.junit.jupiter.api.Assertions.*;

public class APIMapperRegistryTest {
    
    private APIMapperRegistry registry;
    
    @BeforeEach
    public void setUp() {
        registry = new APIMapperRegistry();
    }
    
    @Test
    public void testRegistryInitialization() {
        assertNotNull(registry);
    }
    
    @Test
    public void testGetForgeToFabricEventMapping() {
        APIMapperRegistry.APIMapping mapping = registry.getMapping("forge-to-fabric-event");
        assertNotNull(mapping);
        assertEquals("net.minecraftforge.eventbus.api.EventBus", mapping.sourceAPI);
        assertEquals("net.fabricmc.fabric.api.event.EventFactory", mapping.targetAPI);
        assertNotNull(mapping.translationRule);
    }
    
    @Test
    public void testGetForgeToPaperEventMapping() {
        APIMapperRegistry.APIMapping mapping = registry.getMapping("forge-to-paper-event");
        assertNotNull(mapping);
        assertEquals("net.minecraftforge.eventbus.api.EventBus", mapping.sourceAPI);
        assertEquals("org.bukkit.plugin.PluginManager", mapping.targetAPI);
    }
    
    @Test
    public void testGetFabricToSpigotEventMapping() {
        APIMapperRegistry.APIMapping mapping = registry.getMapping("fabric-to-spigot-event");
        assertNotNull(mapping);
        assertEquals("net.fabricmc.fabric.api.event.EventFactory", mapping.sourceAPI);
        assertEquals("org.bukkit.event.EventHandler", mapping.targetAPI);
    }
    
    @Test
    public void testGetForgeWorldMapping() {
        APIMapperRegistry.APIMapping mapping = registry.getMapping("forge-world-to-fabric");
        assertNotNull(mapping);
        assertNotNull(mapping.description);
        assertNotNull(mapping.translationRule);
    }
    
    @Test
    public void testGetPaperSpigotWorldMapping() {
        APIMapperRegistry.APIMapping mapping = registry.getMapping("paper-to-spigot-world");
        assertNotNull(mapping);
        assertEquals("org.bukkit.World", mapping.sourceAPI);
        assertEquals("org.bukkit.craftbukkit.CraftWorld", mapping.targetAPI);
    }
    
    @Test
    public void testGetForgeEntityMapping() {
        APIMapperRegistry.APIMapping mapping = registry.getMapping("forge-entity-to-fabric");
        assertNotNull(mapping);
        assertNotNull(mapping.description);
    }
    
    @Test
    public void testGetSpigotEntityMapping() {
        APIMapperRegistry.APIMapping mapping = registry.getMapping("spigot-entity-to-paper");
        assertNotNull(mapping);
        assertEquals("net.minecraft.world.entity.Entity", mapping.sourceAPI);
    }
    
    @Test
    public void testGetForgeRegistryMapping() {
        APIMapperRegistry.APIMapping mapping = registry.getMapping("forge-registry-to-fabric");
        assertNotNull(mapping);
        assertNotNull(mapping.translationRule);
    }
    
    @Test
    public void testGetForgeNetworkMapping() {
        APIMapperRegistry.APIMapping mapping = registry.getMapping("forge-network-to-fabric");
        assertNotNull(mapping);
        assertNotNull(mapping.description);
    }
    
    @Test
    public void testGetForgeToBedrockmapping() {
        APIMapperRegistry.APIMapping mapping = registry.getMapping("forge-to-bedrock");
        assertNotNull(mapping);
        assertEquals("net.minecraftforge.eventbus.api.EventBus", mapping.sourceAPI);
    }
    
    @Test
    public void testGetAllMappings() {
        Collection<APIMapperRegistry.APIMapping> mappings = registry.getAllMappings();
        assertNotNull(mappings);
        assertTrue(mappings.size() >= 9);
    }
    
    @Test
    public void testGetUniversalEventBridge() {
        APIMapperRegistry.BridgeTemplate bridge = registry.getBridge("universal-event-bridge");
        assertNotNull(bridge);
        assertEquals("universal-event-bridge", bridge.id);
        assertNotNull(bridge.name);
        assertNotNull(bridge.template);
        assertNotNull(bridge.description);
    }
    
    @Test
    public void testGetUniversalAPIFacade() {
        APIMapperRegistry.BridgeTemplate bridge = registry.getBridge("universal-api-facade");
        assertNotNull(bridge);
        assertEquals("universal-api-facade", bridge.id);
    }
    
    @Test
    public void testGetEntityBridge() {
        APIMapperRegistry.BridgeTemplate bridge = registry.getBridge("entity-bridge");
        assertNotNull(bridge);
        assertEquals("entity-bridge", bridge.id);
    }
    
    @Test
    public void testGetWorldBridge() {
        APIMapperRegistry.BridgeTemplate bridge = registry.getBridge("world-bridge");
        assertNotNull(bridge);
        assertEquals("world-bridge", bridge.id);
    }
    
    @Test
    public void testGetAllBridges() {
        Collection<APIMapperRegistry.BridgeTemplate> bridges = registry.getAllBridges();
        assertNotNull(bridges);
        assertTrue(bridges.size() >= 4);
    }
    
    @Test
    public void testVersionString() {
        String version = APIMapperRegistry.getVersion();
        assertNotNull(version);
        assertEquals("1.0.0", version);
    }
    
    @Test
    public void testMappingProperties() {
        APIMapperRegistry.APIMapping mapping = registry.getMapping("forge-to-fabric-event");
        assertNotNull(mapping.id);
        assertFalse(mapping.id.isEmpty());
        assertNotNull(mapping.description);
        assertFalse(mapping.description.isEmpty());
    }
    
    @Test
    public void testBridgeProperties() {
        APIMapperRegistry.BridgeTemplate bridge = registry.getBridge("universal-event-bridge");
        assertNotNull(bridge.name);
        assertFalse(bridge.name.isEmpty());
        assertTrue(bridge.template.contains("EventBridge"));
    }
}
