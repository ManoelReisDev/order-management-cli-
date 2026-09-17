package dev.manoelreis.ordermanagement.application.customer;

import dev.manoelreis.ordermanagement.domain.customer.Customer;
import dev.manoelreis.ordermanagement.domain.customer.CustomerId;

import java.util.List;

public final class CustomerApplicationService {

    private final CustomerRepository customerRepository;

    public CustomerApplicationService(CustomerRepository customerRepository) {
        if (customerRepository == null) {
            throw new IllegalArgumentException("Customer repository is required");
        }
        this.customerRepository = customerRepository;
    }

    public Customer registerCustomer(String name, String email) {
        Customer customer = new Customer(CustomerId.newId(), name, email);
        customerRepository.save(customer);
        return customer;
    }

    public Customer findCustomerById(CustomerId customerId) {
        CustomerId requiredCustomerId = requireCustomerId(customerId);
        return customerRepository.findById(requiredCustomerId)
                .orElseThrow(() -> new CustomerNotFoundException(requiredCustomerId));
    }

    public List<Customer> listCustomers() {
        return List.copyOf(customerRepository.findAll());
    }

    public Customer renameCustomer(CustomerId customerId, String name) {
        Customer customer = findCustomerById(customerId);
        customer.rename(name);
        customerRepository.save(customer);
        return customer;
    }

    public Customer changeCustomerEmail(CustomerId customerId, String email) {
        Customer customer = findCustomerById(customerId);
        customer.changeEmail(email);
        customerRepository.save(customer);
        return customer;
    }

    private static CustomerId requireCustomerId(CustomerId customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer id is required");
        }
        return customerId;
    }
}
