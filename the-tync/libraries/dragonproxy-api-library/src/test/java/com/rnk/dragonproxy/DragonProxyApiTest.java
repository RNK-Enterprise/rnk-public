package com.rnk.dragonproxy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link DragonProxyApi}.
 */
class DragonProxyApiTest {

    @Test
    void loaderConstantMatchesDragonProxy() {
        assertEquals("DragonProxy", DragonProxyApi.LOADER);
    }

    @Test
    void loaderConstantIsNonNull() {
        assertNotNull(DragonProxyApi.LOADER);
    }

    @Test
    void classIsInstantiable() {
        assertNotNull(new DragonProxyApi());
    }
}
