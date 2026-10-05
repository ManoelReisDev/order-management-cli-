package dev.manoelreis.ordermanagement.infrastructure.persistence.memory;

import dev.manoelreis.ordermanagement.application.product.ProductRepository;
import dev.manoelreis.ordermanagement.domain.product.Product;
import dev.manoelreis.ordermanagement.domain.product.ProductId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryProductRepository implements ProductRepository {

    private final Map<ProductId, Product> products = new LinkedHashMap<>();

    @Override
    public void save(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product is required");
        }
        products.put(product.getId(), product);
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        if (id == null) {
            throw new IllegalArgumentException("Product id is required");
        }
        return Optional.ofNullable(products.get(id));
    }

    @Override
    public List<Product> findAll() {
        return List.copyOf(products.values());
    }
}
