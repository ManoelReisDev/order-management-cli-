package dev.manoelreis.ordermanagement.application.order;

import dev.manoelreis.ordermanagement.domain.order.OrderId;

public final class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(OrderId orderId) {
        super("Order not found: " + orderId);
    }
}
