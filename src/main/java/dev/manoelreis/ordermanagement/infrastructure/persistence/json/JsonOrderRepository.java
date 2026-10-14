package dev.manoelreis.ordermanagement.infrastructure.persistence.json;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.manoelreis.ordermanagement.application.order.OrderRepository;
import dev.manoelreis.ordermanagement.domain.customer.CustomerId;
import dev.manoelreis.ordermanagement.domain.order.Order;
import dev.manoelreis.ordermanagement.domain.order.OrderId;
import dev.manoelreis.ordermanagement.domain.order.OrderItem;
import dev.manoelreis.ordermanagement.domain.order.OrderStatus;
import dev.manoelreis.ordermanagement.domain.product.ProductId;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public final class JsonOrderRepository implements OrderRepository {

    private static final TypeReference<List<OrderDocument>> TYPE = new TypeReference<>() { };
    private final Path file;
    private final JsonFileStore store = new JsonFileStore();

    public JsonOrderRepository(Path file) { this.file = requirePath(file); }

    @Override
    public void save(Order order) {
        if (order == null) throw new IllegalArgumentException("Order is required");
        LinkedHashMap<OrderId, OrderDocument> records = readRecords();
        records.put(order.getId(), toDocument(order));
        store.writeAtomic(file, records.values());
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        if (id == null) throw new IllegalArgumentException("Order id is required");
        return readRecords().values().stream().filter(record -> JsonValues.uuid(record.id()).equals(id.value())).findFirst().map(JsonOrderRepository::toDomain);
    }

    @Override
    public List<Order> findAll() {
        return List.copyOf(readRecords().values().stream().map(JsonOrderRepository::toDomain).toList());
    }

    private LinkedHashMap<OrderId, OrderDocument> readRecords() {
        try {
            LinkedHashMap<OrderId, OrderDocument> result = new LinkedHashMap<>();
            for (OrderDocument record : store.read(file, TYPE)) {
                Order order = toDomain(record);
                if (result.put(order.getId(), record) != null) throw new IllegalArgumentException("duplicate order id");
            }
            return result;
        } catch (Exception exception) {
            if (exception instanceof JsonPersistenceException jsonException) throw jsonException;
            throw new JsonPersistenceException("validar", file, exception);
        }
    }

    private static OrderDocument toDocument(Order order) {
        List<ItemDocument> items = order.getItems().stream()
                .map(item -> new ItemDocument(item.getProductId().toString(), item.getProductName(), item.getUnitPrice().toPlainString(), item.getQuantity()))
                .toList();
        return new OrderDocument(order.getId().toString(), order.getCustomerId().toString(), order.getCreatedAt().toString(), order.getStatus().name(), items);
    }

    private static Order toDomain(OrderDocument record) {
        if (record == null || record.items() == null) throw new IllegalArgumentException("invalid order");
        List<OrderItem> items = record.items().stream().map(item -> {
            if (item == null) throw new IllegalArgumentException("null order item");
            return OrderItem.restore(ProductId.of(JsonValues.uuid(item.productId())), item.productName(),
                    JsonValues.money(item.unitPrice()), item.quantity());
        }).toList();
        return Order.restore(OrderId.of(JsonValues.uuid(record.id())), CustomerId.of(JsonValues.uuid(record.customerId())),
                JsonValues.instant(record.createdAt()), OrderStatus.valueOf(record.status()), items);
    }

    private static Path requirePath(Path path) {
        if (path == null) throw new IllegalArgumentException("File path is required");
        return path;
    }

    private record OrderDocument(String id, String customerId, String createdAt, String status, List<ItemDocument> items) { }
    private record ItemDocument(String productId, String productName, String unitPrice, int quantity) { }
}
