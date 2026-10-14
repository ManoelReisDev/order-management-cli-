package dev.manoelreis.ordermanagement.infrastructure.persistence.json;

import dev.manoelreis.ordermanagement.domain.customer.Customer;
import dev.manoelreis.ordermanagement.domain.customer.CustomerId;
import dev.manoelreis.ordermanagement.domain.order.Order;
import dev.manoelreis.ordermanagement.domain.order.OrderId;
import dev.manoelreis.ordermanagement.domain.order.OrderStatus;
import dev.manoelreis.ordermanagement.domain.payment.PaymentResult;
import dev.manoelreis.ordermanagement.domain.product.Product;
import dev.manoelreis.ordermanagement.domain.product.ProductId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonPersistenceTest {

    @Test
    void shouldRoundTripEntitiesAndHistoricalPaidOrder(@TempDir Path directory) {
        JsonCustomerRepository customers = new JsonCustomerRepository(directory.resolve("nested/customers.json"));
        JsonProductRepository products = new JsonProductRepository(directory.resolve("products.json"));
        JsonOrderRepository orders = new JsonOrderRepository(directory.resolve("orders.json"));
        Customer customer = new Customer(CustomerId.newId(), " Ada ", "ada@example.com");
        Product product = new Product(ProductId.newId(), "Keyboard", "Mechanical", new BigDecimal("100.00"));
        Order order = new Order(OrderId.newId(), customer.getId(), Instant.parse("2026-01-01T10:00:00.123456789Z"));
        order.addProduct(product, 2);
        order.pay(amount -> PaymentResult.APPROVED);

        customers.save(customer);
        products.save(product);
        orders.save(order);

        Order restored = new JsonOrderRepository(directory.resolve("orders.json")).findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.PAID, restored.getStatus());
        assertEquals(order.getCreatedAt(), restored.getCreatedAt());
        assertEquals(new BigDecimal("200.00"), restored.getSubtotal());
        assertEquals("Keyboard", restored.getItems().getFirst().getProductName());
        assertEquals(customer.getName(), new JsonCustomerRepository(directory.resolve("nested/customers.json"))
                .findById(customer.getId()).orElseThrow().getName());
        assertEquals(product.getPrice(), new JsonProductRepository(directory.resolve("products.json"))
                .findById(product.getId()).orElseThrow().getPrice());
    }

    @Test
    void shouldTreatMissingFilesAsEmptyAndCreateParentsOnFirstSave(@TempDir Path directory) {
        Path file = directory.resolve("a/b/products.json");
        JsonProductRepository repository = new JsonProductRepository(file);
        assertTrue(repository.findAll().isEmpty());
        assertTrue(Files.notExists(file));
        repository.save(new Product(ProductId.newId(), "Mouse", "", new BigDecimal("1.00")));
        assertTrue(Files.isRegularFile(file));
    }

    @Test
    void shouldRejectInvalidDocumentWithoutReplacingExistingBytes(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("products.json");
        JsonProductRepository repository = new JsonProductRepository(file);
        repository.save(new Product(ProductId.newId(), "Mouse", "", new BigDecimal("1.00")));
        byte[] previous = Files.readAllBytes(file);
        Files.writeString(file, "[{\"id\":\"bad\"}]", StandardCharsets.UTF_8);

        assertThrows(JsonPersistenceException.class, repository::findAll);
        assertThrows(JsonPersistenceException.class,
                () -> repository.save(new Product(ProductId.newId(), "Keyboard", "", new BigDecimal("2.00"))));
        assertEquals("[{\"id\":\"bad\"}]", Files.readString(file));
        assertTrue(previous.length > 0);
    }
}
