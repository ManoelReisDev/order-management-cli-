package dev.manoelreis.ordermanagement.application.report;

import dev.manoelreis.ordermanagement.application.order.OrderRepository;
import dev.manoelreis.ordermanagement.domain.customer.CustomerId;
import dev.manoelreis.ordermanagement.domain.order.Order;
import dev.manoelreis.ordermanagement.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ReportApplicationService {

    private final OrderRepository orderRepository;

    public ReportApplicationService(OrderRepository orderRepository) {
        if (orderRepository == null) {
            throw new IllegalArgumentException("Order repository is required");
        }
        this.orderRepository = orderRepository;
    }

    public SalesReport generateSalesReport(Instant startInclusive, Instant endExclusive) {
        if (startInclusive == null || endExclusive == null || !startInclusive.isBefore(endExclusive)) {
            throw new IllegalArgumentException("Report start must precede end and both bounds are required");
        }

        List<Order> orders = orderRepository.findAll().stream()
                .filter(order -> !order.getCreatedAt().isBefore(startInclusive)
                        && order.getCreatedAt().isBefore(endExclusive))
                .toList();
        List<Order> paidOrders = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID)
                .toList();

        BigDecimal paidRevenue = paidOrders.stream()
                .map(Order::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<CustomerId, List<Order>> ordersByCustomer = paidOrders.stream()
                .collect(Collectors.groupingBy(Order::getCustomerId));
        List<CustomerSales> customerSales = ordersByCustomer.entrySet().stream()
                .map(entry -> new CustomerSales(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream()
                                .map(Order::getSubtotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)))
                .sorted(Comparator.comparing(CustomerSales::paidRevenue).reversed()
                        .thenComparing(sales -> sales.customerId().toString()))
                .toList();

        return new SalesReport(startInclusive, endExclusive, orders.size(), paidOrders.size(),
                paidRevenue, customerSales);
    }
}
