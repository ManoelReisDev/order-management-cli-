package dev.manoelreis.ordermanagement.application.customer;

import dev.manoelreis.ordermanagement.domain.customer.Customer;
import dev.manoelreis.ordermanagement.domain.customer.CustomerId;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository {

    void save(Customer customer);

    Optional<Customer> findById(CustomerId id);

    List<Customer> findAll();
}
