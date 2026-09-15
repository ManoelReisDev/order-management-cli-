package dev.manoelreis.ordermanagement.domain.product;

import dev.manoelreis.ordermanagement.domain.exception.InvalidProductException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Product {

    private final ProductId id;
    private String name;
    private String description;
    private BigDecimal price;

    public Product(ProductId id, String name, String description, BigDecimal price) {
        this.id = requireId(id);
        this.name = normalizeName(name);
        this.description = normalizeDescription(description);
        this.price = normalizePrice(price);
    }

    public ProductId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void rename(String name) {
        this.name = normalizeName(name);
    }

    public void changeDescription(String description) {
        this.description = normalizeDescription(description);
    }

    public void changePrice(BigDecimal price) {
        this.price = normalizePrice(price);
    }

    private static ProductId requireId(ProductId id) {
        if (id == null) {
            throw new InvalidProductException("Product id is required");
        }
        return id;
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidProductException("Product name is required");
        }
        return name.trim();
    }

    private static String normalizeDescription(String description) {
        if (description == null) {
            throw new InvalidProductException("Product description is required, but may be empty");
        }
        return description.trim();
    }

    private static BigDecimal normalizePrice(BigDecimal price) {
        if (price == null) {
            throw new InvalidProductException("Product price is required");
        }

        final BigDecimal normalizedPrice;
        try {
            normalizedPrice = price.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new InvalidProductException("Product price must have at most two decimal places");
        }

        if (normalizedPrice.signum() <= 0) {
            throw new InvalidProductException("Product price must be greater than zero");
        }
        return normalizedPrice;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Product product)) {
            return false;
        }
        return id.equals(product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
