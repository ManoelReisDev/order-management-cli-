package dev.manoelreis.ordermanagement.infrastructure.persistence.memory;

import dev.manoelreis.ordermanagement.application.order.OrderRepository;
import dev.manoelreis.ordermanagement.domain.order.Order;
import dev.manoelreis.ordermanagement.domain.order.OrderId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryOrderRepository implements OrderRepository {

    private final Map<OrderId, Order> orders = new LinkedHashMap<>();

    @Override
    public void save(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order is required");
        }
        orders.put(order.getId(), order);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        if (id == null) {
            throw new IllegalArgumentException("Order id is required");
        }
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public List<Order> findAll() {
        return List.copyOf(orders.values());
    }
}
