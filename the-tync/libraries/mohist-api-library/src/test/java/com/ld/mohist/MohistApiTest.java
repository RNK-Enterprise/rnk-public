package com.ld.mohist;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link MohistApi}.
 */
class MohistApiTest {

    @Test
    void loaderConstantMatchesMohist() {
        assertEquals("Mohist", MohistApi.LOADER);
    }

    @Test
    void loaderConstantIsNonNull() {
        assertNotNull(MohistApi.LOADER);
    }

    @Test
    void classIsInstantiable() {
        assertNotNull(new MohistApi());
    }
}
