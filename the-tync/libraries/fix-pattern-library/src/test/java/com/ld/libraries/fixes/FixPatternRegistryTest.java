/*
 * LD Fix Pattern Library - Test Suite
 * Copyright (c) 2026 Lisa's Dungeon. All rights reserved.
 */

package com.ld.libraries.fixes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.Collection;
import static org.junit.jupiter.api.Assertions.*;

public class FixPatternRegistryTest {
    
    private FixPatternRegistry registry;
    
    @BeforeEach
    public void setUp() {
        registry = new FixPatternRegistry();
    }
    
    @Test
    public void testRegistryInitialization() {
        assertNotNull(registry);
    }
    
    @Test
    public void testGetUnsafeCollectionFix() {
        FixPatternRegistry.FixTemplate template = registry.getTemplate("fix-unsafe-collection");
        assertNotNull(template);
        assertEquals("fix-unsafe-collection", template.id);
        assertNotNull(template.name);
        assertTrue(template.isAutomatic);
    }
    
    @Test
    public void testGetResourceFix() {
        FixPatternRegistry.FixTemplate template = registry.getTemplate("fix-unclosed-resource");
        assertNotNull(template);
        assertEquals("fix-unclosed-resource", template.id);
        assertTrue(template.isAutomatic);
    }
    
    @Test
    public void testGetNullCheckFix() {
        FixPatternRegistry.FixTemplate template = registry.getTemplate("fix-null-check");
        assertNotNull(template);
        assertEquals("fix-null-check", template.id);
        assertFalse(template.isAutomatic);
    }
    
    @Test
    public void testGetBlockingOperationFix() {
        FixPatternRegistry.FixTemplate template = registry.getTemplate("fix-blocking-operation");
        assertNotNull(template);
        assertEquals("fix-blocking-operation", template.id);
        assertFalse(template.isAutomatic);
        assertEquals("High", template.severity);
    }
    
    @Test
    public void testGetDeprecatedEventFix() {
        FixPatternRegistry.FixTemplate template = registry.getTemplate("fix-deprecated-event");
        assertNotNull(template);
        assertEquals("fix-deprecated-event", template.id);
    }
    
    @Test
    public void testGetCredentialsFix() {
        FixPatternRegistry.FixTemplate template = registry.getTemplate("fix-hardcoded-credentials");
        assertNotNull(template);
        assertEquals("fix-hardcoded-credentials", template.id);
        assertTrue(template.isAutomatic);
        assertEquals("Critical", template.severity);
    }
    
    @Test
    public void testGetCommandInjectionFix() {
        FixPatternRegistry.FixTemplate template = registry.getTemplate("fix-command-injection");
        assertNotNull(template);
        assertEquals("fix-command-injection", template.id);
        assertTrue(template.isAutomatic);
        assertEquals("Critical", template.severity);
    }
    
    @Test
    public void testGetNMSVersionFix() {
        FixPatternRegistry.FixTemplate template = registry.getTemplate("fix-nms-version-check");
        assertNotNull(template);
        assertEquals("fix-nms-version-check", template.id);
        assertTrue(template.isAutomatic);
        assertEquals("High", template.severity);
    }
    
    @Test
    public void testGetUnusedImportsFix() {
        FixPatternRegistry.FixTemplate template = registry.getTemplate("fix-unused-imports");
        assertNotNull(template);
        assertEquals("fix-unused-imports", template.id);
        assertTrue(template.isAutomatic);
    }
    
    @Test
    public void testGetLambdaConversionFix() {
        FixPatternRegistry.FixTemplate template = registry.getTemplate("fix-anonymous-to-lambda");
        assertNotNull(template);
        assertEquals("fix-anonymous-to-lambda", template.id);
        assertFalse(template.isAutomatic);
    }
    
    @Test
    public void testGetAllTemplates() {
        Collection<FixPatternRegistry.FixTemplate> templates = registry.getAllTemplates();
        assertNotNull(templates);
        assertTrue(templates.size() >= 10);
    }
    
    @Test
    public void testGetTemplatesBySeverity() {
        Collection<FixPatternRegistry.FixTemplate> criticalFixes = registry.getTemplatesBySeverity("Critical");
        assertNotNull(criticalFixes);
        assertTrue(criticalFixes.size() > 0);
    }
    
    @Test
    public void testCanAutoFix() {
        assertTrue(registry.canAutoFix("fix-unsafe-collection"));
        assertFalse(registry.canAutoFix("fix-null-check"));
    }
    
    @Test
    public void testVersionString() {
        String version = FixPatternRegistry.getVersion();
        assertNotNull(version);
        assertEquals("1.0.0", version);
    }
    
    @Test
    public void testTemplateProperties() {
        FixPatternRegistry.FixTemplate template = registry.getTemplate("fix-hardcoded-credentials");
        assertNotNull(template.name);
        assertFalse(template.name.isEmpty());
        assertNotNull(template.searchPattern);
        assertNotNull(template.replacementPattern);
        assertTrue(template.timestamp > 0);
    }
}
