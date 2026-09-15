package dev.manoelreis.ordermanagement.domain.order;

import dev.manoelreis.ordermanagement.domain.customer.CustomerId;
import dev.manoelreis.ordermanagement.domain.exception.DomainException;
import dev.manoelreis.ordermanagement.domain.exception.InvalidQuantityException;
import dev.manoelreis.ordermanagement.domain.exception.OrderItemNotFoundException;
import dev.manoelreis.ordermanagement.domain.product.Product;
import dev.manoelreis.ordermanagement.domain.product.ProductId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTest {

    private static final Instant CREATION_INSTANT = Instant.parse("2026-09-14T12:00:00Z");

    @Test
    void shouldCreateAnEmptyDraftOrderAtTheProvidedInstant() {
        OrderId orderId = OrderId.newId();
        CustomerId customerId = CustomerId.newId();

        Order order = new Order(orderId, customerId, CREATION_INSTANT);

        assertEquals(orderId, order.getId());
        assertEquals(customerId, order.getCustomerId());
        assertEquals(CREATION_INSTANT, order.getCreatedAt());
        assertEquals(OrderStatus.DRAFT, order.getStatus());
        assertTrue(order.getItems().isEmpty());
        assertEquals(new BigDecimal("0.00"), order.getSubtotal());
    }

    @Test
    void shouldRejectMissingOrderData() {
        assertThrows(DomainException.class,
                () -> new Order(null, CustomerId.newId(), CREATION_INSTANT));
        assertThrows(DomainException.class,
                () -> new Order(OrderId.newId(), null, CREATION_INSTANT));
        assertThrows(DomainException.class,
                () -> new Order(OrderId.newId(), CustomerId.newId(), null));
    }

    @Test
    void shouldAddProductsAndPreserveTheirInclusionOrder() {
        Order order = validOrder();
        Product keyboard = product("Keyboard", "100.00");
        Product mouse = product("Mouse", "50.00");

        order.addProduct(keyboard, 2);
        order.addProduct(mouse, 1);

        List<OrderItem> items = order.getItems();
        assertEquals(List.of(keyboard.getId(), mouse.getId()),
                items.stream().map(OrderItem::getProductId).toList());
        assertEquals(new BigDecimal("250.00"), order.getSubtotal());
        assertEquals(new BigDecimal("200.00"), items.getFirst().getSubtotal());
    }

    @Test
    void shouldAccumulateQuantityWithoutDuplicatingAnExistingProduct() {
        Order order = validOrder();
        Product keyboard = product("Keyboard", "100.00");

        order.addProduct(keyboard, 2);
        order.addProduct(keyboard, 3);

        assertEquals(1, order.getItems().size());
        assertEquals(5, order.getItems().getFirst().getQuantity());
        assertEquals(new BigDecimal("500.00"), order.getSubtotal());
    }

    @Test
    void shouldChangeTheQuantityOfAnExistingItem() {
        Order order = validOrder();
        Product keyboard = product("Keyboard", "100.00");
        order.addProduct(keyboard, 1);

        order.changeItemQuantity(keyboard.getId(), 4);

        assertEquals(4, order.getItems().getFirst().getQuantity());
        assertEquals(new BigDecimal("400.00"), order.getSubtotal());
    }

    @Test
    void shouldRemoveAnExistingItem() {
        Order order = validOrder();
        Product keyboard = product("Keyboard", "100.00");
        order.addProduct(keyboard, 1);

        order.removeItem(keyboard.getId());

        assertTrue(order.getItems().isEmpty());
        assertEquals(new BigDecimal("0.00"), order.getSubtotal());
    }

    @Test
    void shouldRejectInvalidQuantities() {
        Order order = validOrder();
        Product keyboard = product("Keyboard", "100.00");

        assertThrows(InvalidQuantityException.class, () -> order.addProduct(keyboard, 0));
        assertThrows(InvalidQuantityException.class, () -> order.addProduct(keyboard, -1));

        order.addProduct(keyboard, 1);

        assertThrows(InvalidQuantityException.class,
                () -> order.changeItemQuantity(keyboard.getId(), 0));
        assertThrows(InvalidQuantityException.class,
                () -> order.changeItemQuantity(keyboard.getId(), -1));
    }

    @Test
    void shouldRejectQuantityOverflowWithoutChangingTheItem() {
        Order order = validOrder();
        Product keyboard = product("Keyboard", "1.00");
        order.addProduct(keyboard, Integer.MAX_VALUE);

        assertThrows(InvalidQuantityException.class, () -> order.addProduct(keyboard, 1));
        assertEquals(Integer.MAX_VALUE, order.getItems().getFirst().getQuantity());
    }

    @Test
    void shouldRejectChangesForAMissingItem() {
        Order order = validOrder();
        ProductId missingProductId = ProductId.newId();

        assertThrows(OrderItemNotFoundException.class,
                () -> order.changeItemQuantity(missingProductId, 1));
        assertThrows(OrderItemNotFoundException.class,
                () -> order.removeItem(missingProductId));
        assertThrows(OrderItemNotFoundException.class,
                () -> order.removeItem(null));
    }

    @Test
    void shouldPreserveTheProductSnapshotWhenTheProductChanges() {
        Order order = validOrder();
        Product product = product("Keyboard", "100.00");
        order.addProduct(product, 1);

        product.rename("Updated keyboard");
        product.changePrice(new BigDecimal("150.00"));

        OrderItem item = order.getItems().getFirst();
        assertEquals("Keyboard", item.getProductName());
        assertEquals(new BigDecimal("100.00"), item.getUnitPrice());
        assertEquals(new BigDecimal("100.00"), item.getSubtotal());
    }

    @Test
    void shouldNotExposeAModifiableItemsCollection() {
        Order order = validOrder();
        order.addProduct(product("Keyboard", "100.00"), 1);

        List<OrderItem> items = order.getItems();

        assertThrows(UnsupportedOperationException.class, items::clear);
        assertEquals(1, order.getItems().size());
    }

    @Test
    void shouldDetermineEqualityByOrderId() {
        OrderId id = OrderId.newId();
        Order first = new Order(id, CustomerId.newId(), CREATION_INSTANT);
        Order sameIdentity = new Order(id, CustomerId.newId(), CREATION_INSTANT.plusSeconds(1));
        Order anotherOrder = validOrder();

        assertEquals(first, sameIdentity);
        assertEquals(first.hashCode(), sameIdentity.hashCode());
        assertNotEquals(first, anotherOrder);
    }

    private static Order validOrder() {
        return new Order(OrderId.newId(), CustomerId.newId(), CREATION_INSTANT);
    }

    private static Product product(String name, String price) {
        return new Product(ProductId.newId(), name, "", new BigDecimal(price));
    }
}
