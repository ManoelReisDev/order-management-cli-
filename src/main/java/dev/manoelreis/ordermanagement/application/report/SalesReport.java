package dev.manoelreis.ordermanagement.application.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SalesReport(
        Instant startInclusive,
        Instant endExclusive,
        long orderCount,
        long paidOrderCount,
        BigDecimal paidRevenue,
        List<CustomerSales> customerSales) {

    public SalesReport {
        customerSales = List.copyOf(customerSales);
    }
}
