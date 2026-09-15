package dev.manoelreis.ordermanagement.domain.order;

import dev.manoelreis.ordermanagement.domain.exception.InvalidQuantityException;
import dev.manoelreis.ordermanagement.domain.product.Product;
import dev.manoelreis.ordermanagement.domain.product.ProductId;

import java.math.BigDecimal;

public final class OrderItem {

    private final ProductId productId;
    private final String productName;
    private final BigDecimal unitPrice;
    private final int quantity;

    private OrderItem(ProductId productId, String productName, BigDecimal unitPrice, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = requireValidQuantity(quantity);
    }

    static OrderItem from(Product product, int quantity) {
        return new OrderItem(product.getId(), product.getName(), product.getPrice(), quantity);
    }

    OrderItem withQuantity(int quantity) {
        return new OrderItem(productId, productName, unitPrice, quantity);
    }

    OrderItem addQuantity(int additionalQuantity) {
        requireValidQuantity(additionalQuantity);
        long totalQuantity = (long) quantity + additionalQuantity;
        if (totalQuantity > Integer.MAX_VALUE) {
            throw new InvalidQuantityException("Item quantity exceeds the supported limit");
        }
        return withQuantity((int) totalQuantity);
    }

    public ProductId getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    private static int requireValidQuantity(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("Item quantity must be greater than zero");
        }
        return quantity;
    }
}
