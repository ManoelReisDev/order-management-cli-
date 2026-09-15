package dev.manoelreis.ordermanagement.domain.customer;

import dev.manoelreis.ordermanagement.domain.exception.InvalidCustomerException;

import java.util.Objects;
import java.util.regex.Pattern;

public final class Customer {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final CustomerId id;
    private String name;
    private String email;

    public Customer(CustomerId id, String name, String email) {
        this.id = requireId(id);
        this.name = normalizeName(name);
        this.email = normalizeEmail(email);
    }

    public CustomerId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public void rename(String name) {
        this.name = normalizeName(name);
    }

    public void changeEmail(String email) {
        this.email = normalizeEmail(email);
    }

    private static CustomerId requireId(CustomerId id) {
        if (id == null) {
            throw new InvalidCustomerException("Customer id is required");
        }
        return id;
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidCustomerException("Customer name is required");
        }
        return name.trim();
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidCustomerException("Customer email is required");
        }

        String normalizedEmail = email.trim();
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new InvalidCustomerException("Customer email format is invalid");
        }
        return normalizedEmail;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Customer customer)) {
            return false;
        }
        return id.equals(customer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
