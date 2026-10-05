package dev.manoelreis.ordermanagement.infrastructure.persistence.memory;

import dev.manoelreis.ordermanagement.application.customer.CustomerRepository;
import dev.manoelreis.ordermanagement.domain.customer.Customer;
import dev.manoelreis.ordermanagement.domain.customer.CustomerId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryCustomerRepository implements CustomerRepository {

    private final Map<CustomerId, Customer> customers = new LinkedHashMap<>();

    @Override
    public void save(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer is required");
        }
        customers.put(customer.getId(), customer);
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        if (id == null) {
            throw new IllegalArgumentException("Customer id is required");
        }
        return Optional.ofNullable(customers.get(id));
    }

    @Override
    public List<Customer> findAll() {
        return List.copyOf(customers.values());
    }
}
