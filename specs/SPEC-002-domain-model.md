# SPEC-002 — Modelo de Domínio Inicial

## 1. Metadados

- **Estado:** Concluída
- **Dependências:** SPEC-001 concluída
- **Entrega:** Modelo de domínio de clientes, produtos e pedidos, coberto por testes unitários

## 2. Objetivo

Implementar o primeiro modelo de domínio do **Order Management CLI** com Java puro. Esta etapa deve introduzir clientes, produtos, pedidos e itens de pedido, com identidade, estado encapsulado e invariantes explícitas.

O resultado esperado é um domínio utilizável diretamente por código Java e validado por testes unitários, ainda sem casos de uso, persistência ou interface de terminal.

## 3. Contexto

O projeto existe para exercitar os mecanismos fundamentais de Java antes da introdução de Spring. O domínio deve expressar conceitos de negócio sem conhecer Jackson, arquivos, terminal ou infraestrutura.

O modelo inicial é composto por:

```text
Customer
   │
   └── Order
        └── OrderItem
             └── Product
```

`OrderItem` deve preservar os dados comerciais do produto no momento em que ele é incluído no pedido. Alterações posteriores no cadastro do produto não podem modificar retroativamente o item.

## 4. Pré-condições

- A SPEC-001 deve estar concluída.
- O projeto deve compilar com Java 21 pelo Maven Wrapper.
- Os packages de domínio e de testes devem existir.
- Nenhuma dependência adicional deve ser necessária.

## 5. Escopo

Esta spec autoriza:

- criar identificadores tipados para cliente, produto e pedido;
- criar o modelo `Customer`;
- criar o modelo `Product`;
- criar `Order`, `OrderItem` e `OrderStatus`;
- validar os dados necessários para manter cada objeto válido;
- calcular subtotal de item e subtotal do pedido;
- incluir, alterar a quantidade e remover itens enquanto o pedido estiver em rascunho;
- proteger o estado interno e as coleções do domínio;
- criar exceptions de domínio estritamente necessárias às regras desta spec;
- criar testes unitários do domínio.

## 6. Fora do escopo

Não implementar nesta etapa:

- repositories ou qualquer interface de repository;
- services ou casos de uso da camada `application`;
- persistência em memória ou JSON;
- uso de Jackson no domínio;
- menus, entrada ou saída de terminal;
- descontos ou políticas de desconto;
- pagamentos ou métodos de pagamento;
- transições posteriores ao estado inicial do pedido;
- relatórios;
- estoque;
- frete, endereço de entrega ou cobrança;
- autenticação ou autorização;
- eventos de domínio;
- abstrações genéricas compartilhadas, como `BaseEntity` ou `GenericResult`.

## 7. Requisitos funcionais

### RF-01 — Identidade

`Customer`, `Product` e `Order` devem possuir identificadores tipados e imutáveis. Identificadores de conceitos diferentes não devem ser intercambiáveis pelo sistema de tipos.

### RF-02 — Cliente

Um cliente deve possuir:

- identificador;
- nome;
- e-mail.

Nome e e-mail são obrigatórios. Espaços externos devem ser removidos antes do armazenamento. O e-mail deve possuir uma validação mínima e legível de formato, sem tentar reproduzir integralmente a especificação de endereços eletrônicos.

### RF-03 — Produto

Um produto deve possuir:

- identificador;
- nome;
- descrição;
- preço atual.

Nome é obrigatório. Descrição pode ser vazia, mas nunca `null`. O preço deve ser representado por `BigDecimal`, ser maior que zero e possuir no máximo duas casas decimais.

### RF-04 — Criação do pedido

Um pedido deve possuir:

- identificador próprio;
- identificador do cliente;
- instante de criação;
- status;
- itens.

Todo pedido deve nascer com status `DRAFT` e sem itens. Identificador do cliente e instante de criação são obrigatórios.

### RF-05 — Item do pedido

Um item deve registrar:

- identificador do produto;
- nome do produto no momento da inclusão;
- preço unitário no momento da inclusão;
- quantidade.

A quantidade deve ser um número inteiro maior que zero. O subtotal do item deve ser `preço unitário × quantidade`.

### RF-06 — Inclusão de produto

Enquanto estiver em `DRAFT`, o pedido deve aceitar a inclusão de um produto com uma quantidade válida. Se o produto já estiver no pedido, a inclusão deve somar a nova quantidade ao item existente, mantendo um único item por identificador de produto.

### RF-07 — Alteração de quantidade

Enquanto estiver em `DRAFT`, a quantidade de um item existente pode ser substituída por outro valor maior que zero. A operação deve falhar quando o produto não fizer parte do pedido.

### RF-08 — Remoção de item

Enquanto estiver em `DRAFT`, um item pode ser removido pelo identificador do produto. A operação deve falhar quando o produto não fizer parte do pedido.

### RF-09 — Subtotal do pedido

O subtotal do pedido deve ser a soma dos subtotais dos itens. Um pedido sem itens deve retornar `BigDecimal.ZERO` com escala monetária de duas casas decimais.

### RF-10 — Igualdade

A igualdade de `Customer`, `Product` e `Order` deve ser determinada por seus respectivos identificadores. Os identificadores devem possuir igualdade por valor.

## 8. Requisitos técnicos

### RT-01 — Linguagem e packages

O código deve usar Java 21 e permanecer nos packages:

```text
dev.manoelreis.ordermanagement.domain.customer
dev.manoelreis.ordermanagement.domain.product
dev.manoelreis.ordermanagement.domain.order
dev.manoelreis.ordermanagement.domain.exception
```

### RT-02 — Independência do domínio

O domínio pode depender somente da Java Standard Library. Nenhum tipo ou annotation de Jackson, JUnit, Mockito, CLI, application ou infrastructure pode aparecer em `domain`.

### RT-03 — Identificadores

Os identificadores devem ser `record`s que encapsulam um `UUID` não nulo. Cada tipo deve fornecer uma forma explícita de gerar um novo identificador e uma forma de reconstruí-lo a partir de um `UUID` existente.

### RT-04 — Encapsulamento

Objetos de domínio não devem expor setters genéricos. Mudanças de estado devem ocorrer por operações com significado de negócio. A coleção de itens exposta por `Order` deve ser uma visão não modificável e não pode permitir alteração indireta do estado interno.

### RT-05 — Valores monetários

Valores monetários devem usar `BigDecimal` e escala 2. `float` e `double` são proibidos para preço e subtotal. A normalização deve ocorrer na entrada do domínio com `RoundingMode.UNNECESSARY`, rejeitando valores que exigiriam arredondamento.

### RT-06 — Data e hora

O instante de criação do pedido deve usar `Instant`. O domínio deve recebê-lo explicitamente na criação para manter testes determinísticos; não deve chamar diretamente `Instant.now()` em regras testadas.

### RT-07 — Coleções

O pedido deve preservar a ordem de inclusão dos itens e garantir um único item por produto. A escolha da collection interna deve atender às duas regras sem expor a implementação como parte do contrato público.

### RT-08 — Simplicidade

Não criar builders, factories genéricas, herança ou interfaces sem necessidade concreta. Métodos de criação nomeados são permitidos quando tornarem as invariantes claras.

## 9. Modelo e contratos

### 9.1 `CustomerId`

Value object imutável que encapsula um `UUID`.

Invariantes:

- o valor não pode ser `null`;
- a representação textual deve ser a representação do UUID encapsulado.

### 9.2 `Customer`

Entidade responsável por proteger identidade, nome e e-mail do cliente.

Operações autorizadas:

- criar um cliente válido;
- consultar seus dados;
- alterar o nome com a mesma validação da criação;
- alterar o e-mail com a mesma validação da criação.

### 9.3 `ProductId`

Value object imutável que encapsula um `UUID`, com as mesmas regras de `CustomerId`.

### 9.4 `Product`

Entidade responsável por identidade, nome, descrição e preço atual do produto.

Operações autorizadas:

- criar um produto válido;
- consultar seus dados;
- renomear o produto;
- alterar sua descrição;
- alterar seu preço.

Toda alteração deve reaplicar as invariantes do respectivo atributo.

### 9.5 `OrderId`

Value object imutável que encapsula um `UUID`, com as mesmas regras dos demais identificadores.

### 9.6 `OrderStatus`

Nesta spec, o enum deve declarar somente:

```text
DRAFT
```

Novos estados e suas transições pertencem à specification de workflow de pedidos.

### 9.7 `OrderItem`

Objeto imutável para consumidores externos ao agregado `Order`. Deve preservar o snapshot de identificador, nome, preço unitário e quantidade do produto.

Não deve manter uma referência mutável a `Product`. A alteração de quantidade deve produzir um estado ainda válido sem permitir que consumidores modifiquem diretamente um item pertencente ao pedido.

### 9.8 `Order`

Entidade e raiz responsável pela consistência de seus itens.

Operações autorizadas:

- criar um pedido em `DRAFT`;
- consultar identidade, cliente, criação, status e itens;
- adicionar produto e quantidade;
- alterar quantidade de um produto existente;
- remover um produto;
- calcular o subtotal.

As operações sobre itens devem pertencer a `Order`. Consumidores não devem inserir ou remover objetos diretamente da coleção.

## 10. Tratamento de erros

Dados inválidos de construção ou alteração devem produzir exceptions de domínio específicas e não mensagens silenciosas, valores sentinela ou objetos parcialmente válidos.

Esta spec autoriza a menor hierarquia necessária:

```text
DomainException
├── InvalidCustomerException
├── InvalidProductException
├── InvalidQuantityException
└── OrderItemNotFoundException
```

Regras:

- `DomainException` deve ser uma exception não verificada;
- a mensagem deve identificar a condição violada sem incluir dados sensíveis;
- `NullPointerException`, `NumberFormatException` e exceptions técnicas não devem compor o contrato normal do domínio;
- não criar `InvalidOrderStateException` enquanto `DRAFT` for o único estado desta spec.

## 11. Arquivos afetados

A implementação futura desta spec poderá criar ou alterar somente:

```text
src/main/java/dev/manoelreis/ordermanagement/domain/customer/
src/main/java/dev/manoelreis/ordermanagement/domain/product/
src/main/java/dev/manoelreis/ordermanagement/domain/order/
src/main/java/dev/manoelreis/ordermanagement/domain/exception/

src/test/java/dev/manoelreis/ordermanagement/domain/customer/
src/test/java/dev/manoelreis/ordermanagement/domain/product/
src/test/java/dev/manoelreis/ordermanagement/domain/order/
```

Também poderão ser atualizados `README.md` e esta spec para refletir o estado concluído. Não criar código em `application`, `infrastructure`, `cli`, `payment`, `discount` ou `shared`.

## 12. Estratégia de testes

Usar JUnit Jupiter. Mockito não deve ser usado porque os objetos desta spec não possuem dependências externas a isolar.

Cobrir, no mínimo:

- geração e reconstrução dos três tipos de identificador;
- rejeição de UUID nulo;
- criação e alteração válidas de cliente;
- rejeição de nome vazio e e-mail inválido;
- criação e alteração válidas de produto;
- rejeição de nome vazio, preço nulo, zero, negativo ou com mais de duas casas;
- criação de pedido vazio em `DRAFT` com instante informado;
- inclusão de produto e criação do snapshot do item;
- soma de quantidades ao incluir novamente o mesmo produto;
- alteração de quantidade;
- remoção de item;
- rejeição de quantidade zero ou negativa;
- erro ao alterar ou remover produto inexistente;
- subtotal de item e de pedido com escala monetária correta;
- impossibilidade de modificar externamente a coleção de itens;
- igualdade das entidades por identificador.

Os testes devem verificar comportamento público e invariantes, sem depender do tipo concreto da collection interna.

## 13. Critérios de aceite

- **CA-01:** clientes válidos podem ser criados e alterados sem expor setters genéricos.
- **CA-02:** produtos válidos podem ser criados e alterados com valores monetários de duas casas decimais, sem arredondamento implícito.
- **CA-03:** identificadores são tipados, imutáveis e rejeitam valores nulos.
- **CA-04:** pedidos nascem vazios e em `DRAFT`.
- **CA-05:** produtos podem ser incluídos, ter quantidade alterada e ser removidos somente pelas operações de `Order`.
- **CA-06:** incluir o mesmo produto acumula quantidade sem duplicar o item.
- **CA-07:** itens preservam nome e preço do produto no momento da inclusão.
- **CA-08:** subtotais são calculados corretamente com `BigDecimal` e duas casas decimais.
- **CA-09:** estados inválidos são rejeitados por exceptions de domínio específicas.
- **CA-10:** nenhum código do domínio depende de Jackson ou das demais camadas.
- **CA-11:** todos os testes unitários e o build Maven passam.

## 14. Definition of Done

- [x] `CustomerId` e `Customer` implementados e testados
- [x] `ProductId` e `Product` implementados e testados
- [x] `OrderId`, `OrderStatus`, `OrderItem` e `Order` implementados e testados
- [x] Invariantes e exceptions de domínio implementadas e testadas
- [x] Encapsulamento da coleção de itens verificado
- [x] Valores monetários tratados com `BigDecimal`, escala 2 e sem arredondamento implícito
- [x] Nenhuma dependência externa introduzida no domínio
- [x] `./mvnw test` executado com sucesso
- [x] `./mvnw clean package` executado com sucesso
- [x] Nenhum item fora do escopo implementado
- [x] Estado da spec alterado para `Concluída` após a validação

## 15. Critério de parada

Parar quando clientes, produtos, pedidos e itens cumprirem os contratos desta spec e seus testes estiverem aprovados.

Não iniciar repositories, casos de uso, persistência, descontos, pagamentos, workflow posterior a `DRAFT`, relatórios ou CLI. Essas capacidades devem ser definidas e autorizadas por specifications futuras.
