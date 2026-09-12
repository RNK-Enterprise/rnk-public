/*
 * RNK Bedrock API Library
 * Copyright (c) 2026 Lisa's Dungeon. All rights reserved.
 * 
 * Complete Bedrock Edition addon API definitions for Bedrock 1.20+
 */

package com.rnk.libraries.bedrock;

import java.util.*;

/**
 * Bedrock API Registry - Complete API definitions for Bedrock addon development
 */
public class BedrockAPIRegistry {
    
    private static final String VERSION = "1.0.0";
    private final Map<String, BedrockModule> modules = new HashMap<>();
    private final Map<String, BedrockComponent> components = new HashMap<>();
    private final Map<String, BedrockEvent> events = new HashMap<>();
    
    public BedrockAPIRegistry() {
        initializeModules();
        initializeComponents();
        initializeEvents();
    }
    
    private void initializeModules() {
        modules.put("@minecraft/server", new BedrockModule(
            "@minecraft/server",
            "Core server-side APIs for world manipulation and entity management"
        ));
        
        modules.put("@minecraft/server-ui", new BedrockModule(
            "@minecraft/server-ui",
            "User interface APIs for forms and dialogs"
        ));
        
        modules.put("@minecraft/server-gametest", new BedrockModule(
            "@minecraft/server-gametest",
            "Game testing framework for automated testing"
        ));
        
        modules.put("@minecraft/server-admin", new BedrockModule(
            "@minecraft/server-admin",
            "Administrative server controls"
        ));
        
        modules.put("@minecraft/vanilla-data", new BedrockModule(
            "@minecraft/vanilla-data",
            "Vanilla game data and constants"
        ));
    }
    
    private void initializeComponents() {
        // Entity components
        components.put("minecraft:health", new BedrockComponent(
            "minecraft:health", "Entity health and damage", "Entity"
        ));
        
        components.put("minecraft:movement", new BedrockComponent(
            "minecraft:movement", "Entity movement properties", "Entity"
        ));
        
        components.put("minecraft:inventory", new BedrockComponent(
            "minecraft:inventory", "Entity inventory container", "Entity"
        ));
        
        components.put("minecraft:position", new BedrockComponent(
            "minecraft:position", "Entity world position", "Entity"
        ));
        
        // Block components
        components.put("minecraft:block_state", new BedrockComponent(
            "minecraft:block_state", "Block state properties", "Block"
        ));
        
        components.put("minecraft:redstone", new BedrockComponent(
            "minecraft:redstone", "Redstone power level", "Block"
        ));
        
        // Player components
        components.put("minecraft:player.input", new BedrockComponent(
            "minecraft:player.input", "Player input handling", "Player"
        ));
        
        components.put("minecraft:player.level", new BedrockComponent(
            "minecraft:player.level", "Player experience level", "Player"
        ));
    }
    
    private void initializeEvents() {
        events.put("minecraft:server_initialize", new BedrockEvent(
            "minecraft:server_initialize",
            "Fired when server initializes",
            "SystemEvent"
        ));
        
        events.put("minecraft:tick", new BedrockEvent(
            "minecraft:tick",
            "Fired on each server tick",
            "SystemEvent"
        ));
        
        events.put("minecraft:player_spawn", new BedrockEvent(
            "minecraft:player_spawn",
            "Fired when player spawns",
            "PlayerEvent"
        ));
        
        events.put("minecraft:player_leave", new BedrockEvent(
            "minecraft:player_leave",
            "Fired when player leaves",
            "PlayerEvent"
        ));
        
        events.put("minecraft:entity_create", new BedrockEvent(
            "minecraft:entity_create",
            "Fired when entity is created",
            "EntityEvent"
        ));
        
        events.put("minecraft:entity_die", new BedrockEvent(
            "minecraft:entity_die",
            "Fired when entity dies",
            "EntityEvent"
        ));
        
        events.put("minecraft:block_place", new BedrockEvent(
            "minecraft:block_place",
            "Fired when block is placed",
            "BlockEvent"
        ));
        
        events.put("minecraft:block_break", new BedrockEvent(
            "minecraft:block_break",
            "Fired when block is broken",
            "BlockEvent"
        ));
    }
    
    public BedrockModule getModule(String moduleName) {
        return modules.get(moduleName);
    }
    
    public BedrockComponent getComponent(String componentId) {
        return components.get(componentId);
    }
    
    public BedrockEvent getEvent(String eventName) {
        return events.get(eventName);
    }
    
    public Collection<BedrockModule> getAllModules() {
        return modules.values();
    }
    
    public Collection<BedrockComponent> getAllComponents() {
        return components.values();
    }
    
    public Collection<BedrockEvent> getAllEvents() {
        return events.values();
    }
    
    public static String getVersion() {
        return VERSION;
    }
    
    // Inner classes
    public static class BedrockModule {
        public final String name;
        public final String description;
        
        public BedrockModule(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }
    
    public static class BedrockComponent {
        public final String id;
        public final String description;
        public final String targetType;
        
        public BedrockComponent(String id, String description, String targetType) {
            this.id = id;
            this.description = description;
            this.targetType = targetType;
        }
    }
    
    public static class BedrockEvent {
        public final String name;
        public final String description;
        public final String category;
        
        public BedrockEvent(String name, String description, String category) {
            this.name = name;
            this.description = description;
            this.category = category;
        }
    }
}
