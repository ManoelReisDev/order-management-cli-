package dev.manoelreis.ordermanagement.application.product;

import dev.manoelreis.ordermanagement.domain.product.Product;
import dev.manoelreis.ordermanagement.domain.product.ProductId;

import java.math.BigDecimal;
import java.util.List;

public final class ProductApplicationService {

    private final ProductRepository productRepository;

    public ProductApplicationService(ProductRepository productRepository) {
        if (productRepository == null) {
            throw new IllegalArgumentException("Product repository is required");
        }
        this.productRepository = productRepository;
    }

    public Product registerProduct(String name, String description, BigDecimal price) {
        Product product = new Product(ProductId.newId(), name, description, price);
        productRepository.save(product);
        return product;
    }

    public Product findProductById(ProductId productId) {
        ProductId requiredProductId = requireProductId(productId);
        return productRepository.findById(requiredProductId)
                .orElseThrow(() -> new ProductNotFoundException(requiredProductId));
    }

    public List<Product> listProducts() {
        return List.copyOf(productRepository.findAll());
    }

    public Product renameProduct(ProductId productId, String name) {
        Product product = findProductById(productId);
        product.rename(name);
        productRepository.save(product);
        return product;
    }

    public Product changeProductDescription(ProductId productId, String description) {
        Product product = findProductById(productId);
        product.changeDescription(description);
        productRepository.save(product);
        return product;
    }

    public Product changeProductPrice(ProductId productId, BigDecimal price) {
        Product product = findProductById(productId);
        product.changePrice(price);
        productRepository.save(product);
        return product;
    }

    private static ProductId requireProductId(ProductId productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product id is required");
        }
        return productId;
    }
}
