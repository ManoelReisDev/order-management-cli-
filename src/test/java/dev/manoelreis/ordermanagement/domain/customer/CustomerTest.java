package dev.manoelreis.ordermanagement.domain.customer;

import dev.manoelreis.ordermanagement.domain.exception.InvalidCustomerException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerTest {

    @Test
    void shouldCreateAndChangeAValidCustomer() {
        Customer customer = new Customer(CustomerId.newId(), "  Ada Lovelace  ", "  ada@example.com  ");

        assertEquals("Ada Lovelace", customer.getName());
        assertEquals("ada@example.com", customer.getEmail());

        customer.rename("  Grace Hopper ");
        customer.changeEmail(" grace@example.com ");

        assertEquals("Grace Hopper", customer.getName());
        assertEquals("grace@example.com", customer.getEmail());
    }

    @Test
    void shouldRejectInvalidCustomerDataOnCreationAndChange() {
        Customer customer = validCustomer();

        assertThrows(InvalidCustomerException.class,
                () -> new Customer(null, "Ada", "ada@example.com"));
        assertThrows(InvalidCustomerException.class,
                () -> new Customer(CustomerId.newId(), " ", "ada@example.com"));
        assertThrows(InvalidCustomerException.class,
                () -> new Customer(CustomerId.newId(), "Ada", "invalid-email"));
        assertThrows(InvalidCustomerException.class, () -> customer.rename(null));
        assertThrows(InvalidCustomerException.class, () -> customer.changeEmail("ada@localhost"));
    }

    @Test
    void shouldDetermineEqualityByCustomerId() {
        CustomerId id = CustomerId.newId();
        Customer first = new Customer(id, "Ada", "ada@example.com");
        Customer sameIdentity = new Customer(id, "Grace", "grace@example.com");
        Customer anotherCustomer = validCustomer();

        assertEquals(first, sameIdentity);
        assertEquals(first.hashCode(), sameIdentity.hashCode());
        assertNotEquals(first, anotherCustomer);
    }

    private static Customer validCustomer() {
        return new Customer(CustomerId.newId(), "Ada", "ada@example.com");
    }
}
