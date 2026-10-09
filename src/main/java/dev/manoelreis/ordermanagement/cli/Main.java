package dev.manoelreis.ordermanagement.cli;

import dev.manoelreis.ordermanagement.cli.menu.TerminalSession;

import dev.manoelreis.ordermanagement.application.customer.CustomerApplicationService;
import dev.manoelreis.ordermanagement.application.order.OrderApplicationService;
import dev.manoelreis.ordermanagement.application.product.ProductApplicationService;
import dev.manoelreis.ordermanagement.infrastructure.persistence.memory.InMemoryCustomerRepository;
import dev.manoelreis.ordermanagement.infrastructure.persistence.memory.InMemoryOrderRepository;
import dev.manoelreis.ordermanagement.infrastructure.persistence.memory.InMemoryProductRepository;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        CustomerApplicationService customers = new CustomerApplicationService(customerRepository);
        ProductApplicationService products = new ProductApplicationService(productRepository);
        OrderApplicationService orders = new OrderApplicationService(
                orderRepository, customerRepository, Clock.systemUTC());
        TerminalSession session = new TerminalSession(customers, products, orders,
                new InputStreamReader(System.in, StandardCharsets.UTF_8),
                new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
        try {
            session.run();
        } catch (IOException exception) {
            System.err.println("Falha de entrada/saída no terminal: " + exception.getMessage());
        }
    }
}
