package dev.manoelreis.ordermanagement.application.payment;

import dev.manoelreis.ordermanagement.application.order.OrderNotFoundException;
import dev.manoelreis.ordermanagement.application.order.OrderRepository;
import dev.manoelreis.ordermanagement.domain.order.Order;
import dev.manoelreis.ordermanagement.domain.order.OrderId;
import dev.manoelreis.ordermanagement.domain.payment.PaymentMethod;
import dev.manoelreis.ordermanagement.domain.payment.PaymentResult;

public final class PaymentApplicationService {

    private final OrderRepository orderRepository;

    public PaymentApplicationService(OrderRepository orderRepository) {
        if (orderRepository == null) {
            throw new IllegalArgumentException("Order repository is required");
        }
        this.orderRepository = orderRepository;
    }

    public PaymentResult processPayment(OrderId orderId, PaymentMethod method) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order id is required");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        PaymentResult result = order.pay(method);
        if (result == PaymentResult.APPROVED) {
            orderRepository.save(order);
        }
        return result;
    }
}
