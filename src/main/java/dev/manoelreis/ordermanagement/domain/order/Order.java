package dev.manoelreis.ordermanagement.domain.order;

import dev.manoelreis.ordermanagement.domain.customer.CustomerId;
import dev.manoelreis.ordermanagement.domain.exception.DomainException;
import dev.manoelreis.ordermanagement.domain.exception.OrderItemNotFoundException;
import dev.manoelreis.ordermanagement.domain.product.Product;
import dev.manoelreis.ordermanagement.domain.product.ProductId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Order {

    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");

    private final OrderId id;
    private final CustomerId customerId;
    private final Instant createdAt;
    private final OrderStatus status;
    private final Map<ProductId, OrderItem> items = new LinkedHashMap<>();

    public Order(OrderId id, CustomerId customerId, Instant createdAt) {
        this.id = requireValue(id, "Order id is required");
        this.customerId = requireValue(customerId, "Order customer id is required");
        this.createdAt = requireValue(createdAt, "Order creation instant is required");
        this.status = OrderStatus.DRAFT;
    }

    public OrderId getId() {
        return id;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<OrderItem> getItems() {
        return List.copyOf(items.values());
    }

    public void addProduct(Product product, int quantity) {
        Product requiredProduct = requireValue(product, "Product is required");
        items.compute(requiredProduct.getId(), (productId, existingItem) -> existingItem == null ? OrderItem.from(requiredProduct, quantity) : existingItem.addQuantity(quantity));
    }

    public void changeItemQuantity(ProductId productId, int quantity) {
        OrderItem existingItem = findItem(productId);
        items.put(productId, existingItem.withQuantity(quantity));
    }

    public void removeItem(ProductId productId) {
        findItem(productId);
        items.remove(productId);
    }

    public BigDecimal getSubtotal() {
        return items.values().stream().map(OrderItem::getSubtotal).reduce(ZERO_MONEY, BigDecimal::add);
    }

    private OrderItem findItem(ProductId productId) {
        if (productId == null) {
            throw new OrderItemNotFoundException("Product id is required to locate an order item");
        }

        OrderItem item = items.get(productId);
        if (item == null) {
            throw new OrderItemNotFoundException("Order item was not found for product " + productId);
        }
        return item;
    }

    private static <T> T requireValue(T value, String message) {
        if (value == null) {
            throw new DomainException(message);
        }
        return value;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Order order)) {
            return false;
        }
        return id.equals(order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
