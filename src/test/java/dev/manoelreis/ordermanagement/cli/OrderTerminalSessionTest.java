package dev.manoelreis.ordermanagement.cli;

import dev.manoelreis.ordermanagement.application.customer.CustomerApplicationService;
import dev.manoelreis.ordermanagement.application.order.OrderApplicationService;
import dev.manoelreis.ordermanagement.application.product.ProductApplicationService;
import dev.manoelreis.ordermanagement.cli.input.TerminalInput;
import dev.manoelreis.ordermanagement.cli.menu.TerminalSession;
import dev.manoelreis.ordermanagement.domain.order.Order;
import dev.manoelreis.ordermanagement.domain.order.OrderStatus;
import dev.manoelreis.ordermanagement.domain.payment.PaymentResult;
import dev.manoelreis.ordermanagement.infrastructure.persistence.memory.InMemoryCustomerRepository;
import dev.manoelreis.ordermanagement.infrastructure.persistence.memory.InMemoryOrderRepository;
import dev.manoelreis.ordermanagement.infrastructure.persistence.memory.InMemoryProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTerminalSessionTest {

    private static final Instant NOW = Instant.parse("2026-04-05T14:30:00Z");

    private CustomerApplicationService customers;
    private ProductApplicationService products;
    private OrderApplicationService orders;

    @BeforeEach
    void setUp() {
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        customers = new CustomerApplicationService(customerRepository);
        products = new ProductApplicationService(new InMemoryProductRepository());
        orders = new OrderApplicationService(new InMemoryOrderRepository(), customerRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldRegisterCustomerAndCreateOrderInTheSameSession() throws IOException {
        StringWriter output = new StringWriter();
        StringBuilder script = new StringBuilder("1\nAna\nana@example.com\n7\n");
        Reader input = new Reader() {
            private int position;
            private boolean completed;

            @Override
            public int read(char[] buffer, int offset, int length) {
                if (position == script.length() && !completed) {
                    script.append(customers.listCustomers().getFirst().getId()).append("\n8\n0\n");
                    completed = true;
                }
                if (position == script.length()) {
                    return -1;
                }
                buffer[offset] = script.charAt(position++);
                return 1;
            }

            @Override
            public void close() {
                throw new AssertionError("Caller owns input");
            }
        };

        new TerminalSession(customers, products, orders, input, output).run();

        Order order = orders.listOrders().getFirst();
        assertEquals(customers.listCustomers().getFirst().getId(), order.getCustomerId());
        assertEquals(NOW, order.getCreatedAt());
        assertEquals(OrderStatus.DRAFT, order.getStatus());
        assertTrue(order.getItems().isEmpty());
        assertEquals(new BigDecimal("0.00"), order.getSubtotal());
        assertTrue(output.toString().contains("Pedido criado com sucesso"));
        assertEquals(2, occurrences(output.toString(), "Pedido: " + order.getId()));
        assertTrue(output.toString().contains("Criado em: 2026-04-05T14:30:00Z"));
        assertTrue(output.toString().contains("Status: DRAFT | Subtotal: 0.00"));
        assertTrue(output.toString().contains("Itens: nenhum item"));
    }

    @Test
    void shouldFindAndListOrdersInServiceOrder() throws IOException {
        var customer = customers.registerCustomer("Ana", "ana@example.com");
        Order first = orders.createOrder(customer.getId());
        Order second = orders.createOrder(customer.getId());

        String result = run("9\n  " + first.getId().toString().toUpperCase(Locale.ROOT)
                + "  \n8\n0\n");

        assertEquals(2, occurrences(result, "Pedido: " + first.getId()));
        assertEquals(1, occurrences(result, "Pedido: " + second.getId()));
        assertTrue(result.lastIndexOf(first.getId().toString()) < result.lastIndexOf(second.getId().toString()));
        assertFalse(result.contains("UUID inválido"));
    }

    @Test
    void shouldAddAccumulateChangeAndRemoveOrderItem() throws IOException {
        var customer = customers.registerCustomer("Ana", "ana@example.com");
        var product = products.registerProduct("Teclado", "Mecânico", new BigDecimal("125.50"));
        Order order = orders.createOrder(customer.getId());
        String orderId = order.getId().toString();
        String productId = product.getId().toString();

        String result = run("10\n" + orderId + "\n" + productId + "\n2\n"
                + "10\n" + orderId + "\n" + productId + "\n +3 \n"
                + "11\n" + orderId + "\n" + productId + "\n4\n"
                + "12\n" + orderId + "\n" + productId + "\n0\n");

        assertTrue(order.getItems().isEmpty());
        assertEquals(new BigDecimal("0.00"), order.getSubtotal());
        assertTrue(result.contains("Quantidade: 5 | Subtotal do item: 627.50"));
        assertTrue(result.contains("Quantidade: 4 | Subtotal do item: 502.00"));
        assertTrue(result.contains("Produto adicionado ao pedido com sucesso"));
        assertTrue(result.contains("Quantidade do item alterada com sucesso"));
        assertTrue(result.contains("Item removido do pedido com sucesso"));
        assertTrue(result.contains("Itens: nenhum item"));
    }

    @Test
    void shouldDisplayStoredItemSnapshotAfterProductChanges() throws IOException {
        var customer = customers.registerCustomer("Ana", "ana@example.com");
        var product = products.registerProduct("Teclado", "Original", new BigDecimal("100.00"));
        Order order = orders.createOrder(customer.getId());
        orders.addProductToOrder(order.getId(), product, 2);
        products.renameProduct(product.getId(), "Teclado novo");
        products.changeProductPrice(product.getId(), new BigDecimal("250.00"));

        String result = run("9\n" + order.getId() + "\n0\n");

        assertTrue(result.contains("Produto: Teclado | Preço unitário: 100.00"));
        assertTrue(result.contains("Subtotal do item: 200.00"));
        assertFalse(result.contains("Produto: Teclado novo"));
    }

    @Test
    void shouldReportMissingRecordsAndKeepSessionActive() throws IOException {
        var customer = customers.registerCustomer("Ana", "ana@example.com");
        Order order = orders.createOrder(customer.getId());
        String missingOrder = UUID.randomUUID().toString();
        String missingProduct = UUID.randomUUID().toString();

        String result = run("7\n" + UUID.randomUUID() + "\n"
                + "9\n" + missingOrder + "\n"
                + "10\n" + missingOrder + "\n" + missingProduct + "\n1\n"
                + "11\n" + order.getId() + "\n" + missingProduct + "\n1\n"
                + "12\n" + order.getId() + "\n" + missingProduct + "\n8\n0\n");

        assertEquals(1, orders.listOrders().size());
        assertTrue(order.getItems().isEmpty());
        assertTrue(result.contains("Cliente não encontrado"));
        assertTrue(result.contains("Pedido não encontrado: " + missingOrder));
        assertTrue(result.contains("Produto não encontrado: " + missingProduct));
        assertEquals(2, occurrences(result, "não identifica um item deste pedido"));
        assertTrue(result.contains("Pedido: " + order.getId()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "1.0", "1,0", "1e2", "+", "1 0",
            "2147483648", "-2147483649", "999999999999999999999"})
    void shouldRejectMalformedQuantitiesWithoutChangingOrder(String quantity) throws IOException {
        var customer = customers.registerCustomer("Ana", "ana@example.com");
        var product = products.registerProduct("Mouse", "", new BigDecimal("50.00"));
        Order order = orders.createOrder(customer.getId());

        String result = run("10\n" + order.getId() + "\n" + product.getId() + "\n"
                + quantity + "\n8\n0\n");

        assertTrue(order.getItems().isEmpty());
        assertTrue(result.contains("Formato de quantidade inválido"));
        assertTrue(result.contains("Pedido: " + order.getId()));
    }

    @Test
    void shouldSendRepresentableNonPositiveQuantitiesToDomain() throws IOException {
        var customer = customers.registerCustomer("Ana", "ana@example.com");
        var product = products.registerProduct("Mouse", "", new BigDecimal("50.00"));
        Order order = orders.createOrder(customer.getId());

        String result = run("10\n" + order.getId() + "\n" + product.getId() + "\n0\n"
                + "10\n" + order.getId() + "\n" + product.getId() + "\n-1\n0\n");

        assertTrue(order.getItems().isEmpty());
        assertEquals(2, occurrences(result, "Quantidade inválida"));
        assertFalse(result.contains("Formato de quantidade inválido"));
    }

    @Test
    void shouldRejectAccumulationOverflowWithoutChangingItem() throws IOException {
        var customer = customers.registerCustomer("Ana", "ana@example.com");
        var product = products.registerProduct("Mouse", "", new BigDecimal("1.00"));
        Order order = orders.createOrder(customer.getId());
        orders.addProductToOrder(order.getId(), product, Integer.MAX_VALUE);

        String result = run("10\n" + order.getId() + "\n" + product.getId() + "\n1\n0\n");

        assertEquals(Integer.MAX_VALUE, order.getItems().getFirst().getQuantity());
        assertTrue(result.contains("Quantidade inválida: 1"));
    }

    @Test
    void shouldRejectAllItemChangesWhenOrderIsPaid() throws IOException {
        var customer = customers.registerCustomer("Ana", "ana@example.com");
        var product = products.registerProduct("Mouse", "", new BigDecimal("50.00"));
        Order order = orders.createOrder(customer.getId());
        orders.addProductToOrder(order.getId(), product, 2);
        order.pay(amount -> PaymentResult.APPROVED);

        String result = run("10\n" + order.getId() + "\n" + product.getId() + "\n1\n"
                + "11\n" + order.getId() + "\n" + product.getId() + "\n5\n"
                + "12\n" + order.getId() + "\n" + product.getId() + "\n0\n");

        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(2, order.getItems().getFirst().getQuantity());
        assertEquals(3, occurrences(result, "Operação rejeitada pelo estado do pedido"));
    }

    @Test
    void shouldStopAtEofInEveryOrderFlowWithoutPartialMutation() throws IOException {
        var customer = customers.registerCustomer("Ana", "ana@example.com");
        var product = products.registerProduct("Mouse", "", new BigDecimal("50.00"));
        Order order = orders.createOrder(customer.getId());
        String orderId = order.getId().toString();
        String productId = product.getId().toString();
        String[] incompleteInputs = {
                "7\n", "9\n", "10\n", "10\n" + orderId + "\n",
                "10\n" + orderId + "\n" + productId + "\n",
                "11\n", "11\n" + orderId + "\n",
                "11\n" + orderId + "\n" + productId + "\n",
                "12\n", "12\n" + orderId + "\n"
        };

        for (String input : incompleteInputs) {
            String result = run(input);
            assertTrue(result.contains("Sessão encerrada"));
            assertFalse(result.contains("inválid"));
            assertTrue(order.getItems().isEmpty());
            assertEquals(1, orders.listOrders().size());
        }
    }

    @Test
    void shouldAcceptIntegerLimitsAndExternalSpaces() {
        assertEquals(Integer.MAX_VALUE, TerminalInput.parseQuantity("  +2147483647 ").orElseThrow());
        assertEquals(Integer.MIN_VALUE, TerminalInput.parseQuantity(" -2147483648 ").orElseThrow());
    }

    private String run(String script) throws IOException {
        StringWriter output = new StringWriter();
        new TerminalSession(customers, products, orders, new StringReader(script), output).run();
        return output.toString();
    }

    private static int occurrences(String text, String fragment) {
        return (text.length() - text.replace(fragment, "").length()) / fragment.length();
    }
}
