package dev.manoelreis.ordermanagement.cli.output;

import dev.manoelreis.ordermanagement.domain.customer.Customer;
import dev.manoelreis.ordermanagement.domain.product.Product;

import java.io.IOException;
import java.io.Writer;

public final class TerminalOutput {

    private static final String MENU = """
            ╔══════════════════════════════════╗
            ║         SISTEMA DE GESTÃO        ║
            ╠══════════════════════════════════╣
            ║  CLIENTES                        ║
            ║  [1] Cadastrar cliente           ║
            ║  [3] Listar clientes             ║
            ║  [5] Buscar cliente por ID       ║
            ║                                  ║
            ║  PRODUTOS                        ║
            ║  [2] Cadastrar produto           ║
            ║  [4] Listar produtos             ║
            ║  [6] Buscar produto por ID       ║
            ║                                  ║
            ║  [0] Sair                        ║
            ╚══════════════════════════════════╝
            """;

    private final Writer writer;

    public TerminalOutput(Writer writer) {
        if (writer == null) {
            throw new IllegalArgumentException("Output is required");
        }
        this.writer = writer;
    }

    public void showMenu() throws IOException {
        write(MENU);
    }

    public void showCustomer(Customer customer) throws IOException {
        write("ID: " + customer.getId() + " | Nome: " + customer.getName()
                + " | E-mail: " + customer.getEmail() + "\n");
    }

    public void showProduct(Product product) throws IOException {
        write("ID: " + product.getId() + " | Nome: " + product.getName()
                + " | Descrição: " + product.getDescription()
                + " | Preço: " + product.getPrice().toPlainString() + "\n");
    }

    public void write(String text) throws IOException {
        writer.write(text);
        writer.flush();
    }
}
