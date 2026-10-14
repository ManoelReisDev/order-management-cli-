package dev.manoelreis.ordermanagement.cli.output;

import dev.manoelreis.ordermanagement.domain.customer.Customer;
import dev.manoelreis.ordermanagement.domain.order.Order;
import dev.manoelreis.ordermanagement.domain.order.OrderItem;
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
            ║  PEDIDOS                         ║
            ║  [7] Criar pedido                ║
            ║  [8] Listar pedidos              ║
            ║  [9] Buscar pedido por ID        ║
            ║  [10] Adicionar produto ao pedido║
            ║  [11] Alterar quantidade de item ║
            ║  [12] Remover item do pedido     ║
            ║  [13] Processar pagamento        ║
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

    public void showOrder(Order order) throws IOException {
        write("Pedido: " + order.getId()
                + " | Cliente: " + order.getCustomerId()
                + " | Criado em: " + order.getCreatedAt()
                + " | Status: " + order.getStatus()
                + " | Subtotal: " + order.getSubtotal().toPlainString() + "\n");
        if (order.getItems().isEmpty()) {
            write("Itens: nenhum item.\n");
            return;
        }
        for (OrderItem item : order.getItems()) {
            write("Item: " + item.getProductId()
                    + " | Produto: " + item.getProductName()
                    + " | Preço unitário: " + item.getUnitPrice().toPlainString()
                    + " | Quantidade: " + item.getQuantity()
                    + " | Subtotal do item: " + item.getSubtotal().toPlainString() + "\n");
        }
    }

    public void write(String text) throws IOException {
        writer.write(text);
        writer.flush();
    }
}
