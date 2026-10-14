package dev.manoelreis.ordermanagement.cli;

import dev.manoelreis.ordermanagement.cli.menu.TerminalSession;

import dev.manoelreis.ordermanagement.application.customer.CustomerApplicationService;
import dev.manoelreis.ordermanagement.application.order.OrderApplicationService;
import dev.manoelreis.ordermanagement.application.product.ProductApplicationService;
import dev.manoelreis.ordermanagement.application.payment.PaymentApplicationService;
import dev.manoelreis.ordermanagement.infrastructure.persistence.json.JsonCustomerRepository;
import dev.manoelreis.ordermanagement.infrastructure.persistence.json.JsonOrderRepository;
import dev.manoelreis.ordermanagement.infrastructure.persistence.json.JsonPersistenceException;
import dev.manoelreis.ordermanagement.infrastructure.persistence.json.JsonProductRepository;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            JsonCustomerRepository customerRepository = new JsonCustomerRepository(Path.of("data/customers.json"));
            JsonProductRepository productRepository = new JsonProductRepository(Path.of("data/products.json"));
            JsonOrderRepository orderRepository = new JsonOrderRepository(Path.of("data/orders.json"));
            CustomerApplicationService customers = new CustomerApplicationService(customerRepository);
            ProductApplicationService products = new ProductApplicationService(productRepository);
            OrderApplicationService orders = new OrderApplicationService(orderRepository, customerRepository, Clock.systemUTC());
            PaymentApplicationService payments = new PaymentApplicationService(orderRepository);
            TerminalSession session = new TerminalSession(customers, products, orders, payments,
                    new InputStreamReader(System.in, StandardCharsets.UTF_8),
                    new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
            session.run();
        } catch (JsonPersistenceException exception) {
            System.err.println("Falha de persistência: " + exception.getMessage());
        } catch (IOException exception) {
            System.err.println("Falha de entrada/saída no terminal: " + exception.getMessage());
        }
    }
}
