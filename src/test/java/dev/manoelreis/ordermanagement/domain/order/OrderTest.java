package dev.manoelreis.ordermanagement.domain.order;

import dev.manoelreis.ordermanagement.domain.customer.CustomerId;
import dev.manoelreis.ordermanagement.domain.exception.DomainException;
import dev.manoelreis.ordermanagement.domain.exception.InvalidQuantityException;
import dev.manoelreis.ordermanagement.domain.exception.OrderItemNotFoundException;
import dev.manoelreis.ordermanagement.domain.payment.PaymentMethod;
import dev.manoelreis.ordermanagement.domain.payment.PaymentResult;
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
    void shouldPayAnEligibleOrderUsingItsSubtotalOnce() {
        Order order = validOrder();
        order.addProduct(product("Keyboard", "100.00"), 2);
        RecordingPaymentMethod paymentMethod = new RecordingPaymentMethod(PaymentResult.APPROVED);

        PaymentResult result = order.pay(paymentMethod);

        assertEquals(PaymentResult.APPROVED, result);
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(1, paymentMethod.calls);
        assertEquals(new BigDecimal("200.00"), paymentMethod.amount);
    }

    @Test
    void shouldKeepDraftOrderUnchangedWhenPaymentIsDeclined() {
        Order order = validOrder();
        order.addProduct(product("Keyboard", "100.00"), 2);
        List<OrderItem> itemsBeforePayment = order.getItems();
        BigDecimal subtotalBeforePayment = order.getSubtotal();
        RecordingPaymentMethod paymentMethod = new RecordingPaymentMethod(PaymentResult.DECLINED);

        PaymentResult result = order.pay(paymentMethod);

        assertEquals(PaymentResult.DECLINED, result);
        assertEquals(1, paymentMethod.calls);
        assertEquals(subtotalBeforePayment, paymentMethod.amount);
        assertEquals(OrderStatus.DRAFT, order.getStatus());
        assertEquals(itemsBeforePayment, order.getItems());
        assertEquals(subtotalBeforePayment, order.getSubtotal());
    }

    @Test
    void shouldRejectInvalidPaymentAttemptsBeforeCharging() {
        Order emptyOrder = validOrder();
        RecordingPaymentMethod emptyOrderMethod = new RecordingPaymentMethod(PaymentResult.APPROVED);
        assertThrows(DomainException.class, () -> emptyOrder.pay(emptyOrderMethod));
        assertEquals(0, emptyOrderMethod.calls);

        Order paidOrder = validOrder();
        paidOrder.addProduct(product("Keyboard", "100.00"), 1);
        paidOrder.pay(amount -> PaymentResult.APPROVED);
        RecordingPaymentMethod paidOrderMethod = new RecordingPaymentMethod(PaymentResult.APPROVED);
        assertThrows(DomainException.class, () -> paidOrder.pay(paidOrderMethod));
        assertEquals(0, paidOrderMethod.calls);

        assertThrows(DomainException.class, () -> paidOrder.pay(null));
        assertEquals(OrderStatus.PAID, paidOrder.getStatus());
    }

    @Test
    void shouldNotMarkOrderAsPaidWhenPaymentResultIsNullOrProcessingFails() {
        Order nullResultOrder = orderWithOneProduct();
        assertThrows(DomainException.class, () -> nullResultOrder.pay(amount -> null));
        assertEquals(OrderStatus.DRAFT, nullResultOrder.getStatus());

        Order failedOrder = orderWithOneProduct();
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> failedOrder.pay(amount -> {
                    throw new IllegalStateException("Payment processor unavailable");
                }));
        assertEquals("Payment processor unavailable", failure.getMessage());
        assertEquals(OrderStatus.DRAFT, failedOrder.getStatus());
    }

    @Test
    void shouldRejectEveryItemChangeAfterPaymentWithoutChangingOrder() {
        Order order = validOrder();
        Product keyboard = product("Keyboard", "100.00");
        order.addProduct(keyboard, 2);
        order.pay(amount -> PaymentResult.APPROVED);
        List<OrderItem> paidItems = order.getItems();
        BigDecimal paidSubtotal = order.getSubtotal();

        assertThrows(DomainException.class,
                () -> order.addProduct(product("Mouse", "50.00"), 1));
        assertThrows(DomainException.class,
                () -> order.changeItemQuantity(keyboard.getId(), 3));
        assertThrows(DomainException.class, () -> order.removeItem(keyboard.getId()));

        assertEquals(paidItems, order.getItems());
        assertEquals(paidSubtotal, order.getSubtotal());
        assertEquals(OrderStatus.PAID, order.getStatus());
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

    private static Order orderWithOneProduct() {
        Order order = validOrder();
        order.addProduct(product("Keyboard", "100.00"), 1);
        return order;
    }

    private static Product product(String name, String price) {
        return new Product(ProductId.newId(), name, "", new BigDecimal(price));
    }

    private static final class RecordingPaymentMethod implements PaymentMethod {

        private final PaymentResult result;
        private BigDecimal amount;
        private int calls;

        private RecordingPaymentMethod(PaymentResult result) {
            this.result = result;
        }

        @Override
        public PaymentResult process(BigDecimal amount) {
            calls++;
            this.amount = amount;
            return result;
        }
    }
}
