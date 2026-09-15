package dev.manoelreis.ordermanagement.domain.product;

import dev.manoelreis.ordermanagement.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductIdTest {

    @Test
    void shouldGenerateAndReconstructProductIds() {
        UUID value = UUID.randomUUID();

        ProductId generated = ProductId.newId();
        ProductId reconstructed = ProductId.of(value);

        assertNotNull(generated.value());
        assertEquals(value, reconstructed.value());
        assertEquals(value.toString(), reconstructed.toString());
        assertNotEquals(generated, reconstructed);
    }

    @Test
    void shouldRejectNullValue() {
        assertThrows(DomainException.class, () -> ProductId.of(null));
    }
}
