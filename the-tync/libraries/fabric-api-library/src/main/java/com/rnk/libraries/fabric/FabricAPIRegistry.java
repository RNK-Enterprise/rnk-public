/*
 * RNK Fabric API Library
 * Copyright (c) 2026 Lisa's Dungeon. All rights reserved.
 * 
 * Complete Fabric mod API definitions for versions 1.14 through 1.21.x
 */

package com.rnk.libraries.fabric;

import java.util.*;

/**
 * Fabric API Registry - Complete API definitions for Fabric modding framework
 */
public class FabricAPIRegistry {
    
    private static final String VERSION = "1.0.0";
    private final Map<String, FabricClass> classDefinitions = new HashMap<>();
    private final Map<String, FabricEvent> eventDefinitions = new HashMap<>();
    private final Map<String, FabricModule> modules = new HashMap<>();
    
    public FabricAPIRegistry() {
        initializeFabricClasses();
        initializeFabricEvents();
        initializeFabricModules();
    }
    
    private void initializeFabricClasses() {
        // Mod Initializers
        classDefinitions.put("net.fabricmc.api.ModInitializer", new FabricClass(
            "ModInitializer", "Main mod entry point - runs on both client and server", true
        ));
        
        classDefinitions.put("net.fabricmc.api.ClientModInitializer", new FabricClass(
            "ClientModInitializer", "Client-side mod entry point", true
        ));
        
        classDefinitions.put("net.fabricmc.api.DedicatedServerModInitializer", new FabricClass(
            "DedicatedServerModInitializer", "Dedicated server-side mod entry point", true
        ));
        
        // Event callbacks
        classDefinitions.put("net.fabric.api.event.lifecycle.v1.ServerLifecycleEvents", new FabricClass(
            "ServerLifecycleEvents", "Server lifecycle event callbacks", false
        ));
        
        classDefinitions.put("net.fabric.api.event.lifecycle.v1.ServerTickEvents", new FabricClass(
            "ServerTickEvents", "Server tick event callbacks", false
        ));
        
        // World events
        classDefinitions.put("net.fabric.api.event.world.WorldTickEvents", new FabricClass(
            "WorldTickEvents", "World tick event callbacks", false
        ));
        
        // Entity events
        classDefinitions.put("net.fabric.api.event.entity.EntitySelectorEvents", new FabricClass(
            "EntitySelectorEvents", "Entity selector event callbacks", false
        ));
        
        // Network API
        classDefinitions.put("net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking", new FabricClass(
            "ServerPlayNetworking", "Server-side networking utilities", false
        ));
        
        classDefinitions.put("net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking", new FabricClass(
            "ClientPlayNetworking", "Client-side networking utilities", false
        ));
        
        // Command API
        classDefinitions.put("net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback", new FabricClass(
            "CommandRegistrationCallback", "Command registration callback", true
        ));
    }
    
    private void initializeFabricEvents() {
        eventDefinitions.put("net.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarting", 
            new FabricEvent("ServerStarting", "Fired when server is starting"));
        
        eventDefinitions.put("net.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopping", 
            new FabricEvent("ServerStopping", "Fired when server is stopping"));
        
        eventDefinitions.put("net.fabric.api.event.lifecycle.v1.ServerTickEvents.StartTick", 
            new FabricEvent("StartTick", "Fired at start of server tick"));
        
        eventDefinitions.put("net.fabric.api.event.lifecycle.v1.ServerTickEvents.EndTick", 
            new FabricEvent("EndTick", "Fired at end of server tick"));
        
        eventDefinitions.put("net.fabric.api.event.world.WorldTickEvents.StartTick", 
            new FabricEvent("WorldStartTick", "Fired at start of world tick"));
        
        eventDefinitions.put("net.fabric.api.event.world.WorldTickEvents.EndTick", 
            new FabricEvent("WorldEndTick", "Fired at end of world tick"));
    }
    
    private void initializeFabricModules() {
        modules.put("Lifecycle Events v1", new FabricModule(
            "net.fabric.api.event.lifecycle.v1", 
            "Server and client lifecycle events"
        ));
        
        modules.put("Networking v1", new FabricModule(
            "net.fabricmc.fabric.api.networking.v1", 
            "Network synchronization and packet handling"
        ));
        
        modules.put("Command v2", new FabricModule(
            "net.fabricmc.fabric.api.command.v2", 
            "Command registration and execution"
        ));
        
        modules.put("World API v1", new FabricModule(
            "net.fabric.api.event.world.v1", 
            "World events and interactions"
        ));
        
        modules.put("Entity API v1", new FabricModule(
            "net.fabric.api.event.entity.v1", 
            "Entity events and interactions"
        ));
    }
    
    public FabricClass getClass(String qualifiedName) {
        return classDefinitions.get(qualifiedName);
    }
    
    public FabricEvent getEvent(String qualifiedName) {
        return eventDefinitions.get(qualifiedName);
    }
    
    public FabricModule getModule(String name) {
        return modules.get(name);
    }
    
    public Collection<FabricClass> getAllClasses() {
        return classDefinitions.values();
    }
    
    public Collection<FabricEvent> getAllEvents() {
        return eventDefinitions.values();
    }
    
    public Collection<FabricModule> getAllModules() {
        return modules.values();
    }
    
    public static String getVersion() {
        return VERSION;
    }
    
    // Inner classes
    public static class FabricClass {
        public final String name;
        public final String description;
        public final boolean isInterface;
        
        public FabricClass(String name, String description, boolean isInterface) {
            this.name = name;
            this.description = description;
            this.isInterface = isInterface;
        }
    }
    
    public static class FabricEvent {
        public final String name;
        public final String description;
        
        public FabricEvent(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }
    
    public static class FabricModule {
        public final String packageName;
        public final String description;
        
        public FabricModule(String packageName, String description) {
            this.packageName = packageName;
            this.description = description;
        }
    }
}
