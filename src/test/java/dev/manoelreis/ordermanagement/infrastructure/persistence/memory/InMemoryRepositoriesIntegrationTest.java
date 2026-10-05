package dev.manoelreis.ordermanagement.infrastructure.persistence.memory;

import dev.manoelreis.ordermanagement.application.customer.CustomerApplicationService;
import dev.manoelreis.ordermanagement.application.order.OrderApplicationService;
import dev.manoelreis.ordermanagement.application.product.ProductApplicationService;
import dev.manoelreis.ordermanagement.domain.customer.Customer;
import dev.manoelreis.ordermanagement.domain.order.Order;
import dev.manoelreis.ordermanagement.domain.product.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class InMemoryRepositoriesIntegrationTest {

    @Test
    void shouldRegisterFindAndListCustomersThroughApplicationService() {
        InMemoryCustomerRepository repository = new InMemoryCustomerRepository();
        CustomerApplicationService service = new CustomerApplicationService(repository);

        Customer customer = service.registerCustomer("Ada", "ada@example.com");

        assertSame(customer, service.findCustomerById(customer.getId()));
        assertSame(customer, repository.findById(customer.getId()).orElseThrow());
        assertEquals(List.of(customer), service.listCustomers());
        assertSame(customer, service.listCustomers().getFirst());
    }

    @Test
    void shouldRegisterFindAndListProductsThroughApplicationService() {
        InMemoryProductRepository repository = new InMemoryProductRepository();
        ProductApplicationService service = new ProductApplicationService(repository);

        Product product = service.registerProduct("Keyboard", "Mechanical", new BigDecimal("100.00"));

        assertSame(product, service.findProductById(product.getId()));
        assertSame(product, repository.findById(product.getId()).orElseThrow());
        assertEquals(List.of(product), service.listProducts());
        assertSame(product, service.listProducts().getFirst());
    }

    @Test
    void shouldCreateFindAndListOrdersUsingRegisteredCustomer() {
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        CustomerApplicationService customerService = new CustomerApplicationService(customerRepository);
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        OrderApplicationService orderService = new OrderApplicationService(
                orderRepository, customerRepository, Clock.fixed(createdAt, ZoneOffset.UTC));
        Customer customer = customerService.registerCustomer("Ada", "ada@example.com");

        Order order = orderService.createOrder(customer.getId());

        assertSame(order, orderService.findOrderById(order.getId()));
        assertSame(order, orderRepository.findById(order.getId()).orElseThrow());
        assertEquals(List.of(order), orderService.listOrders());
        assertSame(order, orderService.listOrders().getFirst());
        assertEquals(customer.getId(), order.getCustomerId());
        assertEquals(createdAt, order.getCreatedAt());
    }
}
