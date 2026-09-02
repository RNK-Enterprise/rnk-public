package com.ld.thetync.core;

import java.util.*;

/**
 * Realistic Minecraft mod profiles for training data generation.
 * Contains hardcoded, deterministic mod characteristics based on actual Minecraft ecosystem.
 */
public class MinecraftModProfiles {

    public enum ModCategory {
        GRAPHICS,
        UTILITY,
        CONTENT,
        SERVER,
        OPTIMIZATION,
        GAMEPLAY
    }

    public static class ModCharacteristics {
        public int minSize;
        public int maxSize;
        public int minDependencies;
        public int maxDependencies;
        public double memoryUsageLow;
        public double memoryUsageHigh;
        public double shaderProbability;
        public double textureProbability;
        public double eventHeavyProbability;
        public int minClasses;
        public int maxClasses;

        ModCharacteristics(int minSize, int maxSize, int minDeps, int maxDeps,
                          double memLow, double memHigh, double shaders, double textures,
                          double events, int minClasses, int maxClasses) {
            this.minSize = minSize;
            this.maxSize = maxSize;
            this.minDependencies = minDeps;
            this.maxDependencies = maxDeps;
            this.memoryUsageLow = memLow;
            this.memoryUsageHigh = memHigh;
            this.shaderProbability = shaders;
            this.textureProbability = textures;
            this.eventHeavyProbability = events;
            this.minClasses = minClasses;
            this.maxClasses = maxClasses;
        }
    }

    private static final Map<ModCategory, ModCharacteristics> CATEGORY_PROFILES = new LinkedHashMap<>();

    static {
        // Graphics Mods: 8-50MB, high texture/shader, heavy memory
        CATEGORY_PROFILES.put(ModCategory.GRAPHICS,
            new ModCharacteristics(8_000_000, 50_000_000, 5, 20, 0.6, 0.95, 0.85, 0.90, 0.4, 50, 200));

        // Utility Mods: 0.5-5MB, minimal deps, low memory
        CATEGORY_PROFILES.put(ModCategory.UTILITY,
            new ModCharacteristics(500_000, 5_000_000, 1, 5, 0.05, 0.3, 0.05, 0.1, 0.2, 10, 50));

        // Content Mods: 3-20MB, moderate deps, variable memory
        CATEGORY_PROFILES.put(ModCategory.CONTENT,
            new ModCharacteristics(3_000_000, 20_000_000, 3, 12, 0.2, 0.6, 0.2, 0.4, 0.5, 30, 150));

        // Server Mods: 1-10MB, config-heavy, low memory, event-heavy
        CATEGORY_PROFILES.put(ModCategory.SERVER,
            new ModCharacteristics(1_000_000, 10_000_000, 2, 8, 0.1, 0.4, 0.0, 0.05, 0.8, 20, 80));

        // Optimization Mods: 2-8MB, framework deps, custom tuning
        CATEGORY_PROFILES.put(ModCategory.OPTIMIZATION,
            new ModCharacteristics(2_000_000, 8_000_000, 3, 10, 0.15, 0.5, 0.1, 0.05, 0.3, 25, 100));

        // Gameplay Mods: 2-15MB, event-heavy, moderate complexity
        CATEGORY_PROFILES.put(ModCategory.GAMEPLAY,
            new ModCharacteristics(2_000_000, 15_000_000, 2, 10, 0.25, 0.65, 0.15, 0.2, 0.7, 25, 120));
    }

    /**
     * Get realistic characteristics for a mod category.
     */
    public static ModCharacteristics getCharacteristics(ModCategory category) {
        return CATEGORY_PROFILES.get(category);
    }

    /**
     * Get all categories.
     */
    public static ModCategory[] getAllCategories() {
        return ModCategory.values();
    }

    /**
     * Generate deterministic mod profile based on category and seed.
     */
    public static ModProfile generateProfile(ModCategory category, int seed) {
        ModCharacteristics chars = getCharacteristics(category);
        Random rand = new Random(seed); // Fixed seed for deterministic output

        ModProfile profile = new ModProfile();
        profile.category = category;
        profile.modId = String.format("mod_%s_%d", category.name().toLowerCase(), seed);

        // Deterministic size based on seed and category
        profile.sizeBytes = chars.minSize + rand.nextInt(chars.maxSize - chars.minSize);

        // Deterministic dependencies
        profile.dependencyCount = chars.minDependencies + rand.nextInt(chars.maxDependencies - chars.minDependencies);

        // Deterministic memory usage
        profile.memoryUsage = chars.memoryUsageLow + rand.nextDouble() * (chars.memoryUsageHigh - chars.memoryUsageLow);

        // Deterministic resource probabilities
        profile.hasShaders = rand.nextDouble() < chars.shaderProbability;
        profile.hasTextures = rand.nextDouble() < chars.textureProbability;
        profile.isEventHeavy = rand.nextDouble() < chars.eventHeavyProbability;

        // Deterministic class/method counts
        profile.classCount = chars.minClasses + rand.nextInt(chars.maxClasses - chars.minClasses);
        profile.methodCount = profile.classCount * (5 + rand.nextInt(15)); // 5-20 methods per class

        return profile;
    }

    /**
     * Realistic mod profile data class.
     */
    public static class ModProfile {
        public String modId;
        public ModCategory category;
        public int sizeBytes;
        public int dependencyCount;
        public double memoryUsage;  // 0.0 to 1.0
        public boolean hasShaders;
        public boolean hasTextures;
        public boolean isEventHeavy;
        public int classCount;
        public int methodCount;

        @Override
        public String toString() {
            return String.format("%s (%s): %dMB, %d deps, %.2f mem, shaders=%b, textures=%b, events=%b",
                modId, category, sizeBytes / 1_000_000, dependencyCount, memoryUsage, hasShaders, hasTextures, isEventHeavy);
        }
    }

    /**
     * Get optimal transformation strategy for a mod profile on a specific loader.
     * This defines the 100% accurate label for training.
     */
    public static double[] getOptimalTransformation(ModProfile profile, String loaderName) {
        double[] strategy = new double[10];

        // Strategy index reference:
        // 0: Type checking priority (0-1)
        // 1: Module conversion (0-1)
        // 2: API compatibility (0-1)
        // 3: Bridge generation (0-1)
        // 4: Error recovery (0-1)
        // 5: Code optimization (0-1)
        // 6: Security enhancement (0-1)
        // 7: Resource management (0-1)
        // 8: Scalability optimization (0-1)
        // 9: Advanced techniques (0-1)

        // Base strategy from mod profile characteristics
        double graphicsIntensity = (profile.hasShaders ? 0.5 : 0) + (profile.hasTextures ? 0.5 : 0);
        double complexityScore = Math.min(1.0, profile.dependencyCount / 20.0);
        double memoryPressure = profile.memoryUsage;

        // Universal strategies
        strategy[0] = 0.7; // Type checking (always important)
        strategy[2] = 0.8; // API compatibility (critical)

        // Loader-specific adaptations
        switch (loaderName.toLowerCase()) {
            case "forge":
                strategy[1] = 0.6; // Module conversion
                strategy[3] = 0.8; // Bridge for ForgeRegistry
                strategy[4] = 0.7; // Error recovery (events can fail)
                strategy[5] = 0.6; // Optimization
                strategy[7] = 0.7; // Resource management
                break;

            case "fabric":
                strategy[1] = 0.75; // Module conversion (Mixin-based)
                strategy[3] = 0.6; // Less bridge needed
                strategy[4] = 0.8; // Error recovery (Mixins can be fragile)
                strategy[5] = 0.8; // Optimization (Fabric is performance-focused)
                strategy[7] = 0.8; // Resource management
                break;

            case "paper":
            case "spigot":
                strategy[1] = 0.5; // Less module conversion
                strategy[3] = 0.7; // Plugin.yml bridge
                strategy[4] = 0.9; // Event system requires robust error handling
                strategy[5] = 0.5; // Server-side optimization
                strategy[6] = 0.8; // Security (server context)
                break;

            case "bedrock":
                strategy[1] = 0.85; // Major conversion needed
                strategy[3] = 0.9; // Bridge for C++ interactions
                strategy[4] = 0.8; // Cross-platform compatibility issues
                strategy[5] = 0.7; // Performance important
                break;

            case "quilt":
                strategy[1] = 0.7; // Quilt modules
                strategy[3] = 0.6; // Quilted fabric compatibility
                strategy[4] = 0.8; // Error recovery
                strategy[5] = 0.8; // Optimization
                break;

            case "sponge":
                strategy[1] = 0.65; // Plugin to Sponge
                strategy[3] = 0.75; // Service providers
                strategy[4] = 0.85; // Event system
                strategy[6] = 0.8; // Security
                strategy[7] = 0.8; // Resource management
                break;

            default:
                strategy[1] = 0.6;
                strategy[3] = 0.7;
                strategy[4] = 0.75;
                strategy[5] = 0.65;
        }

        // Adjust for mod characteristics
        // Graphics-intensive mods need optimization
        strategy[5] += 0.2 * graphicsIntensity;

        // Complex mods need better error recovery
        strategy[4] += 0.15 * complexityScore;

        // High memory mods need resource management
        strategy[7] += 0.2 * memoryPressure;

        // Event-heavy mods need error recovery
        if (profile.isEventHeavy) {
            strategy[4] += 0.15;
        }

        // Large mods benefit from lazy loading and advanced techniques
        double sizeScore = Math.min(1.0, profile.sizeBytes / 50_000_000.0);
        strategy[8] += 0.2 * sizeScore; // Scalability
        strategy[9] += 0.15 * sizeScore; // Advanced techniques

        // Normalize to [0, 1] range
        for (int i = 0; i < strategy.length; i++) {
            strategy[i] = Math.min(1.0, Math.max(0.0, strategy[i]));
        }

        return strategy;
    }
}
