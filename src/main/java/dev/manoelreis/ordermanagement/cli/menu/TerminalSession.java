package dev.manoelreis.ordermanagement.cli.menu;

import dev.manoelreis.ordermanagement.application.customer.CustomerApplicationService;
import dev.manoelreis.ordermanagement.application.customer.CustomerNotFoundException;
import dev.manoelreis.ordermanagement.application.order.OrderApplicationService;
import dev.manoelreis.ordermanagement.application.order.OrderNotFoundException;
import dev.manoelreis.ordermanagement.application.order.OrderRepository;
import dev.manoelreis.ordermanagement.application.payment.PaymentApplicationService;
import dev.manoelreis.ordermanagement.application.product.ProductApplicationService;
import dev.manoelreis.ordermanagement.application.product.ProductNotFoundException;
import dev.manoelreis.ordermanagement.cli.input.TerminalInput;
import dev.manoelreis.ordermanagement.cli.output.TerminalOutput;
import dev.manoelreis.ordermanagement.domain.customer.Customer;
import dev.manoelreis.ordermanagement.domain.customer.CustomerId;
import dev.manoelreis.ordermanagement.domain.exception.InvalidCustomerException;
import dev.manoelreis.ordermanagement.domain.exception.InvalidProductException;
import dev.manoelreis.ordermanagement.domain.exception.DomainException;
import dev.manoelreis.ordermanagement.domain.exception.InvalidQuantityException;
import dev.manoelreis.ordermanagement.domain.exception.OrderItemNotFoundException;
import dev.manoelreis.ordermanagement.domain.order.Order;
import dev.manoelreis.ordermanagement.domain.order.OrderId;
import dev.manoelreis.ordermanagement.domain.product.Product;
import dev.manoelreis.ordermanagement.domain.product.ProductId;
import dev.manoelreis.ordermanagement.domain.payment.PaymentMethod;
import dev.manoelreis.ordermanagement.domain.payment.PaymentResult;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

public final class TerminalSession {

    private final CustomerApplicationService customers;
    private final ProductApplicationService products;
    private final OrderApplicationService orders;
    private final PaymentApplicationService payments;
    private final TerminalInput input;
    private final TerminalOutput output;
    private boolean finished;

    public TerminalSession(CustomerApplicationService customers, ProductApplicationService products,
                           OrderApplicationService orders,
                           PaymentApplicationService payments,
                           Reader input, Writer output) {
        if (customers == null || products == null || orders == null || payments == null
                || input == null || output == null) {
            throw new IllegalArgumentException("Services, input and output are required");
        }
        this.customers = customers;
        this.products = products;
        this.orders = orders;
        this.payments = payments;
        this.input = new TerminalInput(input);
        this.output = new TerminalOutput(output);
    }

    /** Retained for callers of the earlier CLI flows; payment operations require the new constructor. */
    public TerminalSession(CustomerApplicationService customers, ProductApplicationService products,
                           OrderApplicationService orders, Reader input, Writer output) {
        this(customers, products, orders, new PaymentApplicationService(new OrderRepository() {
            @Override public void save(Order order) { }
            @Override public Optional<Order> findById(OrderId id) { return Optional.empty(); }
            @Override public List<Order> findAll() { return List.of(); }
        }), input, output);
    }

    public void run() throws IOException {
        if (finished) {
            return;
        }
        try {
            output.write("A sessão pode usar dados somente em memória ou persistidos automaticamente, conforme a composição.\n");
            boolean active = true;
            while (active) {
                output.showMenu();
                String option = read("Escolha uma opção: ");
                if (option == null) {
                    break;
                }
                switch (option.trim()) {
                    case "0" -> active = false;
                    case "1" -> active = registerCustomer();
                    case "2" -> active = registerProduct();
                    case "3" -> listCustomers();
                    case "4" -> listProducts();
                    case "5" -> active = findCustomer();
                    case "6" -> active = findProduct();
                    case "7" -> active = createOrder();
                    case "8" -> listOrders();
                    case "9" -> active = findOrder();
                    case "10" -> active = addProductToOrder();
                    case "11" -> active = changeOrderItemQuantity();
                    case "12" -> active = removeOrderItem();
                    case "13" -> active = processPayment();
                    default -> output.write("Opção inválida. Escolha um código de 0 a 13.\n");
                }
            }
            output.write("Sessão encerrada.\n");
        } finally {
            finished = true;
        }
    }

    private boolean registerCustomer() throws IOException {
        String name = read("Nome do cliente: ");
        if (name == null) {
            return false;
        }
        String email = read("E-mail: ");
        if (email == null) {
            return false;
        }
        try {
            Customer customer = customers.registerCustomer(name, email);
            output.write("Cliente cadastrado com sucesso.\n");
            output.showCustomer(customer);
        } catch (InvalidCustomerException exception) {
            output.write("Dados de cliente inválidos.\n");
        }
        return true;
    }

    private boolean registerProduct() throws IOException {
        String name = read("Nome do produto: ");
        if (name == null) {
            return false;
        }
        String description = read("Descrição (pode ser vazia): ");
        if (description == null) {
            return false;
        }
        String text = read("Preço (use ponto decimal, por exemplo 100.00): ");
        if (text == null) {
            return false;
        }
        var parsedPrice = TerminalInput.parsePrice(text);
        if (parsedPrice.isEmpty()) {
            output.write("Formato de preço inválido. Use dígitos e ponto decimal, por exemplo 100.00.\n");
            return true;
        }
        BigDecimal price = parsedPrice.get();
        try {
            Product product = products.registerProduct(name, description, price);
            output.write("Produto cadastrado com sucesso.\n");
            output.showProduct(product);
        } catch (InvalidProductException exception) {
            output.write("Dados de produto inválidos. Informe um nome e preço positivo com até duas casas decimais.\n");
        }
        return true;
    }

    private boolean findCustomer() throws IOException {
        String text = read("ID do cliente (UUID completo): ");
        if (text == null) {
            return false;
        }
        UUID id = parseId(text);
        if (id != null) {
            try {
                output.showCustomer(customers.findCustomerById(CustomerId.of(id)));
            } catch (CustomerNotFoundException exception) {
                output.write("Cliente não encontrado: " + id + "\n");
            }
        }
        return true;
    }

    private boolean findProduct() throws IOException {
        String text = read("ID do produto (UUID completo): ");
        if (text == null) {
            return false;
        }
        UUID id = parseId(text);
        if (id != null) {
            try {
                output.showProduct(products.findProductById(ProductId.of(id)));
            } catch (ProductNotFoundException exception) {
                output.write("Produto não encontrado: " + id + "\n");
            }
        }
        return true;
    }

    private boolean createOrder() throws IOException {
        String text = read("ID do cliente (UUID completo): ");
        if (text == null) {
            return false;
        }
        UUID id = parseId(text);
        if (id == null) {
            return true;
        }
        try {
            Order order = orders.createOrder(CustomerId.of(id));
            output.write("Pedido criado com sucesso.\n");
            output.showOrder(order);
        } catch (CustomerNotFoundException exception) {
            output.write("Cliente não encontrado: " + id + "\n");
        }
        return true;
    }

    private boolean findOrder() throws IOException {
        String text = read("ID do pedido (UUID completo): ");
        if (text == null) {
            return false;
        }
        UUID id = parseId(text);
        if (id == null) {
            return true;
        }
        try {
            output.showOrder(orders.findOrderById(OrderId.of(id)));
        } catch (OrderNotFoundException exception) {
            output.write("Pedido não encontrado: " + id + "\n");
        }
        return true;
    }

    private boolean addProductToOrder() throws IOException {
        String orderText = read("ID do pedido (UUID completo): ");
        if (orderText == null) {
            return false;
        }
        UUID orderId = parseId(orderText);
        if (orderId == null) {
            return true;
        }
        String productText = read("ID do produto (UUID completo): ");
        if (productText == null) {
            return false;
        }
        UUID productId = parseId(productText);
        if (productId == null) {
            return true;
        }
        String quantityText = read("Quantidade: ");
        if (quantityText == null) {
            return false;
        }
        Integer quantity = parseQuantity(quantityText);
        if (quantity == null) {
            return true;
        }
        try {
            Product product = products.findProductById(ProductId.of(productId));
            Order order = orders.addProductToOrder(OrderId.of(orderId), product, quantity);
            output.write("Produto adicionado ao pedido com sucesso.\n");
            output.showOrder(order);
        } catch (ProductNotFoundException exception) {
            output.write("Produto não encontrado: " + productId + "\n");
        } catch (OrderNotFoundException exception) {
            output.write("Pedido não encontrado: " + orderId + "\n");
        } catch (InvalidQuantityException exception) {
            output.write("Quantidade inválida: " + quantity + ".\n");
        } catch (DomainException exception) {
            output.write("Operação rejeitada pelo estado do pedido.\n");
        }
        return true;
    }

    private boolean changeOrderItemQuantity() throws IOException {
        String orderText = read("ID do pedido (UUID completo): ");
        if (orderText == null) {
            return false;
        }
        UUID orderId = parseId(orderText);
        if (orderId == null) {
            return true;
        }
        String productText = read("ID do produto do item (UUID completo): ");
        if (productText == null) {
            return false;
        }
        UUID productId = parseId(productText);
        if (productId == null) {
            return true;
        }
        String quantityText = read("Nova quantidade: ");
        if (quantityText == null) {
            return false;
        }
        Integer quantity = parseQuantity(quantityText);
        if (quantity == null) {
            return true;
        }
        try {
            Order order = orders.changeOrderItemQuantity(
                    OrderId.of(orderId), ProductId.of(productId), quantity);
            output.write("Quantidade do item alterada com sucesso.\n");
            output.showOrder(order);
        } catch (OrderNotFoundException exception) {
            output.write("Pedido não encontrado: " + orderId + "\n");
        } catch (InvalidQuantityException exception) {
            output.write("Quantidade inválida: " + quantity + ".\n");
        } catch (OrderItemNotFoundException exception) {
            output.write("O produto não identifica um item deste pedido: " + productId + "\n");
        } catch (DomainException exception) {
            output.write("Operação rejeitada pelo estado do pedido.\n");
        }
        return true;
    }

    private boolean removeOrderItem() throws IOException {
        String orderText = read("ID do pedido (UUID completo): ");
        if (orderText == null) {
            return false;
        }
        UUID orderId = parseId(orderText);
        if (orderId == null) {
            return true;
        }
        String productText = read("ID do produto do item (UUID completo): ");
        if (productText == null) {
            return false;
        }
        UUID productId = parseId(productText);
        if (productId == null) {
            return true;
        }
        try {
            Order order = orders.removeOrderItem(OrderId.of(orderId), ProductId.of(productId));
            output.write("Item removido do pedido com sucesso.\n");
            output.showOrder(order);
        } catch (OrderNotFoundException exception) {
            output.write("Pedido não encontrado: " + orderId + "\n");
        } catch (OrderItemNotFoundException exception) {
            output.write("O produto não identifica um item deste pedido: " + productId + "\n");
        } catch (DomainException exception) {
            output.write("Operação rejeitada pelo estado do pedido.\n");
        }
        return true;
    }

    private boolean processPayment() throws IOException {
        String orderText = read("ID do pedido (UUID completo): ");
        if (orderText == null) {
            return false;
        }
        UUID orderId = parseId(orderText);
        if (orderId == null) {
            return true;
        }
        String resultText = read("Resultado simulado (1 - aprovar, 2 - recusar): ");
        if (resultText == null) {
            return false;
        }
        PaymentMethod method = switch (resultText.trim()) {
            case "1" -> amount -> PaymentResult.APPROVED;
            case "2" -> amount -> PaymentResult.DECLINED;
            default -> null;
        };
        if (method == null) {
            output.write("Escolha de resultado inválida. Use 1 para aprovar ou 2 para recusar.\n");
            return true;
        }
        try {
            PaymentResult result = payments.processPayment(OrderId.of(orderId), method);
            if (result == PaymentResult.APPROVED) {
                output.write("Pagamento aprovado.\n");
            } else {
                output.write("Pagamento recusado.\n");
            }
        } catch (OrderNotFoundException exception) {
            output.write("Pedido não encontrado: " + orderId + "\n");
        } catch (DomainException exception) {
            output.write("Operação rejeitada pelo estado do pedido.\n");
        }
        return true;
    }

    private Integer parseQuantity(String text) throws IOException {
        var quantity = TerminalInput.parseQuantity(text);
        if (quantity.isEmpty()) {
            output.write("Formato de quantidade inválido. Use um número inteiro.\n");
            return null;
        }
        return quantity.get();
    }

    private UUID parseId(String text) throws IOException {
        var parsedId = TerminalInput.parseId(text);
        if (parsedId.isEmpty()) {
            output.write("UUID inválido. Informe o identificador completo no formato 8-4-4-4-12.\n");
            return null;
        }
        return parsedId.get();
    }

    private void listCustomers() throws IOException {
        var records = customers.listCustomers();
        if (records.isEmpty()) {
            output.write("Nenhum cliente cadastrado.\n");
        }
        for (Customer customer : records) {
            output.showCustomer(customer);
        }
    }

    private void listProducts() throws IOException {
        var records = products.listProducts();
        if (records.isEmpty()) {
            output.write("Nenhum produto cadastrado.\n");
        }
        for (Product product : records) {
            output.showProduct(product);
        }
    }

    private void listOrders() throws IOException {
        var records = orders.listOrders();
        if (records.isEmpty()) {
            output.write("Nenhum pedido cadastrado.\n");
        }
        for (Order order : records) {
            output.showOrder(order);
        }
    }

    private String read(String prompt) throws IOException {
        output.write(prompt);
        return input.readLine();
    }
}
