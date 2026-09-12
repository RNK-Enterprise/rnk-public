package com.rnk.quilt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link QuiltApi}.
 */
class QuiltApiTest {

    @Test
    void loaderConstantMatchesQuilt() {
        assertEquals("Quilt", QuiltApi.LOADER);
    }

    @Test
    void loaderConstantIsNonNull() {
        assertNotNull(QuiltApi.LOADER);
    }

    @Test
    void classIsInstantiable() {
        assertNotNull(new QuiltApi());
    }
}
