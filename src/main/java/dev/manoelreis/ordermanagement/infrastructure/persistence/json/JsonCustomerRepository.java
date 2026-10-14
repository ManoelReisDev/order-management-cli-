package dev.manoelreis.ordermanagement.infrastructure.persistence.json;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.manoelreis.ordermanagement.application.customer.CustomerRepository;
import dev.manoelreis.ordermanagement.domain.customer.Customer;
import dev.manoelreis.ordermanagement.domain.customer.CustomerId;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JsonCustomerRepository implements CustomerRepository {

    private static final TypeReference<List<CustomerDocument>> TYPE = new TypeReference<>() { };
    private final Path file;
    private final JsonFileStore store = new JsonFileStore();

    public JsonCustomerRepository(Path file) {
        this.file = requirePath(file);
    }

    @Override
    public void save(Customer customer) {
        if (customer == null) throw new IllegalArgumentException("Customer is required");
        LinkedHashMap<CustomerId, CustomerDocument> records = readRecords();
        records.put(customer.getId(), new CustomerDocument(customer.getId().toString(), customer.getName(), customer.getEmail()));
        store.writeAtomic(file, records.values());
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        if (id == null) throw new IllegalArgumentException("Customer id is required");
        return readRecords().values().stream().filter(record -> JsonValues.uuid(record.id()).equals(id.value())).findFirst().map(JsonCustomerRepository::toDomain);
    }

    @Override
    public List<Customer> findAll() {
        return List.copyOf(readRecords().values().stream().map(JsonCustomerRepository::toDomain).toList());
    }

    private LinkedHashMap<CustomerId, CustomerDocument> readRecords() {
        try {
            LinkedHashMap<CustomerId, CustomerDocument> result = new LinkedHashMap<>();
            for (CustomerDocument record : store.read(file, TYPE)) {
                Customer customer = toDomain(record);
                if (result.put(customer.getId(), record) != null) throw new IllegalArgumentException("duplicate customer id");
            }
            return result;
        } catch (Exception exception) {
            throw failure(exception);
        }
    }

    private static Customer toDomain(CustomerDocument record) {
        if (record == null) throw new IllegalArgumentException("null customer");
        UUID id = JsonValues.uuid(record.id());
        return new Customer(CustomerId.of(id), record.name(), record.email());
    }

    private static Path requirePath(Path path) {
        if (path == null) throw new IllegalArgumentException("File path is required");
        return path;
    }

    private JsonPersistenceException failure(Exception exception) {
        if (exception instanceof JsonPersistenceException jsonException) return jsonException;
        return new JsonPersistenceException("validar", file, exception);
    }

    private record CustomerDocument(String id, String name, String email) { }
}
