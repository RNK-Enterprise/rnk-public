/*
 * LD Fix Pattern Library
 * Copyright (c) 2026 Lisa's Dungeon. All rights reserved.
 * 
 * Auto-fix templates for automatic code remediation and modernization
 */

package com.ld.libraries.fixes;

import java.util.*;

/**
 * Fix Pattern Registry - Comprehensive auto-fix templates for code remediation
 */
public class FixPatternRegistry {
    
    private static final String VERSION = "1.0.0";
    private final Map<String, FixTemplate> templates = new HashMap<>();
    
    public FixPatternRegistry() {
        initializeFixTemplates();
    }
    
    private void initializeFixTemplates() {
        // Thread safety fixes
        templates.put("fix-unsafe-collection", new FixTemplate(
            "fix-unsafe-collection",
            "Convert unsafe collection to thread-safe variant",
            "HashMap|HashSet|ArrayList",
            "Collections.synchronizedMap(new HashMap<>())|Collections.synchronizedSet(new HashSet<>())|Collections.synchronizedList(new ArrayList<>())|ConcurrentHashMap|CopyOnWriteArraySet|CopyOnWriteArrayList",
            "Medium",
            true
        ));
        
        // Resource management fixes
        templates.put("fix-unclosed-resource", new FixTemplate(
            "fix-unclosed-resource",
            "Convert to try-with-resources",
            "(?:FileInputStream|FileOutputStream|FileReader|FileWriter|Connection|Statement)\\s+\\w+\\s*=\\s*new",
            "try (TYPE name = new...) {\n  // usage\n}",
            "High",
            true
        ));
        
        // Null safety fixes
        templates.put("fix-null-check", new FixTemplate(
            "fix-null-check",
            "Add null check before dereference",
            "(\\w+)\\.(\\w+)\\(\\)",
            "if ($1 != null) {\n  $1.$2();\n}",
            "Medium",
            false
        ));
        
        // Async task fixes
        templates.put("fix-blocking-operation", new FixTemplate(
            "fix-blocking-operation",
            "Move blocking operations to async task",
            "Thread\\.sleep\\(|wait\\(|join\\(",
            "// Use Bukkit.getScheduler().scheduleAsyncDelayedTask() or equivalent",
            "High",
            false
        ));
        
        // API modernization - Deprecated to modern
        templates.put("fix-deprecated-event", new FixTemplate(
            "fix-deprecated-event",
            "Update deprecated event handlers",
            "@EventHandler.*deprecated",
            "// Update to modern event API",
            "Medium",
            false
        ));
        
        // Security fixes - Credentials
        templates.put("fix-hardcoded-credentials", new FixTemplate(
            "fix-hardcoded-credentials",
            "Move credentials to configuration",
            "(?:password|apiKey|secret|token)\\s*=\\s*[\"']\\w+[\"']",
            "String credential = config.getString(\"key-name\");",
            "Critical",
            true
        ));
        
        // Security fixes - Command injection
        templates.put("fix-command-injection", new FixTemplate(
            "fix-command-injection",
            "Use safe command execution",
            "Runtime\\.exec\\(|new ProcessBuilder\\(",
            "ProcessBuilder pb = new ProcessBuilder(Arrays.asList(command.split(\" \")));\npb.start();",
            "Critical",
            true
        ));
        
        // NMS compatibility fixes
        templates.put("fix-nms-version-check", new FixTemplate(
            "fix-nms-version-check",
            "Add NMS version abstraction",
            "net\\.minecraft\\.server\\.v\\d+",
            "// Use reflection or version-specific wrappers",
            "High",
            true
        ));
        
        // Import cleanup
        templates.put("fix-unused-imports", new FixTemplate(
            "fix-unused-imports",
            "Remove unused imports",
            "import.*(?!\\w+\\.).*",
            "// Remove unused import",
            "Low",
            true
        ));
        
        // Code modernization - Lambda expressions
        templates.put("fix-anonymous-to-lambda", new FixTemplate(
            "fix-anonymous-to-lambda",
            "Convert anonymous class to lambda expression",
            "new (Runnable|Callable|EventHandler)\\(\\)\\s*\\{",
            "() -> { or replace with lambda expression",
            "Low",
            false
        ));
    }
    
    public FixTemplate getTemplate(String templateId) {
        return templates.get(templateId);
    }
    
    public Collection<FixTemplate> getTemplatesBySeverity(String severity) {
        return templates.values().stream()
            .filter(t -> t.severity.equals(severity))
            .toList();
    }
    
    public Collection<FixTemplate> getAllTemplates() {
        return templates.values();
    }
    
    public boolean canAutoFix(String templateId) {
        FixTemplate template = getTemplate(templateId);
        return template != null && template.isAutomatic;
    }
    
    public static String getVersion() {
        return VERSION;
    }
    
    // Inner class
    public static class FixTemplate {
        public final String id;
        public final String name;
        public final String searchPattern;
        public final String replacementPattern;
        public final String severity;
        public final boolean isAutomatic;
        public final long timestamp;
        
        public FixTemplate(String id, String name, String searchPattern, String replacementPattern, 
                          String severity, boolean isAutomatic) {
            this.id = id;
            this.name = name;
            this.searchPattern = searchPattern;
            this.replacementPattern = replacementPattern;
            this.severity = severity;
            this.isAutomatic = isAutomatic;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
