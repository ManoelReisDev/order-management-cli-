package dev.manoelreis.ordermanagement.application.customer;

import dev.manoelreis.ordermanagement.domain.customer.CustomerId;

public final class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(CustomerId customerId) {
        super("Customer not found: " + customerId);
    }
}
