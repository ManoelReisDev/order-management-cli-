package dev.manoelreis.ordermanagement.application.report;

import dev.manoelreis.ordermanagement.domain.customer.CustomerId;

import java.math.BigDecimal;

public record CustomerSales(CustomerId customerId, long paidOrderCount, BigDecimal paidRevenue) {
}
