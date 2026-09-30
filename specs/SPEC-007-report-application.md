# SPEC-007 — Relatório de Vendas na Camada Application

## 1. Metadados

- **Estado:** Concluída
- **Dependências:** SPEC-002, SPEC-004 e SPEC-006 concluídas
- **Entrega:** Relatório de pedidos em um intervalo de tempo, com quantidade de pedidos, vendas pagas e totais por cliente, calculado a partir de `OrderRepository`

## 2. Objetivo

Permitir consultar, por código Java, um resumo de vendas de pedidos criados em determinado período. O relatório deve distinguir pedidos em rascunho de pedidos pagos, somar apenas vendas pagas e agrupar essas vendas por cliente, sem modificar pedidos nem depender de persistência concreta.

O incremento exercita filtragem, agregação, agrupamento e ordenação de coleções com a Stream API, quando isso tornar o cálculo mais claro.

## 3. Contexto

`Order` expõe `getCreatedAt()`, `getCustomerId()`, `getStatus()` e `getSubtotal()`. Após a SPEC-006, `OrderStatus` possui `DRAFT` e `PAID`; não existe total com desconto nem instante de pagamento. `OrderRepository.findAll()` já fornece os pedidos necessários, sem consulta adicional ou mudança no contrato do repository.

O período deste relatório se refere à **criação do pedido**, inclusive para pedidos pagos. O valor de uma venda é o subtotal do pedido pago, calculado pelo domínio a partir dos snapshots de seus itens. Nenhum cadastro de cliente precisa ser consultado para agrupar por `CustomerId`.

```text
Caller
   │
   ▼
ReportApplicationService
   └──► OrderRepository.findAll()
            └──► Order (createdAt, customerId, status, subtotal)
   │
   ▼
SalesReport (resumo e vendas por cliente)
```

## 4. Pré-condições

- SPEC-002, SPEC-004 e SPEC-006 concluídas.
- `Order`, `OrderStatus`, `CustomerId` e `OrderRepository.findAll()` disponíveis com os contratos descritos acima.
- Java 21 e Maven Wrapper disponíveis para compilar, testar e empacotar o projeto.

## 5. Escopo

Esta spec autoriza:

- criar um caso de uso de relatório de vendas em `application.report`;
- filtrar pedidos pelo instante de criação em um intervalo informado pelo chamador;
- contar todos os pedidos do período e, separadamente, os pedidos pagos;
- somar os subtotais apenas dos pedidos pagos;
- agrupar pedidos pagos por `CustomerId`, com contagem e valor por cliente;
- ordenar os grupos por valor decrescente, com desempate determinístico;
- devolver resultados de leitura sem expor a lista de pedidos do repository;
- criar testes unitários do caso de uso e dos contratos de resultado.

## 6. Fora do escopo

Não implementar nesta etapa:

- novos estados, campos, cálculos ou comportamentos em `Order`;
- descontos, taxas, frete, valor final diferente do subtotal ou devoluções;
- data de pagamento, relatório por data de pagamento ou histórico de tentativas;
- busca de nomes ou outros dados em `CustomerRepository`;
- filtros opcionais, múltiplos tipos de relatório, paginação, exportação ou gráficos;
- novas operações em `OrderRepository` ou implementação concreta de repository;
- cache, armazenamento de relatórios, JSON, arquivos, CLI ou interface web;
- regras de concorrência ou snapshots transacionais do repository;
- services genéricos, hierarquias de relatórios ou frameworks proibidos pelo `AGENTS.md`.

## 7. Requisitos funcionais

### RF-01 — Seleção por período

O caso de uso recebe `Instant startInclusive` e `Instant endExclusive` e considera somente pedidos cujo `createdAt` seja maior ou igual ao início e estritamente menor que o fim. O intervalo deve ter início estritamente anterior ao fim. O repository deve ser consultado uma vez para produzir o relatório.

### RF-02 — Resumo do período

`orderCount` representa todos os pedidos selecionados, independentemente de status. `paidOrderCount` representa apenas os pedidos `PAID`. `paidRevenue` é a soma dos subtotais dos pedidos `PAID`; pedidos `DRAFT` não contribuem para receita, mesmo que tenham itens.

### RF-03 — Vendas por cliente

Para cada cliente com ao menos um pedido `PAID` no período, produzir `CustomerSales` com `CustomerId`, quantidade de pedidos pagos e soma dos respectivos subtotais. Pedidos em rascunho não criam entradas e não alteram os valores de clientes que também tenham pedidos pagos.

As entradas são ordenadas por receita decrescente; em empate de valor, pela representação textual de `CustomerId` em ordem crescente. Não há consulta a `CustomerRepository`.

### RF-04 — Período sem vendas

Quando nenhum pedido se enquadrar no intervalo, o resumo deve ter contagens zero, receita `BigDecimal` zero e lista de clientes vazia. Quando houver somente pedidos em rascunho, `orderCount` deve refletir esses pedidos, enquanto `paidOrderCount`, `paidRevenue` e a lista de clientes permanecem zerados/vazios.

### RF-05 — Operação somente de leitura

Gerar o relatório não deve alterar pedidos nem chamar `OrderRepository.save`. A lista de grupos retornada deve ser estruturalmente não modificável e não deve conter entidades `Order`.

## 8. Requisitos técnicos

### RT-01 — Package e dependências

Implementar em `dev.manoelreis.ordermanagement.application.report`, com testes no package correspondente. Dependências de produção limitadas a `application.order.OrderRepository`, tipos de `domain.order`, `domain.customer` e APIs da JDK. Não adicionar bibliotecas nem dependências de CLI ou infraestrutura.

### RT-02 — Application service

`ReportApplicationService` deve ser uma classe concreta e final, receber obrigatoriamente `OrderRepository` pelo construtor e expor:

```text
generateSalesReport(Instant startInclusive, Instant endExclusive) -> SalesReport
```

O service deve usar `OrderRepository.findAll()` como fonte única de pedidos e `Order.getSubtotal()` como fonte única de valores. Não deve recalcular preços ou subtotais a partir de `Product` ou `OrderItem`.

### RT-03 — Resultados

`SalesReport` e `CustomerSales` devem ser records em `application.report`. Usar `long` para contagens e `BigDecimal` para valores monetários. `SalesReport` deve conter:

```text
startInclusive: Instant
endExclusive: Instant
orderCount: long
paidOrderCount: long
paidRevenue: BigDecimal
customerSales: List<CustomerSales>
```

`CustomerSales` deve conter:

```text
customerId: CustomerId
paidOrderCount: long
paidRevenue: BigDecimal
```

`SalesReport` deve copiar defensivamente a estrutura de `customerSales` ao ser construído. Os resultados não devem carregar referências a `Order` ou collections mutáveis do repository. Valores zero devem ser representados por `BigDecimal.ZERO` ou equivalente; comparações monetárias nos testes devem considerar o valor numérico, sem exigir escala específica.

### RT-04 — Processamento de coleções

Usar a Stream API para operações declarativas de filtragem, agrupamento, agregação e/ou ordenação, preservando legibilidade. O cálculo do resumo e dos grupos deve considerar a mesma seleção de pedidos, obtida em uma única chamada a `findAll()`.

### RT-05 — Simplicidade

Não introduzir uma interface de relatório, um novo repository, abstrações genéricas de agregação ou um serviço de busca. Usar injeção manual por construtor e JUnit Jupiter para testes; doubles manuais ou Mockito podem isolar `OrderRepository`.

## 9. Modelo e contratos

### 9.1 `ReportApplicationService`

Responsável por validar o intervalo, selecionar pedidos pelo instante de criação e montar o relatório a partir dos comportamentos públicos de `Order`. Não guarda estado entre chamadas nem persiste o resultado.

### 9.2 `SalesReport`

Resultado de uma consulta para o intervalo `[startInclusive, endExclusive)`. Suas contagens e receita resumem todos os pedidos selecionados conforme RF-02; `customerSales` detalha apenas os pedidos pagos. A lista possui ordem definida por RF-03 e é estruturalmente não modificável.

### 9.3 `CustomerSales`

Resultado agregado por identidade de cliente, sem referência à entidade `Customer`. O valor corresponde à soma dos subtotais dos pedidos pagos desse cliente selecionados no período.

### 9.4 `OrderRepository`

Reutilizar `findAll() -> List<Order>`, cuja lista deve ser não nula e pode estar vazia. A ordenação de entrada não determina a ordenação de `customerSales`. Não alterar esta interface para satisfazer o relatório.

## 10. Tratamento de erros

- `OrderRepository` nulo no construtor deve produzir `IllegalArgumentException`;
- limite de intervalo nulo ou início igual/posterior ao fim deve produzir `IllegalArgumentException` antes de consultar o repository;
- ausência de pedidos ou de vendas pagas é resultado válido, não exception;
- resultados não devem usar `null` ou valores sentinela para indicar ausência de vendas;
- falhas técnicas de `findAll()` devem ser propagadas, sem tradução ou persistência parcial;
- não há compromisso de produzir um snapshot atômico diante de alterações concorrentes no repository.

## 11. Arquivos afetados

A implementação futura desta spec poderá criar ou alterar somente:

```text
src/main/java/dev/manoelreis/ordermanagement/application/report/
src/test/java/dev/manoelreis/ordermanagement/application/report/
README.md
specs/SPEC-007-report-application.md
```

A criação desta specification também autoriza atualizar o índice em `specs/README.md`. Mudanças em `domain`, `application.order`, `infrastructure`, `cli` ou `pom.xml` não são necessárias para este incremento.

## 12. Estratégia de testes

Usar pedidos reais em memória e um double de `OrderRepository` para fornecer listas e observar a ausência de chamadas a `save`. Pagar pedidos de teste com uma estratégia simples que retorna `APPROVED`. Cobrir, no mínimo:

- dependência nula e limites nulos ou invertidos/iguais rejeitados antes de `findAll()`;
- inclusão do pedido criado exatamente no início e exclusão do criado exatamente no fim;
- `findAll()` chamado uma vez e nenhuma chamada a `save`;
- lista vazia e período sem pedidos, com contagens e receita zero;
- pedidos somente em rascunho, inclusive com subtotal positivo, sem receita;
- combinação de pedidos pagos e rascunhos para clientes distintos, verificando contagens e somas;
- dois ou mais pedidos pagos do mesmo cliente agrupados em uma entrada;
- ordenação por receita decrescente e desempate por `CustomerId`;
- lista de resultado estruturalmente não modificável e independente da lista fornecida pelo repository;
- subtotal baseado nos itens do pedido, sem consulta aos dados atuais de produtos ou clientes.

Testar contratos observáveis e limites relevantes sem reproduzir a lógica de agregação dentro dos testes.

## 13. Critérios de aceite

- **CA-01:** um único `generateSalesReport` gera o resumo para o intervalo de criação `[startInclusive, endExclusive)`.
- **CA-02:** o resumo distingue todos os pedidos de pedidos pagos e soma somente os subtotais pagos.
- **CA-03:** o detalhamento agrupa as vendas pagas por `CustomerId` e as ordena conforme RF-03.
- **CA-04:** períodos vazios e períodos só com rascunhos geram resultados definidos, sem exceptions.
- **CA-05:** intervalos inválidos são rejeitados antes da consulta; a consulta válida usa `findAll()` uma vez e não salva pedidos.
- **CA-06:** resultados não expõem `Order` nem lista mutável do repository.
- **CA-07:** `application.report` não depende de infraestrutura, CLI, Jackson ou novos contratos de repository.
- **CA-08:** testes relevantes e `./mvnw clean package` passam quando a spec for implementada.

## 14. Definition of Done

- [x] Requisitos funcionais atendidos
- [x] Requisitos técnicos atendidos
- [x] `ReportApplicationService`, `SalesReport` e `CustomerSales` implementados e testados
- [x] Testes relevantes implementados e aprovados
- [x] Build aprovado com `./mvnw clean package`
- [x] Nenhum item fora do escopo implementado
- [x] Documentação afetada atualizada
- [x] Estado da spec alterado para `Concluída` somente após a validação

## 15. Critério de parada

Parar quando o relatório de vendas por período, incluindo resumo e agrupamento por cliente, estiver disponível por código Java, coberto por testes e desacoplado de infraestrutura. Exportação, CLI, persistência concreta, novos tipos de relatório e conceitos financeiros adicionais exigem specifications próprias.
