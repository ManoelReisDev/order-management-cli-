package dev.manoelreis.ordermanagement.infrastructure.persistence.json;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

final class JsonValues {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern MONEY_PATTERN = Pattern.compile("[0-9]+\\.[0-9]{2}");

    private JsonValues() {
    }

    static UUID uuid(String value) {
        if (value == null || !UUID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid UUID");
        }
        return UUID.fromString(value);
    }

    static BigDecimal money(String value) {
        if (value == null || !MONEY_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid monetary value");
        }
        BigDecimal result = new BigDecimal(value);
        if (result.signum() <= 0) {
            throw new IllegalArgumentException("monetary value must be positive");
        }
        return result;
    }

    static Instant instant(String value) {
        if (value == null || !value.endsWith("Z")) {
            throw new IllegalArgumentException("instant must be UTC");
        }
        return Instant.parse(value);
    }
}
