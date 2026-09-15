package dev.manoelreis.ordermanagement.domain.order;

import dev.manoelreis.ordermanagement.domain.exception.DomainException;

import java.util.UUID;

public record OrderId(UUID value) {

    public OrderId {
        if (value == null) {
            throw new DomainException("Order id is required");
        }
    }

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID());
    }

    public static OrderId of(UUID value) {
        return new OrderId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
