package dev.manoelreis.ordermanagement.application.payment;

import dev.manoelreis.ordermanagement.application.order.OrderNotFoundException;
import dev.manoelreis.ordermanagement.application.order.OrderRepository;
import dev.manoelreis.ordermanagement.domain.customer.CustomerId;
import dev.manoelreis.ordermanagement.domain.exception.DomainException;
import dev.manoelreis.ordermanagement.domain.order.Order;
import dev.manoelreis.ordermanagement.domain.order.OrderId;
import dev.manoelreis.ordermanagement.domain.order.OrderStatus;
import dev.manoelreis.ordermanagement.domain.payment.PaymentMethod;
import dev.manoelreis.ordermanagement.domain.payment.PaymentResult;
import dev.manoelreis.ordermanagement.domain.product.Product;
import dev.manoelreis.ordermanagement.domain.product.ProductId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentApplicationServiceTest {

    private RecordingOrderRepository orderRepository;
    private PaymentApplicationService service;

    @BeforeEach
    void setUp() {
        orderRepository = new RecordingOrderRepository();
        service = new PaymentApplicationService(orderRepository);
    }

    @Test
    void shouldRejectNullRepository() {
        assertThrows(IllegalArgumentException.class, () -> new PaymentApplicationService(null));
    }

    @Test
    void shouldRejectNullOrderIdBeforeConsultingRepository() {
        RecordingPaymentMethod paymentMethod = new RecordingPaymentMethod(PaymentResult.APPROVED);

        assertThrows(IllegalArgumentException.class,
                () -> service.processPayment(null, paymentMethod));

        assertEquals(0, orderRepository.findByIdCalls);
        assertEquals(0, paymentMethod.calls);
        assertTrue(orderRepository.savedOrders.isEmpty());
    }

    @Test
    void shouldNotChargeOrSaveWhenOrderIsMissing() {
        OrderId orderId = OrderId.newId();
        RecordingPaymentMethod paymentMethod = new RecordingPaymentMethod(PaymentResult.APPROVED);

        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class,
                () -> service.processPayment(orderId, paymentMethod));

        assertTrue(exception.getMessage().contains(orderId.toString()));
        assertEquals(0, paymentMethod.calls);
        assertTrue(orderRepository.savedOrders.isEmpty());
    }

    @Test
    void shouldApproveAndSaveOrderOnceUsingItsSubtotal() {
        Order order = orderWithProduct("Keyboard", "100.00", 2);
        orderRepository.orderToFind = Optional.of(order);
        RecordingPaymentMethod paymentMethod = new RecordingPaymentMethod(PaymentResult.APPROVED);

        PaymentResult result = service.processPayment(order.getId(), paymentMethod);

        assertEquals(PaymentResult.APPROVED, result);
        assertEquals(new BigDecimal("200.00"), paymentMethod.amount);
        assertEquals(1, paymentMethod.calls);
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(List.of(order), orderRepository.savedOrders);
        assertSame(order, orderRepository.savedOrders.getFirst());
    }

    @Test
    void shouldReturnDeclineWithoutChangingOrSavingOrder() {
        Order order = orderWithProduct("Keyboard", "100.00", 2);
        orderRepository.orderToFind = Optional.of(order);
        BigDecimal subtotalBeforePayment = order.getSubtotal();
        RecordingPaymentMethod paymentMethod = new RecordingPaymentMethod(PaymentResult.DECLINED);

        PaymentResult result = service.processPayment(order.getId(), paymentMethod);

        assertEquals(PaymentResult.DECLINED, result);
        assertEquals(1, paymentMethod.calls);
        assertEquals(subtotalBeforePayment, paymentMethod.amount);
        assertEquals(OrderStatus.DRAFT, order.getStatus());
        assertEquals(subtotalBeforePayment, order.getSubtotal());
        assertTrue(orderRepository.savedOrders.isEmpty());
    }

    @Test
    void shouldNotSaveWhenDomainRejectsPayment() {
        Order emptyOrder = order();
        orderRepository.orderToFind = Optional.of(emptyOrder);
        RecordingPaymentMethod emptyOrderMethod = new RecordingPaymentMethod(PaymentResult.APPROVED);

        assertThrows(DomainException.class,
                () -> service.processPayment(emptyOrder.getId(), emptyOrderMethod));
        assertEquals(0, emptyOrderMethod.calls);
        assertTrue(orderRepository.savedOrders.isEmpty());

        Order eligibleOrder = orderWithProduct("Keyboard", "100.00", 1);
        orderRepository.orderToFind = Optional.of(eligibleOrder);
        assertThrows(DomainException.class,
                () -> service.processPayment(eligibleOrder.getId(), null));
        assertEquals(OrderStatus.DRAFT, eligibleOrder.getStatus());
        assertTrue(orderRepository.savedOrders.isEmpty());

        eligibleOrder.pay(amount -> PaymentResult.APPROVED);
        RecordingPaymentMethod paidOrderMethod = new RecordingPaymentMethod(PaymentResult.APPROVED);
        assertThrows(DomainException.class,
                () -> service.processPayment(eligibleOrder.getId(), paidOrderMethod));
        assertEquals(0, paidOrderMethod.calls);
        assertTrue(orderRepository.savedOrders.isEmpty());
    }

    @Test
    void shouldNotSaveOrChangeStatusWhenPaymentReturnsNullOrFails() {
        Order nullResultOrder = orderWithProduct("Keyboard", "100.00", 1);
        orderRepository.orderToFind = Optional.of(nullResultOrder);

        assertThrows(DomainException.class,
                () -> service.processPayment(nullResultOrder.getId(), amount -> null));
        assertEquals(OrderStatus.DRAFT, nullResultOrder.getStatus());
        assertTrue(orderRepository.savedOrders.isEmpty());

        Order failedOrder = orderWithProduct("Mouse", "50.00", 1);
        orderRepository.orderToFind = Optional.of(failedOrder);
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> service.processPayment(failedOrder.getId(), amount -> {
                    throw new IllegalStateException("Payment processor unavailable");
                }));

        assertEquals("Payment processor unavailable", failure.getMessage());
        assertEquals(OrderStatus.DRAFT, failedOrder.getStatus());
        assertTrue(orderRepository.savedOrders.isEmpty());
    }

    @Test
    void shouldPropagateRepositoryFailures() {
        IllegalStateException findFailure = new IllegalStateException("Repository unavailable");
        orderRepository.findFailure = findFailure;
        RecordingPaymentMethod paymentMethod = new RecordingPaymentMethod(PaymentResult.APPROVED);

        IllegalStateException propagatedFindFailure = assertThrows(IllegalStateException.class,
                () -> service.processPayment(OrderId.newId(), paymentMethod));

        assertSame(findFailure, propagatedFindFailure);
        assertEquals(0, paymentMethod.calls);
        assertTrue(orderRepository.savedOrders.isEmpty());

        Order order = orderWithProduct("Keyboard", "100.00", 1);
        orderRepository.findFailure = null;
        orderRepository.orderToFind = Optional.of(order);
        IllegalStateException saveFailure = new IllegalStateException("Save unavailable");
        orderRepository.saveFailure = saveFailure;

        IllegalStateException propagatedSaveFailure = assertThrows(IllegalStateException.class,
                () -> service.processPayment(order.getId(), paymentMethod));

        assertSame(saveFailure, propagatedSaveFailure);
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(1, paymentMethod.calls);
        assertTrue(orderRepository.savedOrders.isEmpty());
    }

    private static Order orderWithProduct(String name, String price, int quantity) {
        Order order = order();
        order.addProduct(new Product(ProductId.newId(), name, "", new BigDecimal(price)), quantity);
        return order;
    }

    private static Order order() {
        return new Order(OrderId.newId(), CustomerId.newId(), Instant.parse("2026-09-29T12:00:00Z"));
    }

    private static final class RecordingOrderRepository implements OrderRepository {

        private final List<Order> savedOrders = new ArrayList<>();
        private Optional<Order> orderToFind = Optional.empty();
        private RuntimeException findFailure;
        private RuntimeException saveFailure;
        private int findByIdCalls;

        @Override
        public void save(Order order) {
            if (saveFailure != null) {
                throw saveFailure;
            }
            savedOrders.add(order);
        }

        @Override
        public Optional<Order> findById(OrderId id) {
            findByIdCalls++;
            if (findFailure != null) {
                throw findFailure;
            }
            return orderToFind;
        }

        @Override
        public List<Order> findAll() {
            throw new AssertionError("Payment cases must not list orders");
        }
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
