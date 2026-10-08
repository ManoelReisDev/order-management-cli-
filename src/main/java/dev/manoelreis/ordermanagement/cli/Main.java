package dev.manoelreis.ordermanagement.cli;

import dev.manoelreis.ordermanagement.cli.menu.TerminalSession;

import dev.manoelreis.ordermanagement.application.customer.CustomerApplicationService;
import dev.manoelreis.ordermanagement.application.product.ProductApplicationService;
import dev.manoelreis.ordermanagement.infrastructure.persistence.memory.InMemoryCustomerRepository;
import dev.manoelreis.ordermanagement.infrastructure.persistence.memory.InMemoryProductRepository;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        CustomerApplicationService customers = new CustomerApplicationService(new InMemoryCustomerRepository());
        ProductApplicationService products = new ProductApplicationService(new InMemoryProductRepository());
        TerminalSession session = new TerminalSession(customers, products,
                new InputStreamReader(System.in, StandardCharsets.UTF_8),
                new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
        try {
            session.run();
        } catch (IOException exception) {
            System.err.println("Falha de entrada/saída no terminal: " + exception.getMessage());
        }
    }
}
