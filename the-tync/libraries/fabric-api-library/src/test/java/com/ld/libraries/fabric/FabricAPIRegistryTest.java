/*
 * LD Fabric API Library - Test Suite
 * Copyright (c) 2026 Lisa's Dungeon. All rights reserved.
 */

package com.ld.libraries.fabric;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.Collection;
import static org.junit.jupiter.api.Assertions.*;

public class FabricAPIRegistryTest {
    
    private FabricAPIRegistry registry;
    
    @BeforeEach
    public void setUp() {
        registry = new FabricAPIRegistry();
    }
    
    @Test
    public void testRegistryInitialization() {
        assertNotNull(registry);
    }
    
    @Test
    public void testGetModInitializer() {
        FabricAPIRegistry.FabricClass modInit = registry.getClass("net.fabricmc.api.ModInitializer");
        assertNotNull(modInit);
        assertEquals("ModInitializer", modInit.name);
        assertTrue(modInit.isInterface);
    }
    
    @Test
    public void testGetClientModInitializer() {
        FabricAPIRegistry.FabricClass clientInit = registry.getClass("net.fabricmc.api.ClientModInitializer");
        assertNotNull(clientInit);
        assertEquals("ClientModInitializer", clientInit.name);
        assertTrue(clientInit.isInterface);
    }
    
    @Test
    public void testGetServerModInitializer() {
        FabricAPIRegistry.FabricClass serverInit = registry.getClass("net.fabricmc.api.DedicatedServerModInitializer");
        assertNotNull(serverInit);
        assertEquals("DedicatedServerModInitializer", serverInit.name);
        assertTrue(serverInit.isInterface);
    }
    
    @Test
    public void testGetLifecycleEvents() {
        FabricAPIRegistry.FabricClass lifecycle = registry.getClass("net.fabric.api.event.lifecycle.v1.ServerLifecycleEvents");
        assertNotNull(lifecycle);
        assertEquals("ServerLifecycleEvents", lifecycle.name);
        assertFalse(lifecycle.isInterface);
    }
    
    @Test
    public void testGetServerTickEvents() {
        FabricAPIRegistry.FabricClass tickEvents = registry.getClass("net.fabric.api.event.lifecycle.v1.ServerTickEvents");
        assertNotNull(tickEvents);
        assertEquals("ServerTickEvents", tickEvents.name);
    }
    
    @Test
    public void testGetNetworkingAPI() {
        FabricAPIRegistry.FabricClass networking = registry.getClass("net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking");
        assertNotNull(networking);
        assertEquals("ServerPlayNetworking", networking.name);
    }
    
    @Test
    public void testGetCommandRegistration() {
        FabricAPIRegistry.FabricClass cmdReg = registry.getClass("net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback");
        assertNotNull(cmdReg);
        assertEquals("CommandRegistrationCallback", cmdReg.name);
        assertTrue(cmdReg.isInterface);
    }
    
    @Test
    public void testGetEvent() {
        FabricAPIRegistry.FabricEvent event = registry.getEvent("net.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarting");
        assertNotNull(event);
        assertEquals("ServerStarting", event.name);
    }
    
    @Test
    public void testGetAllClasses() {
        Collection<FabricAPIRegistry.FabricClass> classes = registry.getAllClasses();
        assertNotNull(classes);
        assertTrue(classes.size() > 0);
        assertTrue(classes.size() >= 10);
    }
    
    @Test
    public void testGetAllEvents() {
        Collection<FabricAPIRegistry.FabricEvent> events = registry.getAllEvents();
        assertNotNull(events);
        assertTrue(events.size() > 0);
        assertTrue(events.size() >= 6);
    }
    
    @Test
    public void testGetAllModules() {
        Collection<FabricAPIRegistry.FabricModule> modules = registry.getAllModules();
        assertNotNull(modules);
        assertTrue(modules.size() > 0);
        assertTrue(modules.size() >= 5);
    }
    
    @Test
    public void testGetModule() {
        FabricAPIRegistry.FabricModule module = registry.getModule("Lifecycle Events v1");
        assertNotNull(module);
        assertEquals("net.fabric.api.event.lifecycle.v1", module.packageName);
    }
    
    @Test
    public void testVersionString() {
        String version = FabricAPIRegistry.getVersion();
        assertNotNull(version);
        assertEquals("1.0.0", version);
    }
    
    @Test
    public void testModuleDescriptions() {
        FabricAPIRegistry.FabricModule module = registry.getModule("Networking v1");
        assertNotNull(module);
        assertNotNull(module.description);
        assertFalse(module.description.isEmpty());
    }
}
