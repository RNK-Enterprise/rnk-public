package com.rnk.cuberite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link CuberiteApi}.
 */
class CuberiteApiTest {

    @Test
    void loaderConstantMatchesCuberite() {
        assertEquals("Cuberite", CuberiteApi.LOADER);
    }

    @Test
    void loaderConstantIsNonNull() {
        assertNotNull(CuberiteApi.LOADER);
    }

    @Test
    void classIsInstantiable() {
        assertNotNull(new CuberiteApi());
    }
}
