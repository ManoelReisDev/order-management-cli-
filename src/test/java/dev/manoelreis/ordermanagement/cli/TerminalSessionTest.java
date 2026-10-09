package dev.manoelreis.ordermanagement.cli;

import dev.manoelreis.ordermanagement.cli.menu.TerminalSession;

import dev.manoelreis.ordermanagement.application.customer.CustomerApplicationService;
import dev.manoelreis.ordermanagement.application.order.OrderApplicationService;
import dev.manoelreis.ordermanagement.application.product.ProductApplicationService;
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
import java.io.Writer;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalSessionTest {

    private CustomerApplicationService customers;
    private ProductApplicationService products;
    private OrderApplicationService orders;
    private StringWriter output;

    @BeforeEach
    void setUp() {
        InMemoryCustomerRepository customerRepository = new InMemoryCustomerRepository();
        customers = new CustomerApplicationService(customerRepository);
        products = new ProductApplicationService(new InMemoryProductRepository());
        orders = new OrderApplicationService(new InMemoryOrderRepository(), customerRepository,
                Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC));
        output = new StringWriter();
    }

    @Test
    void shouldShowMenuAndStopWithoutFurtherReadsOrClosingResources() throws IOException {
        Reader input = new Reader() {
            private int position;

            @Override
            public int read(char[] buffer, int offset, int length) {
                assertTrue(position < 2, "Must not read after exit");
                assertTrue(output.toString().contains("Escolha uma opção"));
                buffer[offset] = "0\n".charAt(position++);
                return 1;
            }

            @Override
            public void close() {
                throw new AssertionError("Caller owns input");
            }
        };
        StringWriter trackedOutput = new StringWriter() {
            @Override
            public void close() {
                throw new AssertionError("Caller owns output");
            }
        };
        output = trackedOutput;
        TerminalSession session = new TerminalSession(customers, products, orders, input, output);

        session.run();
        String firstRun = output.toString();
        session.run();

        assertEquals(firstRun, output.toString());
        assertTrue(firstRun.contains("somente em memória"));
        for (String operation : new String[]{"[1] Cadastrar cliente", "[2] Cadastrar produto",
                "[3] Listar clientes", "[4] Listar produtos", "[5] Buscar cliente por ID",
                "[6] Buscar produto por ID", "[7] Criar pedido", "[8] Listar pedidos",
                "[9] Buscar pedido por ID", "[10] Adicionar produto ao pedido",
                "[11] Alterar quantidade de item", "[12] Remover item do pedido", "[0] Sair"}) {
            assertTrue(firstRun.contains(operation));
        }
        assertEquals(1, occurrences(firstRun, "Escolha uma opção"));
        assertTrue(firstRun.contains("Sessão encerrada"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1\n", "1\nAna\n", "2\n", "2\nTeclado\n", "2\nTeclado\n\n",
            "5\n", "6\n"})
    void shouldStopAtEofWithoutPartialRegistration(String input) throws IOException {
        String result = run(input);

        assertTrue(customers.listCustomers().isEmpty());
        assertTrue(products.listProducts().isEmpty());
        assertTrue(result.contains("Sessão encerrada"));
        assertFalse(result.contains("inválid"));
        assertEquals(1, occurrences(result, "Escolha uma opção"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "13", "-1", "abc", "1.0", "999999999999999999999999999"})
    void shouldRecoverFromInvalidOptionIncludingEmptyLine(String option) throws IOException {
        String result = run(option + "\n3\n0\n");

        assertTrue(result.contains("Opção inválida"));
        assertTrue(result.contains("Nenhum cliente cadastrado"));
        assertEquals(3, occurrences(result, "Escolha uma opção"));
    }

    @Test
    void shouldRegisterAndListCustomersThenFindUsingSameServices() throws IOException {
        String registration = run(" 1 \n  Ana  \n  ana@example.com  \n3\n0\n");
        var customer = customers.listCustomers().getFirst();

        assertEquals("Ana", customer.getName());
        assertEquals("ana@example.com", customer.getEmail());
        assertTrue(registration.contains("Cliente cadastrado com sucesso"));
        assertEquals(2, occurrences(registration, customer.getId().toString()));
        assertEquals(2, occurrences(registration, "Nome: Ana | E-mail: ana@example.com"));

        String lookup = run("5\n  " + customer.getId().toString().toUpperCase(Locale.ROOT) + "  \n0\n");
        assertTrue(lookup.contains(customer.getId().toString()));
        assertTrue(lookup.contains("Nome: Ana | E-mail: ana@example.com"));
        assertFalse(lookup.contains("inválido"));
    }

    @Test
    void shouldRegisterAndListProductsThenFindUsingSameServices() throws IOException {
        String registration = run("2\n  Teclado  \n  Mecânico  \n +100.5 \n4\n0\n");
        var product = products.listProducts().getFirst();

        assertEquals("Teclado", product.getName());
        assertEquals("Mecânico", product.getDescription());
        assertEquals(new BigDecimal("100.50"), product.getPrice());
        assertTrue(registration.contains("Produto cadastrado com sucesso"));
        assertEquals(2, occurrences(registration, product.getId().toString()));
        assertEquals(2, occurrences(registration, "Nome: Teclado | Descrição: Mecânico | Preço: 100.50"));

        String lookup = run("6\n  " + product.getId().toString().toUpperCase(Locale.ROOT) + "  \n0\n");
        assertTrue(lookup.contains(product.getId().toString()));
        assertTrue(lookup.contains("Nome: Teclado | Descrição: Mecânico | Preço: 100.50"));
        assertFalse(lookup.contains("inválido"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"100", "100.00", " +100.00 ", "100.000", "000100.00"})
    void shouldAllowEmptyDescriptionAndDomainAcceptedPrice(String price) throws IOException {
        String result = run("2\nTeclado\n\n" + price + "\n4\n0\n");

        assertEquals("", products.listProducts().getFirst().getDescription());
        assertEquals(new BigDecimal("100.00"), products.listProducts().getFirst().getPrice());
        assertTrue(result.contains("Descrição:  | Preço: 100.00"));
    }

    @Test
    void shouldReportEmptyLists() throws IOException {
        String result = run("3\n4\n0\n");

        assertTrue(result.contains("Nenhum cliente cadastrado"));
        assertTrue(result.contains("Nenhum produto cadastrado"));
    }

    @Test
    void shouldPreserveServiceOrderInLists() throws IOException {
        var firstCustomer = customers.registerCustomer("Ana", "ana@example.com");
        var secondCustomer = customers.registerCustomer("Bruno", "bruno@example.com");
        var firstProduct = products.registerProduct("Teclado", "Mecânico", new BigDecimal("100"));
        var secondProduct = products.registerProduct("Mouse", "Sem fio", new BigDecimal("50"));

        String result = run("3\n4\n0\n");

        assertTrue(result.indexOf(firstCustomer.getId().toString()) < result.indexOf(secondCustomer.getId().toString()));
        assertTrue(result.indexOf(firstProduct.getId().toString()) < result.indexOf(secondProduct.getId().toString()));
        assertTrue(result.contains("Nome: Bruno | E-mail: bruno@example.com"));
        assertTrue(result.contains("Nome: Mouse | Descrição: Sem fio | Preço: 50.00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid", "1-1-1-1-1", "12345678-1234-1234-1234-12345678901",
            "12345678-1234-1234-1234-1234567890123", "12345678123412341234123456789012",
            "gggggggg-1234-1234-1234-123456789012", "{12345678-1234-1234-1234-123456789012}"})
    void shouldRejectMalformedIdsAndContinue(String id) throws IOException {
        String result = run("5\n" + id + "\n6\n" + id + "\n3\n0\n");

        assertEquals(2, occurrences(result, "UUID inválido"));
        assertTrue(result.contains("Nenhum cliente cadastrado"));
        assertFalse(result.contains("não encontrado"));
    }

    @Test
    void shouldReportMissingRecordsAndContinueToRegistration() throws IOException {
        String id = UUID.randomUUID().toString();
        String result = run("5\n" + id + "\n6\n" + id + "\n1\nAna\nana@example.com\n0\n");

        assertTrue(result.contains("Cliente não encontrado: " + id));
        assertTrue(result.contains("Produto não encontrado: " + id));
        assertTrue(result.contains("Cliente cadastrado com sucesso"));
        assertEquals(1, customers.listCustomers().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1,00", "1,000.00", "1.000,00", "$1.00", "R$ 1.00", "1e2", "1E+2",
            ".50", "100.", "+", "NaN", "1 000", "--1"})
    void shouldRejectMalformedPricesAndContinue(String price) throws IOException {
        String result = run("2\nTeclado\n\n" + price + "\n4\n0\n");

        assertTrue(products.listProducts().isEmpty());
        assertTrue(result.contains("Formato de preço inválido"));
        assertTrue(result.contains("Nenhum produto cadastrado"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-0.00", "0.001", "1.234"})
    void shouldPresentDomainPriceErrorsAndContinue(String price) throws IOException {
        String result = run("2\nTeclado\n\n" + price + "\n4\n0\n");

        assertTrue(products.listProducts().isEmpty());
        assertTrue(result.contains("Dados de produto inválidos"));
        assertFalse(result.contains("Formato de preço inválido"));
        assertTrue(result.contains("Nenhum produto cadastrado"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\nana@example.com", "Ana\n", "Ana\ninvalid"})
    void shouldPresentCustomerDomainErrorsWithoutSaving(String fields) throws IOException {
        String result = run("1\n" + fields + "\n3\n0\n");

        assertTrue(customers.listCustomers().isEmpty());
        assertTrue(result.contains("Dados de cliente inválidos"));
        assertTrue(result.contains("Nenhum cliente cadastrado"));
    }

    @Test
    void shouldPresentProductNameErrorWithoutSaving() throws IOException {
        String result = run("2\n \nDescrição\n100\n4\n0\n");

        assertTrue(products.listProducts().isEmpty());
        assertTrue(result.contains("Dados de produto inválidos"));
        assertTrue(result.contains("Nenhum produto cadastrado"));
    }

    @Test
    void shouldIsolateSeparatelyComposedSessions() throws IOException {
        run("1\nAna\nana@example.com\n2\nTeclado\n\n100\n0\n");
        var newProducts = new ProductApplicationService(new InMemoryProductRepository());
        StringWriter newOutput = new StringWriter();

        InMemoryCustomerRepository newCustomerRepository = new InMemoryCustomerRepository();
        var newCustomers = new CustomerApplicationService(newCustomerRepository);
        var newOrders = new OrderApplicationService(new InMemoryOrderRepository(), newCustomerRepository,
                Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC));

        new TerminalSession(newCustomers, newProducts, newOrders,
                new StringReader("3\n4\n8\n0\n"), newOutput).run();

        assertTrue(newCustomers.listCustomers().isEmpty());
        assertTrue(newProducts.listProducts().isEmpty());
        assertTrue(newOutput.toString().contains("Nenhum cliente cadastrado"));
        assertTrue(newOutput.toString().contains("Nenhum produto cadastrado"));
        assertTrue(newOutput.toString().contains("Nenhum pedido cadastrado"));
        assertEquals(1, customers.listCustomers().size());
        assertEquals(1, products.listProducts().size());
    }

    @Test
    void shouldRejectNullDependencies() {
        assertThrows(IllegalArgumentException.class,
                () -> new TerminalSession(null, products, orders, new StringReader(""), output));
        assertThrows(IllegalArgumentException.class,
                () -> new TerminalSession(customers, null, orders, new StringReader(""), output));
        assertThrows(IllegalArgumentException.class,
                () -> new TerminalSession(customers, products, null, new StringReader(""), output));
        assertThrows(IllegalArgumentException.class,
                () -> new TerminalSession(customers, products, orders, null, output));
        assertThrows(IllegalArgumentException.class,
                () -> new TerminalSession(customers, products, orders, new StringReader(""), null));
    }

    @Test
    void shouldPropagateReadFailureWithoutRetryingOrTreatingItAsEof() throws IOException {
        Reader failingInput = new Reader() {
            private int attempts;

            @Override
            public int read(char[] buffer, int offset, int length) throws IOException {
                assertEquals(1, ++attempts);
                throw new IOException("Read failure");
            }

            @Override
            public void close() {
                throw new AssertionError("Caller owns input");
            }
        };
        TerminalSession session = new TerminalSession(customers, products, orders, failingInput, output);

        IOException failure = assertThrows(IOException.class, session::run);
        session.run();

        assertEquals("Read failure", failure.getMessage());
        assertFalse(output.toString().contains("Sessão encerrada"));
        assertFalse(output.toString().contains("inválid"));
    }

    @Test
    void shouldFlushPromptBeforeReading() throws IOException {
        StringBuilder flushed = new StringBuilder();
        Writer bufferedOutput = new Writer() {
            private final StringBuilder pending = new StringBuilder();

            @Override
            public void write(char[] buffer, int offset, int length) {
                pending.append(buffer, offset, length);
            }

            @Override
            public void flush() {
                flushed.append(pending);
                pending.setLength(0);
            }

            @Override
            public void close() {
                throw new AssertionError("Caller owns output");
            }
        };
        Reader input = new StringReader("0\n") {
            @Override
            public int read(char[] buffer, int offset, int length) throws IOException {
                assertTrue(flushed.toString().contains("Escolha uma opção"));
                return super.read(buffer, offset, length);
            }
        };

        new TerminalSession(customers, products, orders, input, bufferedOutput).run();

        assertTrue(flushed.toString().contains("Sessão encerrada"));
    }

    @Test
    void shouldPropagateWriteFailureWithoutReading() {
        Writer failingOutput = new Writer() {
            @Override
            public void write(char[] buffer, int offset, int length) throws IOException {
                throw new IOException("Write failure");
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Reader unreadInput = new Reader() {
            @Override
            public int read(char[] buffer, int offset, int length) {
                throw new AssertionError("Must not read after write failure");
            }

            @Override
            public void close() {
            }
        };
        TerminalSession session = new TerminalSession(customers, products, orders, unreadInput, failingOutput);

        assertEquals("Write failure", assertThrows(IOException.class, session::run).getMessage());
    }

    private String run(String input) throws IOException {
        output = new StringWriter();
        new TerminalSession(customers, products, orders, new StringReader(input), output).run();
        return output.toString();
    }

    private static int occurrences(String text, String fragment) {
        return (text.length() - text.replace(fragment, "").length()) / fragment.length();
    }
}
