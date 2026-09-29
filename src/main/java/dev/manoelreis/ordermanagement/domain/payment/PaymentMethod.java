package dev.manoelreis.ordermanagement.domain.payment;

import java.math.BigDecimal;

@FunctionalInterface
public interface PaymentMethod {

    PaymentResult process(BigDecimal amount);
}
