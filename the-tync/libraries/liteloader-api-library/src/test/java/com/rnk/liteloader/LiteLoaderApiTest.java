package com.rnk.liteloader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link LiteLoaderApi}.
 */
class LiteLoaderApiTest {

    @Test
    void loaderConstantMatchesLiteLoader() {
        assertEquals("LiteLoader", LiteLoaderApi.LOADER);
    }

    @Test
    void loaderConstantIsNonNull() {
        assertNotNull(LiteLoaderApi.LOADER);
    }

    @Test
    void classIsInstantiable() {
        assertNotNull(new LiteLoaderApi());
    }
}
