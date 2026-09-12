package com.rnk.patchwork;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link PatchworkApi}.
 */
class PatchworkApiTest {

    @Test
    void loaderConstantMatchesPatchwork() {
        assertEquals("Patchwork", PatchworkApi.LOADER);
    }

    @Test
    void loaderConstantIsNonNull() {
        assertNotNull(PatchworkApi.LOADER);
    }

    @Test
    void classIsInstantiable() {
        assertNotNull(new PatchworkApi());
    }
}
