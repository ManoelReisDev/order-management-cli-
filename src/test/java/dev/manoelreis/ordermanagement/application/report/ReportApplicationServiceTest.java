package dev.manoelreis.ordermanagement.application.report;

import dev.manoelreis.ordermanagement.application.order.OrderRepository;
import dev.manoelreis.ordermanagement.domain.customer.CustomerId;
import dev.manoelreis.ordermanagement.domain.order.Order;
import dev.manoelreis.ordermanagement.domain.order.OrderId;
import dev.manoelreis.ordermanagement.domain.order.OrderStatus;
import dev.manoelreis.ordermanagement.domain.payment.PaymentResult;
import dev.manoelreis.ordermanagement.domain.product.Product;
import dev.manoelreis.ordermanagement.domain.product.ProductId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportApplicationServiceTest {

    private static final Instant START = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-10-01T00:00:00Z");

    private RecordingOrderRepository repository;
    private ReportApplicationService service;

    @BeforeEach
    void setUp() {
        repository = new RecordingOrderRepository();
        service = new ReportApplicationService(repository);
    }

    @Test
    void shouldRejectMissingRepositoryAndInvalidIntervalsBeforeQuerying() {
        assertThrows(IllegalArgumentException.class, () -> new ReportApplicationService(null));
        assertThrows(IllegalArgumentException.class, () -> service.generateSalesReport(null, END));
        assertThrows(IllegalArgumentException.class, () -> service.generateSalesReport(START, null));
        assertThrows(IllegalArgumentException.class, () -> service.generateSalesReport(START, START));
        assertThrows(IllegalArgumentException.class, () -> service.generateSalesReport(END, START));

        assertEquals(0, repository.findAllCalls);
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void shouldIncludeStartAndExcludeEndUsingOrderCreationTime() {
        CustomerId customerId = CustomerId.newId();
        Order atStart = paidOrder(customerId, START, "12.50");
        Order atEnd = paidOrder(customerId, END, "99.00");
        Order beforeStart = paidOrder(customerId, START.minusNanos(1), "100.00");
        repository.orders = List.of(atEnd, beforeStart, atStart);

        SalesReport report = service.generateSalesReport(START, END);

        assertEquals(START, report.startInclusive());
        assertEquals(END, report.endExclusive());
        assertEquals(1, report.orderCount());
        assertEquals(1, report.paidOrderCount());
        assertMoney("12.50", report.paidRevenue());
        assertEquals(1, report.customerSales().size());
        assertEquals(customerId, report.customerSales().getFirst().customerId());
        assertEquals(1, repository.findAllCalls);
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void shouldReturnZeroSummaryForEmptyRepositoryOrPeriodWithoutOrders() {
        SalesReport empty = service.generateSalesReport(START, END);
        assertEquals(0, empty.orderCount());
        assertEquals(0, empty.paidOrderCount());
        assertMoney("0", empty.paidRevenue());
        assertTrue(empty.customerSales().isEmpty());

        repository.orders = List.of(paidOrder(CustomerId.newId(), END, "50.00"));
        SalesReport outsidePeriod = service.generateSalesReport(START, END);
        assertEquals(0, outsidePeriod.orderCount());
        assertMoney("0", outsidePeriod.paidRevenue());
        assertTrue(outsidePeriod.customerSales().isEmpty());
        assertEquals(2, repository.findAllCalls);
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void shouldCountDraftsWithoutTreatingTheirSubtotalsAsRevenue() {
        Order draft = order(CustomerId.newId(), START);
        draft.addProduct(product("100.00"), 2);
        repository.orders = List.of(draft);

        SalesReport report = service.generateSalesReport(START, END);

        assertEquals(1, report.orderCount());
        assertEquals(0, report.paidOrderCount());
        assertMoney("0", report.paidRevenue());
        assertTrue(report.customerSales().isEmpty());
        assertEquals(OrderStatus.DRAFT, draft.getStatus());
        assertMoney("200.00", draft.getSubtotal());
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void shouldAggregatePaidOrdersByCustomerAndSortByRevenueThenId() {
        CustomerId firstId = new CustomerId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        CustomerId secondId = new CustomerId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        CustomerId highestId = CustomerId.newId();
        Order first = paidOrder(firstId, START, "15.00");
        Order second = paidOrder(firstId, START.plusSeconds(1), "25.00");
        Order tied = paidOrder(secondId, START, "40.00");
        Order highest = paidOrder(highestId, START, "70.00");
        Order draft = order(firstId, START);
        draft.addProduct(product("500.00"), 1);
        Product changedProduct = product("10.00");
        Order snapshot = order(highestId, START);
        snapshot.addProduct(changedProduct, 3);
        snapshot.pay(amount -> PaymentResult.APPROVED);
        changedProduct.changePrice(new BigDecimal("900.00"));
        repository.orders = List.of(draft, tied, second, snapshot, highest, first);

        SalesReport report = service.generateSalesReport(START, END);

        assertEquals(6, report.orderCount());
        assertEquals(5, report.paidOrderCount());
        assertMoney("180.00", report.paidRevenue());
        assertEquals(List.of(highestId, firstId, secondId),
                report.customerSales().stream().map(CustomerSales::customerId).toList());
        assertEquals(List.of(2L, 2L, 1L),
                report.customerSales().stream().map(CustomerSales::paidOrderCount).toList());
        assertMoney("100.00", report.customerSales().get(0).paidRevenue());
        assertMoney("40.00", report.customerSales().get(1).paidRevenue());
        assertMoney("40.00", report.customerSales().get(2).paidRevenue());
        assertEquals(OrderStatus.DRAFT, draft.getStatus());
        assertEquals(1, repository.findAllCalls);
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void shouldReturnAnUnmodifiableReportIndependentOfRepositoryList() {
        Order order = paidOrder(CustomerId.newId(), START, "10.00");
        List<Order> repositoryOrders = new ArrayList<>(List.of(order));
        repository.orders = repositoryOrders;

        SalesReport report = service.generateSalesReport(START, END);
        repositoryOrders.clear();

        assertEquals(1, report.orderCount());
        assertEquals(1, report.customerSales().size());
        assertThrows(UnsupportedOperationException.class, () -> report.customerSales().clear());

        List<CustomerSales> mutableGroups = new ArrayList<>(report.customerSales());
        SalesReport copied = new SalesReport(START, END, 1, 1, report.paidRevenue(), mutableGroups);
        mutableGroups.clear();
        assertEquals(report.customerSales(), copied.customerSales());
        assertThrows(UnsupportedOperationException.class, () -> copied.customerSales().clear());
    }

    @Test
    void shouldPropagateRepositoryFailureWithoutSaving() {
        RuntimeException failure = new IllegalStateException("Repository unavailable");
        repository.failure = failure;

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> service.generateSalesReport(START, END)));
        assertEquals(1, repository.findAllCalls);
        assertEquals(0, repository.saveCalls);
    }

    private static Order order(CustomerId customerId, Instant createdAt) {
        return new Order(OrderId.newId(), customerId, createdAt);
    }

    private static Order paidOrder(CustomerId customerId, Instant createdAt, String price) {
        Order order = order(customerId, createdAt);
        order.addProduct(product(price), 1);
        order.pay(amount -> PaymentResult.APPROVED);
        return order;
    }

    private static Product product(String price) {
        return new Product(ProductId.newId(), "Product", "", new BigDecimal(price));
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private static final class RecordingOrderRepository implements OrderRepository {

        private List<Order> orders = List.of();
        private RuntimeException failure;
        private int findAllCalls;
        private int saveCalls;

        @Override
        public void save(Order order) {
            saveCalls++;
        }

        @Override
        public Optional<Order> findById(OrderId id) {
            throw new AssertionError("Reports must not look up individual orders");
        }

        @Override
        public List<Order> findAll() {
            findAllCalls++;
            if (failure != null) {
                throw failure;
            }
            return orders;
        }
    }
}
