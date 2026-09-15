package dev.manoelreis.ordermanagement.domain.product;

import dev.manoelreis.ordermanagement.domain.exception.DomainException;

import java.util.UUID;

public record ProductId(UUID value) {

    public ProductId {
        if (value == null) {
            throw new DomainException("Product id is required");
        }
    }

    public static ProductId newId() {
        return new ProductId(UUID.randomUUID());
    }

    public static ProductId of(UUID value) {
        return new ProductId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
