package dev.manoelreis.ordermanagement.application.product;

import dev.manoelreis.ordermanagement.domain.exception.InvalidProductException;
import dev.manoelreis.ordermanagement.domain.product.Product;
import dev.manoelreis.ordermanagement.domain.product.ProductId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductApplicationServiceTest {

    private RecordingProductRepository productRepository;
    private ProductApplicationService service;

    @BeforeEach
    void setUp() {
        productRepository = new RecordingProductRepository();
        service = new ProductApplicationService(productRepository);
    }

    @Test
    void shouldRejectNullRepository() {
        assertThrows(IllegalArgumentException.class, () -> new ProductApplicationService(null));
    }

    @Test
    void shouldRegisterProductWithNewIdAndNormalizedData() {
        Product registeredProduct = service.registerProduct(
                "  Keyboard  ",
                "  Mechanical  ",
                new BigDecimal("100"));

        assertSame(registeredProduct, productRepository.savedProducts.getFirst());
        assertNotNull(registeredProduct.getId());
        assertEquals("Keyboard", registeredProduct.getName());
        assertEquals("Mechanical", registeredProduct.getDescription());
        assertEquals(new BigDecimal("100.00"), registeredProduct.getPrice());
    }

    @Test
    void shouldRegisterProductWithEmptyDescription() {
        Product registeredProduct = service.registerProduct("Keyboard", "", new BigDecimal("100.00"));

        assertEquals("", registeredProduct.getDescription());
        assertEquals(List.of(registeredProduct), productRepository.savedProducts);
    }

    @Test
    void shouldNotSaveWhenRegistrationDataIsInvalid() {
        assertThrows(InvalidProductException.class,
                () -> service.registerProduct(" ", "Mechanical", new BigDecimal("100.00")));
        assertThrows(InvalidProductException.class,
                () -> service.registerProduct("Keyboard", null, new BigDecimal("100.00")));
        assertThrows(InvalidProductException.class,
                () -> service.registerProduct("Keyboard", "Mechanical", BigDecimal.ZERO));
        assertTrue(productRepository.savedProducts.isEmpty());
        assertEquals(0, productRepository.findByIdCalls);
    }

    @Test
    void shouldFindProductById() {
        Product product = product("Keyboard", "Mechanical", "100.00");
        productRepository.productToFind = Optional.of(product);

        Product foundProduct = service.findProductById(product.getId());

        assertSame(product, foundProduct);
    }

    @Test
    void shouldThrowExceptionWithIdWhenProductIsNotFound() {
        ProductId productId = ProductId.newId();

        ProductNotFoundException exception = assertThrows(ProductNotFoundException.class,
                () -> service.findProductById(productId));

        assertFalse(exception.getMessage().isBlank());
        assertTrue(exception.getMessage().contains(productId.toString()));
    }

    @Test
    void shouldReturnEmptyProductList() {
        assertEquals(List.of(), service.listProducts());
    }

    @Test
    void shouldReturnUnmodifiableProductListCopyInRepositoryOrder() {
        Product first = product("Keyboard", "Mechanical", "100.00");
        Product second = product("Mouse", "Wireless", "50.00");
        List<Product> repositoryProducts = new ArrayList<>(List.of(first, second));
        productRepository.products = repositoryProducts;

        List<Product> products = service.listProducts();
        repositoryProducts.clear();

        assertEquals(List.of(first, second), products);
        assertThrows(UnsupportedOperationException.class, () -> products.add(first));
        assertThrows(UnsupportedOperationException.class, () -> products.remove(first));
    }

    @Test
    void shouldRenameExistingProductAndSaveIt() {
        Product product = product("Keyboard", "Mechanical", "100.00");
        productRepository.productToFind = Optional.of(product);

        Product renamedProduct = service.renameProduct(product.getId(), "  Gaming Keyboard  ");

        assertSame(product, renamedProduct);
        assertEquals("Gaming Keyboard", renamedProduct.getName());
        assertEquals(List.of(product), productRepository.savedProducts);
    }

    @Test
    void shouldChangeExistingProductDescriptionAndSaveIt() {
        Product product = product("Keyboard", "Mechanical", "100.00");
        productRepository.productToFind = Optional.of(product);

        Product changedProduct = service.changeProductDescription(product.getId(), "  Wireless  ");

        assertSame(product, changedProduct);
        assertEquals("Wireless", changedProduct.getDescription());
        assertEquals(List.of(product), productRepository.savedProducts);
    }

    @Test
    void shouldChangeExistingProductPriceAndSaveIt() {
        Product product = product("Keyboard", "Mechanical", "100.00");
        productRepository.productToFind = Optional.of(product);

        Product changedProduct = service.changeProductPrice(product.getId(), new BigDecimal("149.90"));

        assertSame(product, changedProduct);
        assertEquals(new BigDecimal("149.90"), changedProduct.getPrice());
        assertEquals(List.of(product), productRepository.savedProducts);
    }

    @Test
    void shouldNotSaveChangesWhenProductIsNotFound() {
        ProductId productId = ProductId.newId();

        assertThrows(ProductNotFoundException.class,
                () -> service.renameProduct(productId, "Gaming Keyboard"));
        assertThrows(ProductNotFoundException.class,
                () -> service.changeProductDescription(productId, "Wireless"));
        assertThrows(ProductNotFoundException.class,
                () -> service.changeProductPrice(productId, new BigDecimal("149.90")));

        assertTrue(productRepository.savedProducts.isEmpty());
    }

    @Test
    void shouldNotSaveInvalidProductChanges() {
        Product product = product("Keyboard", "Mechanical", "100.00");
        productRepository.productToFind = Optional.of(product);

        assertThrows(InvalidProductException.class, () -> service.renameProduct(product.getId(), " "));
        assertThrows(InvalidProductException.class,
                () -> service.changeProductDescription(product.getId(), null));
        assertThrows(InvalidProductException.class,
                () -> service.changeProductPrice(product.getId(), new BigDecimal("10.999")));

        assertTrue(productRepository.savedProducts.isEmpty());
    }

    @Test
    void shouldRejectNullIdBeforeConsultingRepository() {
        assertThrows(IllegalArgumentException.class, () -> service.findProductById(null));
        assertThrows(IllegalArgumentException.class, () -> service.renameProduct(null, "Gaming Keyboard"));
        assertThrows(IllegalArgumentException.class,
                () -> service.changeProductDescription(null, "Wireless"));
        assertThrows(IllegalArgumentException.class,
                () -> service.changeProductPrice(null, new BigDecimal("149.90")));

        assertEquals(0, productRepository.findByIdCalls);
        assertTrue(productRepository.savedProducts.isEmpty());
    }

    private static Product product(String name, String description, String price) {
        return new Product(ProductId.newId(), name, description, new BigDecimal(price));
    }

    private static final class RecordingProductRepository implements ProductRepository {

        private final List<Product> savedProducts = new ArrayList<>();
        private Optional<Product> productToFind = Optional.empty();
        private List<Product> products = List.of();
        private int findByIdCalls;

        @Override
        public void save(Product product) {
            savedProducts.add(product);
        }

        @Override
        public Optional<Product> findById(ProductId id) {
            findByIdCalls++;
            return productToFind;
        }

        @Override
        public List<Product> findAll() {
            return products;
        }
    }
}
