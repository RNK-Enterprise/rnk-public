package com.rnk.magma;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link MagmaApi}.
 */
class MagmaApiTest {

    @Test
    void loaderConstantMatchesMagma() {
        assertEquals("Magma", MagmaApi.LOADER);
    }

    @Test
    void loaderConstantIsNonNull() {
        assertNotNull(MagmaApi.LOADER);
    }

    @Test
    void classIsInstantiable() {
        assertNotNull(new MagmaApi());
    }
}
