package com.ld.sponge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link SpongeApi}.
 */
class SpongeApiTest {

    @Test
    void loaderConstantMatchesSponge() {
        assertEquals("Sponge", SpongeApi.LOADER);
    }

    @Test
    void loaderConstantIsNonNull() {
        assertNotNull(SpongeApi.LOADER);
    }

    @Test
    void classIsInstantiable() {
        assertNotNull(new SpongeApi());
    }
}
