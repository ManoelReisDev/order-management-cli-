package dev.manoelreis.ordermanagement.infrastructure.persistence.memory;

import dev.manoelreis.ordermanagement.domain.customer.Customer;
import dev.manoelreis.ordermanagement.domain.customer.CustomerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryCustomerRepositoryTest {

    private InMemoryCustomerRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCustomerRepository();
    }

    @Test
    void shouldReturnEmptyResultsWhenNew() {
        assertEquals(Optional.empty(), repository.findById(CustomerId.newId()));
        assertEquals(List.of(), repository.findAll());
    }

    @Test
    void shouldFindSavedEntityByEquivalentIdAndSameReference() {
        Customer entity = entity(CustomerId.newId());
        repository.save(entity);

        assertSame(entity, repository.findById(CustomerId.of(entity.getId().value())).orElseThrow());
        assertSame(entity, repository.findAll().getFirst());
    }

    @Test
    void shouldListEntitiesInFirstInsertionOrder() {
        Customer first = entity(CustomerId.newId());
        Customer second = entity(CustomerId.newId());
        Customer third = entity(CustomerId.newId());

        repository.save(first);
        repository.save(second);
        repository.save(third);

        List<Customer> entities = repository.findAll();
        assertEquals(3, entities.size());
        assertSame(first, entities.get(0));
        assertSame(second, entities.get(1));
        assertSame(third, entities.get(2));
    }

    @Test
    void shouldReplaceReferenceWithoutDuplicatingOrChangingPosition() {
        Customer first = entity(CustomerId.newId());
        Customer second = entity(CustomerId.newId());
        Customer replacement = entity(CustomerId.of(first.getId().value()));
        Customer third = entity(CustomerId.newId());
        repository.save(first);
        repository.save(second);
        List<Customer> snapshot = repository.findAll();

        repository.save(replacement);
        repository.save(third);

        List<Customer> entities = repository.findAll();
        assertEquals(3, entities.size());
        assertSame(replacement, repository.findById(first.getId()).orElseThrow());
        assertSame(replacement, entities.get(0));
        assertSame(second, entities.get(1));
        assertSame(third, entities.get(2));
        assertEquals(2, snapshot.size());
        assertSame(first, snapshot.get(0));
        assertSame(second, snapshot.get(1));
    }

    @Test
    void shouldKeepPreviousSnapshotsUnchangedAfterInsertion() {
        List<Customer> emptySnapshot = repository.findAll();
        Customer first = entity(CustomerId.newId());
        repository.save(first);
        List<Customer> snapshot = repository.findAll();

        repository.save(entity(CustomerId.newId()));

        assertEquals(List.of(), emptySnapshot);
        assertEquals(1, snapshot.size());
        assertSame(first, snapshot.getFirst());
        assertEquals(2, repository.findAll().size());
    }

    @Test
    void shouldRejectStructuralChangesToReturnedList() {
        Customer entity = entity(CustomerId.newId());
        repository.save(entity);
        List<Customer> snapshot = repository.findAll();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(entity));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.remove(entity));
        assertEquals(1, repository.findAll().size());
        assertSame(entity, repository.findById(entity.getId()).orElseThrow());
    }

    @Test
    void shouldKeepEntityReferencesWhenTheirStateChanges() {
        Customer entity = entity(CustomerId.newId());
        repository.save(entity);
        List<Customer> snapshot = repository.findAll();

        entity.rename("Updated name");
        assertEquals("Updated name", snapshot.getFirst().getName());
        assertSame(entity, snapshot.getFirst());
        assertSame(entity, repository.findById(entity.getId()).orElseThrow());
    }

    @Test
    void shouldRejectNullArgumentsWithoutChangingStoredEntities() {
        Customer entity = entity(CustomerId.newId());
        repository.save(entity);

        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
        assertThrows(IllegalArgumentException.class, () -> repository.findById(null));

        assertEquals(1, repository.findAll().size());
        assertSame(entity, repository.findAll().getFirst());
        assertSame(entity, repository.findById(entity.getId()).orElseThrow());
    }

    @Test
    void shouldKeepStateSeparateBetweenInstances() {
        Customer entity = entity(CustomerId.newId());
        repository.save(entity);
        InMemoryCustomerRepository otherRepository = new InMemoryCustomerRepository();

        assertEquals(Optional.empty(), otherRepository.findById(entity.getId()));
        assertEquals(List.of(), otherRepository.findAll());

        otherRepository.save(entity(CustomerId.newId()));
        assertEquals(1, repository.findAll().size());
        assertSame(entity, repository.findAll().getFirst());
    }

    private static Customer entity(CustomerId id) {
        return new Customer(id, "Ada", "ada@example.com");
    }
}
