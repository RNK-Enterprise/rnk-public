/*
 * RNK Issue Pattern Library
 * Copyright (c) 2026 Lisa's Dungeon. All rights reserved.
 * 
 * Bug detection patterns for mod code analysis and security vulnerability detection
 */

package com.rnk.libraries.patterns;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Issue Pattern Registry - Comprehensive bug detection patterns for mod analysis
 */
public class IssuePatternRegistry {
    
    private static final String VERSION = "1.0.0";
    private final Map<String, IssuePattern> patterns = new HashMap<>();
    
    public IssuePatternRegistry() {
        initializePatterns();
    }
    
    private void initializePatterns() {
        // Deprecation patterns
        patterns.put("deprecated-api-usage", new IssuePattern(
            "deprecated-api-usage",
            "Usage of deprecated APIs",
            "MEDIUM",
            "\\b(deprecated|@Deprecated)\\b",
            "Update code to use modern APIs"
        ));
        
        // NMS version compatibility issues
        patterns.put("nms-version-mismatch", new IssuePattern(
            "nms-version-mismatch",
            "NMS version-specific code without abstraction",
            "HIGH",
            "net\\.minecraft\\.server\\.v\\d+",
            "Wrap NMS code in version abstraction layer"
        ));
        
        // Thread safety issues
        patterns.put("unsafe-concurrent-access", new IssuePattern(
            "unsafe-concurrent-access",
            "Non-thread-safe access to shared resources",
            "HIGH",
            "\\b(static\\s+)?(List|Map|Set|HashSet|HashMap)\\s+\\w+\\s*=",
            "Use thread-safe collections like ConcurrentHashMap"
        ));
        
        // Event handler issues
        patterns.put("blocking-event-handler", new IssuePattern(
            "blocking-event-handler",
            "Blocking operations in event handlers",
            "HIGH",
            "\\b(Thread\\.sleep|wait|join)\\b",
            "Use async tasks for blocking operations"
        ));
        
        // Resource leak patterns
        patterns.put("unclosed-resource", new IssuePattern(
            "unclosed-resource",
            "Unclosed file, stream, or database resource",
            "MEDIUM",
            "new\\s+(FileInputStream|FileOutputStream|FileReader|FileWriter|Connection|Statement)",
            "Use try-with-resources or ensure proper cleanup"
        ));
        
        // Null pointer dereference
        patterns.put("null-pointer-risk", new IssuePattern(
            "null-pointer-risk",
            "Potential null pointer dereference",
            "MEDIUM",
            "\\.\\w+\\(\\)\\s*(?!instanceof|==|!=|\\?)(?=[\\w\\[\\.])",
            "Add null checks before dereferencing"
        ));
        
        // Command injection vulnerability
        patterns.put("command-injection", new IssuePattern(
            "command-injection",
            "Potential command injection vulnerability",
            "CRITICAL",
            "\\b(Runtime\\.exec|ProcessBuilder)\\s*\\(",
            "Validate and sanitize command inputs"
        ));
        
        // Serialization vulnerability
        patterns.put("unsafe-deserialization", new IssuePattern(
            "unsafe-deserialization",
            "Unsafe deserialization of untrusted data",
            "CRITICAL",
            "\\b(ObjectInputStream|readObject|XMLDecoder)\\b",
            "Use safe serialization methods or validate input"
        ));
        
        // Regex DOS vulnerability
        patterns.put("regex-dos", new IssuePattern(
            "regex-dos",
            "Potentially vulnerable regex pattern (ReDoS)",
            "MEDIUM",
            "\\(.*\\*\\+.*\\).*\\(.*\\*\\+.*\\)",
            "Optimize regex or add timeout controls"
        ));
        
        // Hard-coded credentials
        patterns.put("hardcoded-credentials", new IssuePattern(
            "hardcoded-credentials",
            "Hard-coded credentials or API keys",
            "CRITICAL",
            "\\b(password|apiKey|secret|token)\\s*=\\s*[\"']\\w+[\"']",
            "Use configuration files or environment variables"
        ));
    }
    
    public IssuePattern getPattern(String patternId) {
        return patterns.get(patternId);
    }
    
    public Collection<IssuePattern> getPatternsBySeverity(String severity) {
        return patterns.values().stream()
            .filter(p -> p.severity.equals(severity))
            .toList();
    }
    
    public Collection<IssuePattern> getAllPatterns() {
        return patterns.values();
    }
    
    public static String getVersion() {
        return VERSION;
    }
    
    // Inner class
    public static class IssuePattern {
        public final String id;
        public final String name;
        public final String severity; // CRITICAL, HIGH, MEDIUM, LOW
        public final String pattern;
        public final String remediation;
        public final long timestamp;
        
        public IssuePattern(String id, String name, String severity, String pattern, String remediation) {
            this.id = id;
            this.name = name;
            this.severity = severity;
            this.pattern = pattern;
            this.remediation = remediation;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean matches(String code) {
            try {
                return Pattern.compile(pattern, Pattern.MULTILINE | Pattern.DOTALL).matcher(code).find();
            } catch (Exception e) {
                return false;
            }
        }
    }
}
