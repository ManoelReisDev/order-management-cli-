package dev.manoelreis.ordermanagement.application.customer;

import dev.manoelreis.ordermanagement.domain.customer.Customer;
import dev.manoelreis.ordermanagement.domain.customer.CustomerId;
import dev.manoelreis.ordermanagement.domain.exception.InvalidCustomerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerApplicationServiceTest {

    private RecordingCustomerRepository customerRepository;
    private CustomerApplicationService service;

    @BeforeEach
    void setUp() {
        customerRepository = new RecordingCustomerRepository();
        service = new CustomerApplicationService(customerRepository);
    }

    @Test
    void shouldRejectNullRepository() {
        assertThrows(IllegalArgumentException.class, () -> new CustomerApplicationService(null));
    }

    @Test
    void shouldRegisterCustomerWithNewIdAndNormalizedData() {
        Customer registeredCustomer = service.registerCustomer("  Ada Lovelace  ", "  ada@example.com  ");

        Customer savedCustomer = customerRepository.savedCustomers.getFirst();
        assertSame(registeredCustomer, savedCustomer);
        assertNotNull(registeredCustomer.getId());
        assertEquals("Ada Lovelace", registeredCustomer.getName());
        assertEquals("ada@example.com", registeredCustomer.getEmail());
    }

    @Test
    void shouldNotSaveWhenRegistrationDataIsInvalid() {
        assertThrows(InvalidCustomerException.class,
                () -> service.registerCustomer(" ", "ada@example.com"));

        assertTrue(customerRepository.savedCustomers.isEmpty());
        assertEquals(0, customerRepository.findByIdCalls);
    }

    @Test
    void shouldFindCustomerById() {
        Customer customer = customer("Ada", "ada@example.com");
        customerRepository.customerToFind = Optional.of(customer);

        Customer foundCustomer = service.findCustomerById(customer.getId());

        assertSame(customer, foundCustomer);
    }

    @Test
    void shouldThrowExceptionWithIdWhenCustomerIsNotFound() {
        CustomerId customerId = CustomerId.newId();

        CustomerNotFoundException exception = assertThrows(CustomerNotFoundException.class,
                () -> service.findCustomerById(customerId));

        assertFalse(exception.getMessage().isBlank());
        assertTrue(exception.getMessage().contains(customerId.toString()));
    }

    @Test
    void shouldReturnEmptyCustomerList() {
        List<Customer> customers = service.listCustomers();

        assertEquals(List.of(), customers);
    }

    @Test
    void shouldReturnUnmodifiableCustomerListCopyInRepositoryOrder() {
        Customer first = customer("Ada", "ada@example.com");
        Customer second = customer("Grace", "grace@example.com");
        List<Customer> repositoryCustomers = new ArrayList<>(List.of(first, second));
        customerRepository.customers = repositoryCustomers;

        List<Customer> customers = service.listCustomers();
        repositoryCustomers.clear();

        assertEquals(List.of(first, second), customers);
        assertThrows(UnsupportedOperationException.class, () -> customers.add(first));
        assertThrows(UnsupportedOperationException.class, () -> customers.remove(first));
    }

    @Test
    void shouldRenameExistingCustomerAndSaveIt() {
        Customer customer = customer("Ada", "ada@example.com");
        customerRepository.customerToFind = Optional.of(customer);

        Customer renamedCustomer = service.renameCustomer(customer.getId(), "  Grace Hopper  ");

        assertSame(customer, renamedCustomer);
        assertEquals("Grace Hopper", renamedCustomer.getName());
        assertEquals(List.of(customer), customerRepository.savedCustomers);
    }

    @Test
    void shouldChangeExistingCustomerEmailAndSaveIt() {
        Customer customer = customer("Ada", "ada@example.com");
        customerRepository.customerToFind = Optional.of(customer);

        Customer changedCustomer = service.changeCustomerEmail(customer.getId(), "  new@example.com  ");

        assertSame(customer, changedCustomer);
        assertEquals("new@example.com", changedCustomer.getEmail());
        assertEquals(List.of(customer), customerRepository.savedCustomers);
    }

    @Test
    void shouldNotSaveChangesWhenCustomerIsNotFound() {
        CustomerId customerId = CustomerId.newId();

        assertThrows(CustomerNotFoundException.class,
                () -> service.renameCustomer(customerId, "Grace"));
        assertThrows(CustomerNotFoundException.class,
                () -> service.changeCustomerEmail(customerId, "grace@example.com"));

        assertTrue(customerRepository.savedCustomers.isEmpty());
    }

    @Test
    void shouldNotSaveInvalidCustomerChanges() {
        Customer customer = customer("Ada", "ada@example.com");
        customerRepository.customerToFind = Optional.of(customer);

        assertThrows(InvalidCustomerException.class,
                () -> service.renameCustomer(customer.getId(), " "));
        assertThrows(InvalidCustomerException.class,
                () -> service.changeCustomerEmail(customer.getId(), "invalid-email"));

        assertTrue(customerRepository.savedCustomers.isEmpty());
    }

    @Test
    void shouldRejectNullIdBeforeConsultingRepository() {
        assertThrows(IllegalArgumentException.class, () -> service.findCustomerById(null));
        assertThrows(IllegalArgumentException.class, () -> service.renameCustomer(null, "Grace"));
        assertThrows(IllegalArgumentException.class,
                () -> service.changeCustomerEmail(null, "grace@example.com"));

        assertEquals(0, customerRepository.findByIdCalls);
        assertTrue(customerRepository.savedCustomers.isEmpty());
    }

    private static Customer customer(String name, String email) {
        return new Customer(CustomerId.newId(), name, email);
    }

    private static final class RecordingCustomerRepository implements CustomerRepository {

        private final List<Customer> savedCustomers = new ArrayList<>();
        private Optional<Customer> customerToFind = Optional.empty();
        private List<Customer> customers = List.of();
        private int findByIdCalls;

        @Override
        public void save(Customer customer) {
            savedCustomers.add(customer);
        }

        @Override
        public Optional<Customer> findById(CustomerId id) {
            findByIdCalls++;
            return customerToFind;
        }

        @Override
        public List<Customer> findAll() {
            return customers;
        }
    }
}
