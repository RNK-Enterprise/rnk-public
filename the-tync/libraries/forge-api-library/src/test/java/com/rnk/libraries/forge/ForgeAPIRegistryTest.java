/*
 * RNK Forge API Library - Test Suite
 * Copyright (c) 2026 RNK Studios. Licensed under GPL-3.0-only.
 */

package com.rnk.libraries.forge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.Collection;
import static org.junit.jupiter.api.Assertions.*;

public class ForgeAPIRegistryTest {
    
    private ForgeAPIRegistry registry;
    
    @BeforeEach
    public void setUp() {
        registry = new ForgeAPIRegistry();
    }
    
    @Test
    public void testRegistryInitialization() {
        assertNotNull(registry);
    }
    
    @Test
    public void testGetForgeClass() {
        ForgeAPIRegistry.ForgeClass eventBus = registry.getClass("net.minecraftforge.eventbus.api.EventBus");
        assertNotNull(eventBus);
        assertEquals("EventBus", eventBus.name);
        assertTrue(eventBus.isInterface);
        assertNotNull(eventBus.methods);
        assertTrue(eventBus.methods.length > 0);
    }
    
    @Test
    public void testGetForgeEvent() {
        ForgeAPIRegistry.ForgeEvent event = registry.getEvent("net.minecraftforge.event.server.ServerStartingEvent");
        assertNotNull(event);
        assertEquals("ServerStartingEvent", event.name);
        assertEquals("ServerEvent", event.parent);
    }
    
    @Test
    public void testGetAllClasses() {
        Collection<ForgeAPIRegistry.ForgeClass> classes = registry.getAllClasses();
        assertNotNull(classes);
        assertTrue(classes.size() > 0);
        assertTrue(classes.size() >= 6);
    }
    
    @Test
    public void testGetAllEvents() {
        Collection<ForgeAPIRegistry.ForgeEvent> events = registry.getAllEvents();
        assertNotNull(events);
        assertTrue(events.size() > 0);
        assertTrue(events.size() >= 7);
    }
    
    @Test
    public void testVersionString() {
        String version = ForgeAPIRegistry.getVersion();
        assertNotNull(version);
        assertEquals("1.0.0", version);
    }
    
    @Test
    public void testRegistryRegistry() {
        ForgeAPIRegistry.ForgeClass registry = this.registry.getClass("net.minecraftforge.registries.IForgeRegistry");
        assertNotNull(registry);
        assertEquals("IForgeRegistry", registry.name);
        assertTrue(registry.isInterface);
    }
    
    @Test
    public void testBlockAPI() {
        ForgeAPIRegistry.ForgeClass block = registry.getClass("net.minecraftforge.common.extensions.IForgeBlock");
        assertNotNull(block);
        assertEquals("IForgeBlock", block.name);
    }
    
    @Test
    public void testEntityAPI() {
        ForgeAPIRegistry.ForgeClass entity = registry.getClass("net.minecraftforge.common.extensions.IForgeEntity");
        assertNotNull(entity);
        assertEquals("IForgeEntity", entity.name);
    }
    
    @Test
    public void testItemAPI() {
        ForgeAPIRegistry.ForgeClass item = registry.getClass("net.minecraftforge.common.extensions.IForgeItem");
        assertNotNull(item);
        assertEquals("IForgeItem", item.name);
    }
    
    @Test
    public void testEventHierarchy() {
        ForgeAPIRegistry.ForgeEvent entityEvent = registry.getEvent("net.minecraftforge.event.entity.EntityEvent");
        assertNotNull(entityEvent);
        assertEquals("Event", entityEvent.parent);
    }
    
    @Test
    public void testLivingEventHierarchy() {
        ForgeAPIRegistry.ForgeEvent livingEvent = registry.getEvent("net.minecraftforge.event.entity.living.LivingEvent");
        assertNotNull(livingEvent);
        assertEquals("EntityEvent", livingEvent.parent);
    }
    
    @Test
    public void testClassProperties() {
        ForgeAPIRegistry.ForgeClass eventBus = registry.getClass("net.minecraftforge.eventbus.api.EventBus");
        assertNotNull(eventBus.description);
        assertFalse(eventBus.description.isEmpty());
        assertNotNull(eventBus.methods);
    }
    
    @Test
    public void testEventProperties() {
        ForgeAPIRegistry.ForgeEvent event = registry.getEvent("net.minecraftforge.event.server.ServerStartingEvent");
        assertNotNull(event.description);
        assertFalse(event.description.isEmpty());
    }

    @Test
    public void testGetInterface() {
        ForgeAPIRegistry.ForgeInterface capability =
            registry.getInterface("net.minecraftforge.common.capabilities.ICapabilitySerializable");
        assertNotNull(capability);
        assertEquals("ICapabilitySerializable", capability.name);
        assertFalse(capability.description.isEmpty());
    }

    @Test
    public void testGetInterfaceUnknownReturnsNull() {
        assertNull(registry.getInterface("net.minecraftforge.unknown.UnknownInterface"));
    }
    }
