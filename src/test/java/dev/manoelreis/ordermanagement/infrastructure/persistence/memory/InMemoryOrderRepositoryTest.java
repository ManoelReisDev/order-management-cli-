package dev.manoelreis.ordermanagement.infrastructure.persistence.memory;

import dev.manoelreis.ordermanagement.domain.order.Order;
import dev.manoelreis.ordermanagement.domain.order.OrderId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.manoelreis.ordermanagement.domain.customer.CustomerId;
import dev.manoelreis.ordermanagement.domain.product.Product;
import dev.manoelreis.ordermanagement.domain.product.ProductId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryOrderRepositoryTest {

    private InMemoryOrderRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOrderRepository();
    }

    @Test
    void shouldReturnEmptyResultsWhenNew() {
        assertEquals(Optional.empty(), repository.findById(OrderId.newId()));
        assertEquals(List.of(), repository.findAll());
    }

    @Test
    void shouldFindSavedEntityByEquivalentIdAndSameReference() {
        Order entity = entity(OrderId.newId());
        repository.save(entity);

        assertSame(entity, repository.findById(OrderId.of(entity.getId().value())).orElseThrow());
        assertSame(entity, repository.findAll().getFirst());
    }

    @Test
    void shouldListEntitiesInFirstInsertionOrder() {
        Order first = entity(OrderId.newId());
        Order second = entity(OrderId.newId());
        Order third = entity(OrderId.newId());

        repository.save(first);
        repository.save(second);
        repository.save(third);

        List<Order> entities = repository.findAll();
        assertEquals(3, entities.size());
        assertSame(first, entities.get(0));
        assertSame(second, entities.get(1));
        assertSame(third, entities.get(2));
    }

    @Test
    void shouldReplaceReferenceWithoutDuplicatingOrChangingPosition() {
        Order first = entity(OrderId.newId());
        Order second = entity(OrderId.newId());
        Order replacement = entity(OrderId.of(first.getId().value()));
        Order third = entity(OrderId.newId());
        repository.save(first);
        repository.save(second);
        List<Order> snapshot = repository.findAll();

        repository.save(replacement);
        repository.save(third);

        List<Order> entities = repository.findAll();
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
        List<Order> emptySnapshot = repository.findAll();
        Order first = entity(OrderId.newId());
        repository.save(first);
        List<Order> snapshot = repository.findAll();

        repository.save(entity(OrderId.newId()));

        assertEquals(List.of(), emptySnapshot);
        assertEquals(1, snapshot.size());
        assertSame(first, snapshot.getFirst());
        assertEquals(2, repository.findAll().size());
    }

    @Test
    void shouldRejectStructuralChangesToReturnedList() {
        Order entity = entity(OrderId.newId());
        repository.save(entity);
        List<Order> snapshot = repository.findAll();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(entity));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.remove(entity));
        assertEquals(1, repository.findAll().size());
        assertSame(entity, repository.findById(entity.getId()).orElseThrow());
    }

    @Test
    void shouldKeepEntityReferencesWhenTheirStateChanges() {
        Order entity = entity(OrderId.newId());
        repository.save(entity);
        List<Order> snapshot = repository.findAll();

        entity.addProduct(new Product(ProductId.newId(), "Keyboard", "", new BigDecimal("100.00")), 2);
        assertEquals(new BigDecimal("200.00"), snapshot.getFirst().getSubtotal());
        assertSame(entity, snapshot.getFirst());
        assertSame(entity, repository.findById(entity.getId()).orElseThrow());
    }

    @Test
    void shouldRejectNullArgumentsWithoutChangingStoredEntities() {
        Order entity = entity(OrderId.newId());
        repository.save(entity);

        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
        assertThrows(IllegalArgumentException.class, () -> repository.findById(null));

        assertEquals(1, repository.findAll().size());
        assertSame(entity, repository.findAll().getFirst());
        assertSame(entity, repository.findById(entity.getId()).orElseThrow());
    }

    @Test
    void shouldKeepStateSeparateBetweenInstances() {
        Order entity = entity(OrderId.newId());
        repository.save(entity);
        InMemoryOrderRepository otherRepository = new InMemoryOrderRepository();

        assertEquals(Optional.empty(), otherRepository.findById(entity.getId()));
        assertEquals(List.of(), otherRepository.findAll());

        otherRepository.save(entity(OrderId.newId()));
        assertEquals(1, repository.findAll().size());
        assertSame(entity, repository.findAll().getFirst());
    }

    private static Order entity(OrderId id) {
        return new Order(id, CustomerId.newId(), Instant.parse("2026-01-01T10:00:00Z"));
    }
}
