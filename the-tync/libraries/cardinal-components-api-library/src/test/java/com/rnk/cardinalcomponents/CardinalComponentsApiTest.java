package com.rnk.cardinalcomponents;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link CardinalComponentsApi}.
 */
class CardinalComponentsApiTest {

    @Test
    void loaderConstantMatchesCardinalComponents() {
        assertEquals("CardinalComponents", CardinalComponentsApi.LOADER);
    }

    @Test
    void loaderConstantIsNonNull() {
        assertNotNull(CardinalComponentsApi.LOADER);
    }

    @Test
    void classIsInstantiable() {
        assertNotNull(new CardinalComponentsApi());
    }
}
