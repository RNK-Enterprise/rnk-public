/*
 * RNK Issue Pattern Library - Test Suite
 * Copyright (c) 2026 Lisa's Dungeon. All rights reserved.
 */

package com.rnk.libraries.patterns;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.Collection;
import static org.junit.jupiter.api.Assertions.*;

public class IssuePatternRegistryTest {
    
    private IssuePatternRegistry registry;
    
    @BeforeEach
    public void setUp() {
        registry = new IssuePatternRegistry();
    }
    
    @Test
    public void testRegistryInitialization() {
        assertNotNull(registry);
    }
    
    @Test
    public void testGetDeprecationPattern() {
        IssuePatternRegistry.IssuePattern pattern = registry.getPattern("deprecated-api-usage");
        assertNotNull(pattern);
        assertEquals("deprecated-api-usage", pattern.id);
        assertEquals("MEDIUM", pattern.severity);
        assertNotNull(pattern.pattern);
        assertNotNull(pattern.remediation);
    }
    
    @Test
    public void testGetNMSVersionPattern() {
        IssuePatternRegistry.IssuePattern pattern = registry.getPattern("nms-version-mismatch");
        assertNotNull(pattern);
        assertEquals("nms-version-mismatch", pattern.id);
        assertEquals("HIGH", pattern.severity);
    }
    
    @Test
    public void testGetThreadSafetyPattern() {
        IssuePatternRegistry.IssuePattern pattern = registry.getPattern("unsafe-concurrent-access");
        assertNotNull(pattern);
        assertEquals("unsafe-concurrent-access", pattern.id);
        assertEquals("HIGH", pattern.severity);
    }
    
    @Test
    public void testGetBlockingOperationPattern() {
        IssuePatternRegistry.IssuePattern pattern = registry.getPattern("blocking-event-handler");
        assertNotNull(pattern);
        assertEquals("blocking-event-handler", pattern.id);
        assertEquals("HIGH", pattern.severity);
    }
    
    @Test
    public void testGetResourceLeakPattern() {
        IssuePatternRegistry.IssuePattern pattern = registry.getPattern("unclosed-resource");
        assertNotNull(pattern);
        assertEquals("unclosed-resource", pattern.id);
        assertEquals("MEDIUM", pattern.severity);
    }
    
    @Test
    public void testGetNullPointerPattern() {
        IssuePatternRegistry.IssuePattern pattern = registry.getPattern("null-pointer-risk");
        assertNotNull(pattern);
        assertEquals("null-pointer-risk", pattern.id);
        assertEquals("MEDIUM", pattern.severity);
    }
    
    @Test
    public void testGetCommandInjectionPattern() {
        IssuePatternRegistry.IssuePattern pattern = registry.getPattern("command-injection");
        assertNotNull(pattern);
        assertEquals("command-injection", pattern.id);
        assertEquals("CRITICAL", pattern.severity);
    }
    
    @Test
    public void testGetDeserializationPattern() {
        IssuePatternRegistry.IssuePattern pattern = registry.getPattern("unsafe-deserialization");
        assertNotNull(pattern);
        assertEquals("unsafe-deserialization", pattern.id);
        assertEquals("CRITICAL", pattern.severity);
    }
    
    @Test
    public void testGetRegexDosPattern() {
        IssuePatternRegistry.IssuePattern pattern = registry.getPattern("regex-dos");
        assertNotNull(pattern);
        assertEquals("regex-dos", pattern.id);
        assertEquals("MEDIUM", pattern.severity);
    }
    
    @Test
    public void testGetHardcodedCredentialsPattern() {
        IssuePatternRegistry.IssuePattern pattern = registry.getPattern("hardcoded-credentials");
        assertNotNull(pattern);
        assertEquals("hardcoded-credentials", pattern.id);
        assertEquals("CRITICAL", pattern.severity);
    }
    
    @Test
    public void testGetAllPatterns() {
        Collection<IssuePatternRegistry.IssuePattern> patterns = registry.getAllPatterns();
        assertNotNull(patterns);
        assertTrue(patterns.size() >= 10);
    }
    
    @Test
    public void testGetPatternsBySeverity() {
        Collection<IssuePatternRegistry.IssuePattern> criticalPatterns = registry.getPatternsBySeverity("CRITICAL");
        assertNotNull(criticalPatterns);
        assertTrue(criticalPatterns.size() > 0);
    }
    
    @Test
    public void testPatternMatching() {
        IssuePatternRegistry.IssuePattern pattern = registry.getPattern("deprecated-api-usage");
        boolean matchesDeprecated = pattern.matches("public void oldMethod() { } // deprecated");
        assertTrue(matchesDeprecated);
    }
    
    @Test
    public void testVersionString() {
        String version = IssuePatternRegistry.getVersion();
        assertNotNull(version);
        assertEquals("1.0.0", version);
    }
    
    @Test
    public void testPatternProperties() {
        IssuePatternRegistry.IssuePattern pattern = registry.getPattern("hardcoded-credentials");
        assertNotNull(pattern.name);
        assertFalse(pattern.name.isEmpty());
        assertNotNull(pattern.remediation);
        assertFalse(pattern.remediation.isEmpty());
        assertTrue(pattern.timestamp > 0);
    }

    @Test
    public void testMatchesWithInvalidPatternReturnsFalse() {
        IssuePatternRegistry.IssuePattern pattern = new IssuePatternRegistry.IssuePattern(
            "test-invalid-regex", "Invalid Regex", "LOW", "[invalid(regex", "no-op");
        assertFalse(pattern.matches("any code"));
    }

    @Test
    public void testMatchesWithNullCodeReturnsFalse() {
        IssuePatternRegistry.IssuePattern pattern = new IssuePatternRegistry.IssuePattern(
            "test-null-code", "Null Code", "LOW", "synchronized", "no-op");
        assertFalse(pattern.matches(null));
    }
    }
