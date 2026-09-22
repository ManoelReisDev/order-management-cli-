package dev.manoelreis.ordermanagement.application.order;

import dev.manoelreis.ordermanagement.application.customer.CustomerNotFoundException;
import dev.manoelreis.ordermanagement.application.customer.CustomerRepository;
import dev.manoelreis.ordermanagement.domain.customer.Customer;
import dev.manoelreis.ordermanagement.domain.customer.CustomerId;
import dev.manoelreis.ordermanagement.domain.exception.DomainException;
import dev.manoelreis.ordermanagement.domain.exception.InvalidQuantityException;
import dev.manoelreis.ordermanagement.domain.exception.OrderItemNotFoundException;
import dev.manoelreis.ordermanagement.domain.order.Order;
import dev.manoelreis.ordermanagement.domain.order.OrderId;
import dev.manoelreis.ordermanagement.domain.order.OrderStatus;
import dev.manoelreis.ordermanagement.domain.product.Product;
import dev.manoelreis.ordermanagement.domain.product.ProductId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderApplicationServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-22T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    private RecordingOrderRepository orderRepository;
    private RecordingCustomerRepository customerRepository;
    private OrderApplicationService service;

    @BeforeEach
    void setUp() {
        orderRepository = new RecordingOrderRepository();
        customerRepository = new RecordingCustomerRepository();
        service = new OrderApplicationService(orderRepository, customerRepository, FIXED_CLOCK);
    }

    @Test
    void shouldRejectNullDependencies() {
        assertThrows(IllegalArgumentException.class,
                () -> new OrderApplicationService(null, customerRepository, FIXED_CLOCK));
        assertThrows(IllegalArgumentException.class,
                () -> new OrderApplicationService(orderRepository, null, FIXED_CLOCK));
        assertThrows(IllegalArgumentException.class,
                () -> new OrderApplicationService(orderRepository, customerRepository, null));
    }

    @Test
    void shouldCreateAndSaveEmptyDraftOrderForExistingCustomer() {
        Customer customer = customer();
        customerRepository.customerToFind = Optional.of(customer);

        Order createdOrder = service.createOrder(customer.getId());

        assertNotNull(createdOrder.getId());
        assertEquals(customer.getId(), createdOrder.getCustomerId());
        assertEquals(FIXED_INSTANT, createdOrder.getCreatedAt());
        assertEquals(OrderStatus.DRAFT, createdOrder.getStatus());
        assertTrue(createdOrder.getItems().isEmpty());
        assertEquals(new BigDecimal("0.00"), createdOrder.getSubtotal());
        assertEquals(List.of(createdOrder), orderRepository.savedOrders);
    }

    @Test
    void shouldNotCreateOrderForMissingCustomer() {
        CustomerId customerId = CustomerId.newId();

        CustomerNotFoundException exception = assertThrows(CustomerNotFoundException.class,
                () -> service.createOrder(customerId));

        assertTrue(exception.getMessage().contains(customerId.toString()));
        assertTrue(orderRepository.savedOrders.isEmpty());
    }

    @Test
    void shouldRejectNullCustomerIdBeforeUsingCollaborators() {
        assertThrows(IllegalArgumentException.class, () -> service.createOrder(null));

        assertEquals(0, customerRepository.findByIdCalls);
        assertTrue(orderRepository.savedOrders.isEmpty());
    }

    @Test
    void shouldFindOrderById() {
        Order order = order();
        orderRepository.orderToFind = Optional.of(order);

        Order foundOrder = service.findOrderById(order.getId());

        assertSame(order, foundOrder);
    }

    @Test
    void shouldThrowExceptionWithIdWhenOrderIsNotFound() {
        OrderId orderId = OrderId.newId();

        OrderNotFoundException exception = assertThrows(OrderNotFoundException.class,
                () -> service.findOrderById(orderId));

        assertFalse(exception.getMessage().isBlank());
        assertTrue(exception.getMessage().contains(orderId.toString()));
    }

    @Test
    void shouldReturnEmptyOrderList() {
        assertEquals(List.of(), service.listOrders());
    }

    @Test
    void shouldReturnUnmodifiableOrderListCopyInRepositoryOrder() {
        Order first = order();
        Order second = order();
        List<Order> repositoryOrders = new ArrayList<>(List.of(first, second));
        orderRepository.orders = repositoryOrders;

        List<Order> orders = service.listOrders();
        repositoryOrders.clear();

        assertEquals(List.of(first, second), orders);
        assertNotSame(repositoryOrders, orders);
        assertThrows(UnsupportedOperationException.class, () -> orders.add(first));
        assertThrows(UnsupportedOperationException.class, () -> orders.remove(first));
    }

    @Test
    void shouldAddProductAndSaveOrder() {
        Order order = order();
        Product product = product("Keyboard", "100.00");
        orderRepository.orderToFind = Optional.of(order);

        Order changedOrder = service.addProductToOrder(order.getId(), product, 2);

        assertSame(order, changedOrder);
        assertEquals(1, order.getItems().size());
        assertEquals(2, order.getItems().getFirst().getQuantity());
        assertEquals(new BigDecimal("200.00"), order.getSubtotal());
        assertEquals(List.of(order), orderRepository.savedOrders);
    }

    @Test
    void shouldAccumulateQuantityWhenAddingSameProductAgain() {
        Order order = order();
        Product product = product("Keyboard", "100.00");
        orderRepository.orderToFind = Optional.of(order);

        service.addProductToOrder(order.getId(), product, 2);
        service.addProductToOrder(order.getId(), product, 3);

        assertEquals(1, order.getItems().size());
        assertEquals(5, order.getItems().getFirst().getQuantity());
        assertEquals(2, orderRepository.savedOrders.size());
    }

    @Test
    void shouldChangeItemQuantityAndSaveOrder() {
        Order order = order();
        Product product = product("Keyboard", "100.00");
        order.addProduct(product, 1);
        orderRepository.orderToFind = Optional.of(order);

        Order changedOrder = service.changeOrderItemQuantity(order.getId(), product.getId(), 4);

        assertSame(order, changedOrder);
        assertEquals(4, order.getItems().getFirst().getQuantity());
        assertEquals(List.of(order), orderRepository.savedOrders);
    }

    @Test
    void shouldRemoveItemAndSaveOrder() {
        Order order = order();
        Product product = product("Keyboard", "100.00");
        order.addProduct(product, 1);
        orderRepository.orderToFind = Optional.of(order);

        Order changedOrder = service.removeOrderItem(order.getId(), product.getId());

        assertSame(order, changedOrder);
        assertTrue(order.getItems().isEmpty());
        assertEquals(List.of(order), orderRepository.savedOrders);
    }

    @Test
    void shouldNotSaveChangesWhenOrderIsNotFound() {
        OrderId orderId = OrderId.newId();
        Product product = product("Keyboard", "100.00");

        assertThrows(OrderNotFoundException.class,
                () -> service.addProductToOrder(orderId, product, 1));
        assertThrows(OrderNotFoundException.class,
                () -> service.changeOrderItemQuantity(orderId, product.getId(), 2));
        assertThrows(OrderNotFoundException.class,
                () -> service.removeOrderItem(orderId, product.getId()));

        assertTrue(orderRepository.savedOrders.isEmpty());
    }

    @Test
    void shouldPropagateDomainErrorsWithoutSavingOrder() {
        Order order = order();
        Product product = product("Keyboard", "100.00");
        order.addProduct(product, 1);
        orderRepository.orderToFind = Optional.of(order);

        assertThrows(DomainException.class,
                () -> service.addProductToOrder(order.getId(), null, 1));
        assertThrows(InvalidQuantityException.class,
                () -> service.addProductToOrder(order.getId(), product, 0));
        assertThrows(OrderItemNotFoundException.class,
                () -> service.changeOrderItemQuantity(order.getId(), ProductId.newId(), 2));
        assertThrows(OrderItemNotFoundException.class,
                () -> service.removeOrderItem(order.getId(), null));

        assertTrue(orderRepository.savedOrders.isEmpty());
    }

    @Test
    void shouldRejectNullOrderIdBeforeConsultingRepository() {
        Product product = product("Keyboard", "100.00");

        assertThrows(IllegalArgumentException.class, () -> service.findOrderById(null));
        assertThrows(IllegalArgumentException.class,
                () -> service.addProductToOrder(null, product, 1));
        assertThrows(IllegalArgumentException.class,
                () -> service.changeOrderItemQuantity(null, product.getId(), 2));
        assertThrows(IllegalArgumentException.class,
                () -> service.removeOrderItem(null, product.getId()));

        assertEquals(0, orderRepository.findByIdCalls);
        assertTrue(orderRepository.savedOrders.isEmpty());
    }

    private static Customer customer() {
        return new Customer(CustomerId.newId(), "Ada", "ada@example.com");
    }

    private static Order order() {
        return new Order(OrderId.newId(), CustomerId.newId(), FIXED_INSTANT);
    }

    private static Product product(String name, String price) {
        return new Product(ProductId.newId(), name, "", new BigDecimal(price));
    }

    private static final class RecordingOrderRepository implements OrderRepository {

        private final List<Order> savedOrders = new ArrayList<>();
        private Optional<Order> orderToFind = Optional.empty();
        private List<Order> orders = List.of();
        private int findByIdCalls;

        @Override
        public void save(Order order) {
            savedOrders.add(order);
        }

        @Override
        public Optional<Order> findById(OrderId id) {
            findByIdCalls++;
            return orderToFind;
        }

        @Override
        public List<Order> findAll() {
            return orders;
        }
    }

    private static final class RecordingCustomerRepository implements CustomerRepository {

        private Optional<Customer> customerToFind = Optional.empty();
        private int findByIdCalls;

        @Override
        public void save(Customer customer) {
            throw new AssertionError("Order cases must not save customers");
        }

        @Override
        public Optional<Customer> findById(CustomerId id) {
            findByIdCalls++;
            return customerToFind;
        }

        @Override
        public List<Customer> findAll() {
            throw new AssertionError("Order cases must not list customers");
        }
    }
}
