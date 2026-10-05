package dev.manoelreis.ordermanagement.infrastructure.persistence.memory;

import dev.manoelreis.ordermanagement.domain.product.Product;
import dev.manoelreis.ordermanagement.domain.product.ProductId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryProductRepositoryTest {

    private InMemoryProductRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProductRepository();
    }

    @Test
    void shouldReturnEmptyResultsWhenNew() {
        assertEquals(Optional.empty(), repository.findById(ProductId.newId()));
        assertEquals(List.of(), repository.findAll());
    }

    @Test
    void shouldFindSavedEntityByEquivalentIdAndSameReference() {
        Product entity = entity(ProductId.newId());
        repository.save(entity);

        assertSame(entity, repository.findById(ProductId.of(entity.getId().value())).orElseThrow());
        assertSame(entity, repository.findAll().getFirst());
    }

    @Test
    void shouldListEntitiesInFirstInsertionOrder() {
        Product first = entity(ProductId.newId());
        Product second = entity(ProductId.newId());
        Product third = entity(ProductId.newId());

        repository.save(first);
        repository.save(second);
        repository.save(third);

        List<Product> entities = repository.findAll();
        assertEquals(3, entities.size());
        assertSame(first, entities.get(0));
        assertSame(second, entities.get(1));
        assertSame(third, entities.get(2));
    }

    @Test
    void shouldReplaceReferenceWithoutDuplicatingOrChangingPosition() {
        Product first = entity(ProductId.newId());
        Product second = entity(ProductId.newId());
        Product replacement = entity(ProductId.of(first.getId().value()));
        Product third = entity(ProductId.newId());
        repository.save(first);
        repository.save(second);
        List<Product> snapshot = repository.findAll();

        repository.save(replacement);
        repository.save(third);

        List<Product> entities = repository.findAll();
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
        List<Product> emptySnapshot = repository.findAll();
        Product first = entity(ProductId.newId());
        repository.save(first);
        List<Product> snapshot = repository.findAll();

        repository.save(entity(ProductId.newId()));

        assertEquals(List.of(), emptySnapshot);
        assertEquals(1, snapshot.size());
        assertSame(first, snapshot.getFirst());
        assertEquals(2, repository.findAll().size());
    }

    @Test
    void shouldRejectStructuralChangesToReturnedList() {
        Product entity = entity(ProductId.newId());
        repository.save(entity);
        List<Product> snapshot = repository.findAll();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(entity));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.remove(entity));
        assertEquals(1, repository.findAll().size());
        assertSame(entity, repository.findById(entity.getId()).orElseThrow());
    }

    @Test
    void shouldKeepEntityReferencesWhenTheirStateChanges() {
        Product entity = entity(ProductId.newId());
        repository.save(entity);
        List<Product> snapshot = repository.findAll();

        entity.rename("Updated name");
        assertEquals("Updated name", snapshot.getFirst().getName());
        assertSame(entity, snapshot.getFirst());
        assertSame(entity, repository.findById(entity.getId()).orElseThrow());
    }

    @Test
    void shouldRejectNullArgumentsWithoutChangingStoredEntities() {
        Product entity = entity(ProductId.newId());
        repository.save(entity);

        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
        assertThrows(IllegalArgumentException.class, () -> repository.findById(null));

        assertEquals(1, repository.findAll().size());
        assertSame(entity, repository.findAll().getFirst());
        assertSame(entity, repository.findById(entity.getId()).orElseThrow());
    }

    @Test
    void shouldKeepStateSeparateBetweenInstances() {
        Product entity = entity(ProductId.newId());
        repository.save(entity);
        InMemoryProductRepository otherRepository = new InMemoryProductRepository();

        assertEquals(Optional.empty(), otherRepository.findById(entity.getId()));
        assertEquals(List.of(), otherRepository.findAll());

        otherRepository.save(entity(ProductId.newId()));
        assertEquals(1, repository.findAll().size());
        assertSame(entity, repository.findAll().getFirst());
    }

    private static Product entity(ProductId id) {
        return new Product(id, "Keyboard", "Mechanical keyboard", new BigDecimal("100.00"));
    }
}
