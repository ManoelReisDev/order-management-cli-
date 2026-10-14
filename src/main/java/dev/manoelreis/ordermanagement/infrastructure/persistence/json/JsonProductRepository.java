package dev.manoelreis.ordermanagement.infrastructure.persistence.json;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.manoelreis.ordermanagement.application.product.ProductRepository;
import dev.manoelreis.ordermanagement.domain.product.Product;
import dev.manoelreis.ordermanagement.domain.product.ProductId;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JsonProductRepository implements ProductRepository {

    private static final TypeReference<List<ProductDocument>> TYPE = new TypeReference<>() { };
    private final Path file;
    private final JsonFileStore store = new JsonFileStore();

    public JsonProductRepository(Path file) { this.file = requirePath(file); }

    @Override
    public void save(Product product) {
        if (product == null) throw new IllegalArgumentException("Product is required");
        LinkedHashMap<ProductId, ProductDocument> records = readRecords();
        records.put(product.getId(), new ProductDocument(product.getId().toString(), product.getName(), product.getDescription(), product.getPrice().toPlainString()));
        store.writeAtomic(file, records.values());
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        if (id == null) throw new IllegalArgumentException("Product id is required");
        return readRecords().values().stream().filter(record -> JsonValues.uuid(record.id()).equals(id.value())).findFirst().map(JsonProductRepository::toDomain);
    }

    @Override
    public List<Product> findAll() {
        return List.copyOf(readRecords().values().stream().map(JsonProductRepository::toDomain).toList());
    }

    private LinkedHashMap<ProductId, ProductDocument> readRecords() {
        try {
            LinkedHashMap<ProductId, ProductDocument> result = new LinkedHashMap<>();
            for (ProductDocument record : store.read(file, TYPE)) {
                Product product = toDomain(record);
                if (result.put(product.getId(), record) != null) throw new IllegalArgumentException("duplicate product id");
            }
            return result;
        } catch (Exception exception) {
            if (exception instanceof JsonPersistenceException jsonException) throw jsonException;
            throw new JsonPersistenceException("validar", file, exception);
        }
    }

    private static Product toDomain(ProductDocument record) {
        if (record == null) throw new IllegalArgumentException("null product");
        return new Product(ProductId.of(JsonValues.uuid(record.id())), record.name(), record.description(), JsonValues.money(record.price()));
    }

    private static Path requirePath(Path path) {
        if (path == null) throw new IllegalArgumentException("File path is required");
        return path;
    }

    private record ProductDocument(String id, String name, String description, String price) { }
}
