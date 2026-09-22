package dev.manoelreis.ordermanagement.application.order;

import dev.manoelreis.ordermanagement.domain.order.Order;
import dev.manoelreis.ordermanagement.domain.order.OrderId;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    void save(Order order);

    Optional<Order> findById(OrderId id);

    List<Order> findAll();
}
