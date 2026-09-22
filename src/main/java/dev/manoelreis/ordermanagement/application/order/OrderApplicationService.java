package dev.manoelreis.ordermanagement.application.order;

import dev.manoelreis.ordermanagement.application.customer.CustomerNotFoundException;
import dev.manoelreis.ordermanagement.application.customer.CustomerRepository;
import dev.manoelreis.ordermanagement.domain.customer.CustomerId;
import dev.manoelreis.ordermanagement.domain.order.Order;
import dev.manoelreis.ordermanagement.domain.order.OrderId;
import dev.manoelreis.ordermanagement.domain.product.Product;
import dev.manoelreis.ordermanagement.domain.product.ProductId;

import java.time.Clock;
import java.util.List;

public final class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final Clock clock;

    public OrderApplicationService(
            OrderRepository orderRepository,
            CustomerRepository customerRepository,
            Clock clock) {
        if (orderRepository == null) {
            throw new IllegalArgumentException("Order repository is required");
        }
        if (customerRepository == null) {
            throw new IllegalArgumentException("Customer repository is required");
        }
        if (clock == null) {
            throw new IllegalArgumentException("Clock is required");
        }
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.clock = clock;
    }

    public Order createOrder(CustomerId customerId) {
        CustomerId requiredCustomerId = requireCustomerId(customerId);
        customerRepository.findById(requiredCustomerId)
                .orElseThrow(() -> new CustomerNotFoundException(requiredCustomerId));

        Order order = new Order(OrderId.newId(), requiredCustomerId, clock.instant());
        orderRepository.save(order);
        return order;
    }

    public Order findOrderById(OrderId orderId) {
        OrderId requiredOrderId = requireOrderId(orderId);
        return orderRepository.findById(requiredOrderId)
                .orElseThrow(() -> new OrderNotFoundException(requiredOrderId));
    }

    public List<Order> listOrders() {
        return List.copyOf(orderRepository.findAll());
    }

    public Order addProductToOrder(OrderId orderId, Product product, int quantity) {
        Order order = findOrderById(orderId);
        order.addProduct(product, quantity);
        orderRepository.save(order);
        return order;
    }

    public Order changeOrderItemQuantity(OrderId orderId, ProductId productId, int quantity) {
        Order order = findOrderById(orderId);
        order.changeItemQuantity(productId, quantity);
        orderRepository.save(order);
        return order;
    }

    public Order removeOrderItem(OrderId orderId, ProductId productId) {
        Order order = findOrderById(orderId);
        order.removeItem(productId);
        orderRepository.save(order);
        return order;
    }

    private static CustomerId requireCustomerId(CustomerId customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer id is required");
        }
        return customerId;
    }

    private static OrderId requireOrderId(OrderId orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order id is required");
        }
        return orderId;
    }
}
