package com.rnk.glowstone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link GlowstoneApi}.
 */
class GlowstoneApiTest {

    @Test
    void loaderConstantMatchesGlowstone() {
        assertEquals("Glowstone", GlowstoneApi.LOADER);
    }

    @Test
    void loaderConstantIsNonNull() {
        assertNotNull(GlowstoneApi.LOADER);
    }

    @Test
    void classIsInstantiable() {
        assertNotNull(new GlowstoneApi());
    }
}
