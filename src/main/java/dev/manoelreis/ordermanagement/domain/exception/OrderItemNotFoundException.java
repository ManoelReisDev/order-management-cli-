package dev.manoelreis.ordermanagement.domain.exception;

public final class OrderItemNotFoundException extends DomainException {

    public OrderItemNotFoundException(String message) {
        super(message);
    }
}
