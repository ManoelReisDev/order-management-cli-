package dev.manoelreis.ordermanagement.cli.menu;

import dev.manoelreis.ordermanagement.application.customer.CustomerApplicationService;
import dev.manoelreis.ordermanagement.application.customer.CustomerNotFoundException;
import dev.manoelreis.ordermanagement.application.product.ProductApplicationService;
import dev.manoelreis.ordermanagement.application.product.ProductNotFoundException;
import dev.manoelreis.ordermanagement.cli.input.TerminalInput;
import dev.manoelreis.ordermanagement.cli.output.TerminalOutput;
import dev.manoelreis.ordermanagement.domain.customer.Customer;
import dev.manoelreis.ordermanagement.domain.customer.CustomerId;
import dev.manoelreis.ordermanagement.domain.exception.InvalidCustomerException;
import dev.manoelreis.ordermanagement.domain.exception.InvalidProductException;
import dev.manoelreis.ordermanagement.domain.product.Product;
import dev.manoelreis.ordermanagement.domain.product.ProductId;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.UUID;

public final class TerminalSession {

    private final CustomerApplicationService customers;
    private final ProductApplicationService products;
    private final TerminalInput input;
    private final TerminalOutput output;
    private boolean finished;

    public TerminalSession(CustomerApplicationService customers, ProductApplicationService products,
                           Reader input, Writer output) {
        if (customers == null || products == null || input == null || output == null) {
            throw new IllegalArgumentException("Services, input and output are required");
        }
        this.customers = customers;
        this.products = products;
        this.input = new TerminalInput(input);
        this.output = new TerminalOutput(output);
    }

    public void run() throws IOException {
        if (finished) {
            return;
        }
        try {
            output.write("Os dados são mantidos somente em memória e serão perdidos ao encerrar.\n");
            boolean active = true;
            while (active) {
                output.showMenu();
                String option = read("Escolha uma opção: ");
                if (option == null) {
                    break;
                }
                switch (option.trim()) {
                    case "0" -> active = false;
                    case "1" -> active = registerCustomer();
                    case "2" -> active = registerProduct();
                    case "3" -> listCustomers();
                    case "4" -> listProducts();
                    case "5" -> active = findCustomer();
                    case "6" -> active = findProduct();
                    default -> output.write("Opção inválida. Escolha um código de 0 a 6.\n");
                }
            }
            output.write("Sessão encerrada.\n");
        } finally {
            finished = true;
        }
    }

    private boolean registerCustomer() throws IOException {
        String name = read("Nome do cliente: ");
        if (name == null) {
            return false;
        }
        String email = read("E-mail: ");
        if (email == null) {
            return false;
        }
        try {
            Customer customer = customers.registerCustomer(name, email);
            output.write("Cliente cadastrado com sucesso.\n");
            output.showCustomer(customer);
        } catch (InvalidCustomerException exception) {
            output.write("Dados de cliente inválidos.\n");
        }
        return true;
    }

    private boolean registerProduct() throws IOException {
        String name = read("Nome do produto: ");
        if (name == null) {
            return false;
        }
        String description = read("Descrição (pode ser vazia): ");
        if (description == null) {
            return false;
        }
        String text = read("Preço (use ponto decimal, por exemplo 100.00): ");
        if (text == null) {
            return false;
        }
        var parsedPrice = TerminalInput.parsePrice(text);
        if (parsedPrice.isEmpty()) {
            output.write("Formato de preço inválido. Use dígitos e ponto decimal, por exemplo 100.00.\n");
            return true;
        }
        BigDecimal price = parsedPrice.get();
        try {
            Product product = products.registerProduct(name, description, price);
            output.write("Produto cadastrado com sucesso.\n");
            output.showProduct(product);
        } catch (InvalidProductException exception) {
            output.write("Dados de produto inválidos. Informe um nome e preço positivo com até duas casas decimais.\n");
        }
        return true;
    }

    private boolean findCustomer() throws IOException {
        String text = read("ID do cliente (UUID completo): ");
        if (text == null) {
            return false;
        }
        UUID id = parseId(text);
        if (id != null) {
            try {
                output.showCustomer(customers.findCustomerById(CustomerId.of(id)));
            } catch (CustomerNotFoundException exception) {
                output.write("Cliente não encontrado: " + id + "\n");
            }
        }
        return true;
    }

    private boolean findProduct() throws IOException {
        String text = read("ID do produto (UUID completo): ");
        if (text == null) {
            return false;
        }
        UUID id = parseId(text);
        if (id != null) {
            try {
                output.showProduct(products.findProductById(ProductId.of(id)));
            } catch (ProductNotFoundException exception) {
                output.write("Produto não encontrado: " + id + "\n");
            }
        }
        return true;
    }

    private UUID parseId(String text) throws IOException {
        var parsedId = TerminalInput.parseId(text);
        if (parsedId.isEmpty()) {
            output.write("UUID inválido. Informe o identificador completo no formato 8-4-4-4-12.\n");
            return null;
        }
        return parsedId.get();
    }

    private void listCustomers() throws IOException {
        var records = customers.listCustomers();
        if (records.isEmpty()) {
            output.write("Nenhum cliente cadastrado.\n");
        }
        for (Customer customer : records) {
            output.showCustomer(customer);
        }
    }

    private void listProducts() throws IOException {
        var records = products.listProducts();
        if (records.isEmpty()) {
            output.write("Nenhum produto cadastrado.\n");
        }
        for (Product product : records) {
            output.showProduct(product);
        }
    }

    private String read(String prompt) throws IOException {
        output.write(prompt);
        return input.readLine();
    }
}
