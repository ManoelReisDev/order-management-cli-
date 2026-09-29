package dev.manoelreis.ordermanagement.application.product;

import dev.manoelreis.ordermanagement.domain.product.ProductId;

public final class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(ProductId productId) {
        super("Product not found: " + productId);
    }
}
