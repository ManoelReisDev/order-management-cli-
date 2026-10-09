package dev.manoelreis.ordermanagement.cli.input;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public final class TerminalInput {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern PRICE_PATTERN = Pattern.compile("[+-]?[0-9]+(?:\\.[0-9]+)?");
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("[+-]?[0-9]+");

    private final BufferedReader reader;

    public TerminalInput(Reader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("Input is required");
        }
        this.reader = new BufferedReader(reader);
    }

    public String readLine() throws IOException {
        return reader.readLine();
    }

    public static Optional<UUID> parseId(String text) {
        String normalized = text.trim();
        if (!UUID_PATTERN.matcher(normalized).matches()) {
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(normalized));
    }

    public static Optional<BigDecimal> parsePrice(String text) {
        String normalized = text.trim();
        if (!PRICE_PATTERN.matcher(normalized).matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(normalized));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public static Optional<Integer> parseQuantity(String text) {
        String normalized = text.trim();
        if (!QUANTITY_PATTERN.matcher(normalized).matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(normalized));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
