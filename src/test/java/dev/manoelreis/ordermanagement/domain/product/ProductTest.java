package dev.manoelreis.ordermanagement.domain.product;

import dev.manoelreis.ordermanagement.domain.exception.InvalidProductException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductTest {

    @Test
    void shouldCreateAndChangeAValidProduct() {
        Product product = new Product(ProductId.newId(), "  Keyboard  ", "  Mechanical  ", new BigDecimal("100"));

        assertEquals("Keyboard", product.getName());
        assertEquals("Mechanical", product.getDescription());
        assertEquals(new BigDecimal("100.00"), product.getPrice());

        product.rename(" Mouse ");
        product.changeDescription(" Wireless ");
        product.changePrice(new BigDecimal("49.90"));

        assertEquals("Mouse", product.getName());
        assertEquals("Wireless", product.getDescription());
        assertEquals(new BigDecimal("49.90"), product.getPrice());
    }

    @Test
    void shouldAllowAnEmptyDescription() {
        Product product = new Product(ProductId.newId(), "Keyboard", "", new BigDecimal("100.00"));

        assertEquals("", product.getDescription());
    }

    @Test
    void shouldRejectInvalidProductData() {
        assertThrows(InvalidProductException.class,
                () -> new Product(null, "Keyboard", "", new BigDecimal("100.00")));
        assertThrows(InvalidProductException.class,
                () -> new Product(ProductId.newId(), " ", "", new BigDecimal("100.00")));
        assertThrows(InvalidProductException.class,
                () -> new Product(ProductId.newId(), "Keyboard", null, new BigDecimal("100.00")));
        assertThrows(InvalidProductException.class,
                () -> new Product(ProductId.newId(), "Keyboard", "", null));
        assertThrows(InvalidProductException.class,
                () -> new Product(ProductId.newId(), "Keyboard", "", BigDecimal.ZERO));
        assertThrows(InvalidProductException.class,
                () -> new Product(ProductId.newId(), "Keyboard", "", new BigDecimal("-1.00")));
        assertThrows(InvalidProductException.class,
                () -> new Product(ProductId.newId(), "Keyboard", "", new BigDecimal("10.999")));
    }

    @Test
    void shouldReapplyInvariantsWhenChangingProduct() {
        Product product = validProduct(ProductId.newId());

        assertThrows(InvalidProductException.class, () -> product.rename(""));
        assertThrows(InvalidProductException.class, () -> product.changeDescription(null));
        assertThrows(InvalidProductException.class, () -> product.changePrice(new BigDecimal("5.555")));
    }

    @Test
    void shouldDetermineEqualityByProductId() {
        ProductId id = ProductId.newId();
        Product first = validProduct(id);
        Product sameIdentity = new Product(id, "Mouse", "Wireless", new BigDecimal("50.00"));
        Product anotherProduct = validProduct(ProductId.newId());

        assertEquals(first, sameIdentity);
        assertEquals(first.hashCode(), sameIdentity.hashCode());
        assertNotEquals(first, anotherProduct);
    }

    private static Product validProduct(ProductId id) {
        return new Product(id, "Keyboard", "Mechanical", new BigDecimal("100.00"));
    }
}
