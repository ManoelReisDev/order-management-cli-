package dev.manoelreis.ordermanagement.domain.customer;

import dev.manoelreis.ordermanagement.domain.exception.DomainException;

import java.util.UUID;

public record CustomerId(UUID value) {

    public CustomerId {
        if (value == null) {
            throw new DomainException("Customer id is required");
        }
    }

    public static CustomerId newId() {
        return new CustomerId(UUID.randomUUID());
    }

    public static CustomerId of(UUID value) {
        return new CustomerId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
