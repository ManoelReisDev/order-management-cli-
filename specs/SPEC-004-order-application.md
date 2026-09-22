# SPEC-004 — Casos de Uso de Order

## 1. Metadados

- **Estado:** Concluída
- **Dependências:** SPEC-002 e SPEC-003 concluídas
- **Entrega:** Casos de uso de criação, consulta, listagem e manutenção dos itens de pedidos em rascunho, coordenados pela camada `application` por meio de uma abstração de repository

## 2. Objetivo

Introduzir os casos de uso da camada `application` para o contexto de pedidos.

Esta spec deve permitir criar um pedido para um cliente existente, consultá-lo por identificador, listar pedidos e coordenar a inclusão, a alteração de quantidade e a remoção de itens. A aplicação deve utilizar o modelo de domínio criado na SPEC-002, reutilizar a abstração de clientes criada na SPEC-003 e depender de uma abstração própria para armazenar pedidos.

O resultado esperado é uma API de aplicação utilizável diretamente por código Java e coberta por testes unitários. Nenhuma interface de usuário ou implementação concreta de persistência deve ser criada nesta etapa.

## 3. Contexto

A SPEC-002 introduziu `Order`, `OrderId`, `OrderItem` e `OrderStatus`, incluindo as regras de itens e subtotal. A SPEC-003 introduziu `CustomerRepository` e o tratamento explícito de clientes inexistentes.

A camada `application` deve agora coordenar operações completas de pedido sem duplicar regras do domínio. O fluxo desta etapa é:

```text
Caller
   │
   ▼
OrderApplicationService
   ├──► Order
   ├──► OrderRepository
   ├──► CustomerRepository
   └──► Clock
```

`OrderRepository` representa somente a necessidade de armazenamento observada pelos casos de uso. `CustomerRepository` é reutilizado para verificar se o cliente existe antes da criação do pedido. `Clock` representa a fonte de tempo da aplicação e evita acesso direto e não determinístico a `Instant.now()`.

Ainda não existe uma abstração de repository para produtos. Por isso, a inclusão de item recebe um `Product` já obtido pelo chamador. Criar ou consultar produtos pertence a uma specification própria e não deve ser antecipado aqui.

## 4. Pré-condições

- A SPEC-002 deve estar concluída.
- A SPEC-003 deve estar concluída.
- `Order`, `OrderId`, `OrderItem`, `Product`, `ProductId` e `CustomerId` devem estar implementados e testados.
- `CustomerRepository` e `CustomerNotFoundException` devem estar disponíveis na camada `application.customer`.
- O projeto deve compilar e executar os testes com Java 21 pelo Maven Wrapper.
- Nenhuma dependência adicional deve ser necessária.

## 5. Escopo

Esta spec autoriza:

- criar uma abstração de repository específica para `Order`;
- criar um application service específico para os casos de uso de pedido;
- criar um pedido em rascunho para um cliente existente;
- obter o instante de criação por meio de um `Clock` injetado;
- buscar um pedido por identificador;
- listar os pedidos fornecidos pelo repository;
- adicionar um produto e sua quantidade a um pedido existente;
- alterar a quantidade de um item de pedido existente;
- remover um item de pedido existente;
- representar explicitamente a tentativa de operar sobre um pedido inexistente;
- reutilizar o tratamento de cliente inexistente definido na SPEC-003;
- aplicar injeção de dependência manual por construtor;
- criar testes unitários da camada `application.order`.

## 6. Fora do escopo

Não implementar nesta etapa:

- implementações de repository em memória, JSON ou qualquer outro mecanismo;
- criação de `ProductRepository` ou casos de uso de cadastro e consulta de produtos;
- busca de produto por identificador durante a inclusão de item;
- leitura ou escrita de arquivos;
- Jackson na camada `application`;
- menus, comandos, entrada ou saída de terminal;
- novos estados de pedido ou transições de status;
- confirmação, cancelamento ou conclusão de pedidos;
- descontos, pagamentos, estoque, frete ou relatórios;
- exclusão de pedidos;
- filtros, paginação, ordenação ou busca de pedidos por cliente, status ou período;
- DTOs, mappers, controllers, presenters, commands ou results;
- repositories genéricos, services genéricos ou classes base;
- alterações no modelo de domínio criado pela SPEC-002;
- tratamento transacional ou concorrência;
- Spring, container de injeção de dependência ou qualquer tecnologia proibida pelo `AGENTS.md`.

## 7. Requisitos funcionais

### RF-01 — Criação de pedido

A aplicação deve receber um `CustomerId`, verificar sua existência por meio de `CustomerRepository`, gerar um novo `OrderId`, obter o instante atual por meio do `Clock`, criar um `Order` e solicitar seu armazenamento ao `OrderRepository`.

O pedido criado deve ser retornado ao chamador vazio e com status `DRAFT`, conforme definido pelo domínio.

Quando o cliente não existir, a aplicação deve sinalizar `CustomerNotFoundException` e não deve criar nem armazenar um pedido.

### RF-02 — Consulta por identificador

A aplicação deve consultar um pedido por `OrderId`.

Quando o repository encontrar o pedido, a aplicação deve retorná-lo. Quando não encontrar, deve sinalizar `OrderNotFoundException`.

### RF-03 — Listagem de pedidos

A aplicação deve retornar todos os pedidos fornecidos pelo repository, preservando a ordem recebida.

Quando não houver pedidos, deve retornar uma lista vazia. O chamador não deve conseguir adicionar ou remover elementos da lista retornada.

### RF-04 — Inclusão de produto

A aplicação deve localizar o pedido pelo identificador, solicitar ao próprio objeto de domínio a inclusão do `Product` com a quantidade informada e solicitar ao repository o armazenamento do estado alterado.

A operação deve retornar o pedido alterado. A criação do snapshot do produto, o acúmulo de quantidade e o cálculo do subtotal permanecem sob responsabilidade de `Order`.

### RF-05 — Alteração de quantidade

A aplicação deve localizar o pedido pelo identificador, solicitar ao próprio objeto de domínio a alteração da quantidade do item identificado por `ProductId` e solicitar ao repository o armazenamento do estado alterado.

A operação deve retornar o pedido alterado.

### RF-06 — Remoção de item

A aplicação deve localizar o pedido pelo identificador, solicitar ao próprio objeto de domínio a remoção do item identificado por `ProductId` e solicitar ao repository o armazenamento do estado alterado.

A operação deve retornar o pedido alterado.

### RF-07 — Preservação das regras de domínio

A camada `application` não deve reimplementar validação de quantidade, gerenciamento de itens, preservação do snapshot nem cálculo de subtotal. As operações devem usar os comportamentos públicos de `Order` e propagar as exceptions de domínio definidas na SPEC-002.

Se uma alteração for rejeitada pelo domínio, a aplicação não deve solicitar o armazenamento do estado inválido.

## 8. Requisitos técnicos

### RT-01 — Linguagem e package

O código deve usar Java 21 e permanecer no package:

```text
dev.manoelreis.ordermanagement.application.order
```

Os testes devem acompanhar esse package na árvore de testes.

### RT-02 — Direção de dependências

`application.order` pode depender de `domain.order`, `domain.product`, `domain.customer`, `application.customer` e das APIs da Java Standard Library.

O domínio não pode depender de `application`. A camada `application` não pode depender de `infrastructure`, `cli`, Jackson ou detalhes de persistência.

### RT-03 — Abstração de repository

`OrderRepository` deve ser uma interface específica para pedidos e declarar somente as operações exigidas nesta spec:

```text
save(Order order)
findById(OrderId id) -> Optional<Order>
findAll() -> List<Order>
```

O contrato não deve revelar estruturas ou detalhes de uma futura implementação de persistência.

### RT-04 — Application service

`OrderApplicationService` deve ser uma classe concreta e final. Ela deve receber `OrderRepository`, `CustomerRepository` e `Clock` obrigatoriamente pelo construtor e não deve criar, localizar ou escolher implementações concretas dessas dependências.

O service deve oferecer operações com nomes que expressem os casos de uso:

```text
createOrder(customerId)
findOrderById(orderId)
listOrders()
addProductToOrder(orderId, product, quantity)
changeOrderItemQuantity(orderId, productId, quantity)
removeOrderItem(orderId, productId)
```

### RT-05 — Fonte de tempo

O instante usado na criação do pedido deve ser obtido por `clock.instant()`. O service não deve chamar `Instant.now()` diretamente nem receber o instante como dado do caso de uso.

Os testes devem usar `Clock.fixed` para tornar o comportamento determinístico.

### RT-06 — Optional

`OrderRepository.findById` e o `CustomerRepository.findById` já existente devem usar `Optional` para representar ausência legítima de resultado. O application service deve resolver essas ausências e não deve retornar `null` nem `Optional` nos casos de uso definidos nesta spec.

### RT-07 — Collections

`OrderRepository.findAll` deve retornar `List<Order>`. `OrderApplicationService.listOrders` deve fornecer uma cópia estrutural não modificável da lista recebida, preservando sua ordem e sem expor a collection do repository.

Esta proteção se refere à estrutura da lista; não exige copiar as entidades contidas nela.

### RT-08 — Dependências e simplicidade

Nenhuma dependência de produção deve ser adicionada. JUnit Jupiter deve ser usado nos testes, e Mockito pode ser usado somente para isolar `OrderRepository` e `CustomerRepository`.

Não criar DTOs ou abstrações adicionais enquanto os parâmetros e retornos do domínio forem suficientes para estes casos de uso.

## 9. Modelo e contratos

### 9.1 `OrderRepository`

Porta de saída utilizada pela aplicação para armazenar e recuperar pedidos.

Responsabilidades:

- armazenar o pedido informado por `save`;
- representar o resultado de `findById` com `Optional`;
- fornecer por `findAll` uma lista não nula, possivelmente vazia.

Esta interface não define nesta etapa:

- tecnologia de persistência;
- comportamento entre execuções do processo;
- ordenação própria;
- controle transacional;
- consultas além das estritamente necessárias.

### 9.2 `OrderApplicationService`

Coordenador dos casos de uso de pedido.

Responsabilidades:

- confirmar a existência do cliente antes de criar um pedido;
- gerar a identidade e obter o instante da criação;
- criar e modificar pedidos por meio do modelo de domínio;
- usar `OrderRepository` para consulta e armazenamento;
- converter a ausência de um pedido em erro explícito de aplicação;
- proteger a estrutura da lista devolvida ao chamador.

O service não deve conter regras sobre quantidades, itens ou subtotal, nem conhecer como clientes e pedidos são armazenados.

### 9.3 `OrderNotFoundException`

Exception não verificada da camada `application.order`, utilizada quando um caso de uso exige um pedido existente e `OrderRepository.findById` não o encontra.

A exception deve identificar o `OrderId` procurado em sua mensagem e não deve ser adicionada à hierarquia de exceptions de domínio.

### 9.4 Colaboração com `application.customer`

`CustomerRepository` deve ser reutilizado sem alteração. A ausência do cliente durante a criação deve continuar sendo representada por `CustomerNotFoundException`.

`OrderApplicationService` não deve depender de `CustomerApplicationService`, pois necessita apenas da porta de consulta já existente. Nenhuma nova operação deve ser adicionada ao contrato de clientes para atender esta spec.

### 9.5 Colaboração com `Product`

`addProductToOrder` deve receber um `Product` válido já resolvido pelo chamador. O service não deve criar, reconstruir nem consultar produtos.

Essa decisão não estabelece o contrato definitivo dos futuros casos de uso de produto; apenas evita antecipar `ProductRepository` nesta spec.

## 10. Tratamento de erros

- `OrderRepository`, `CustomerRepository` ou `Clock` ausente na construção do application service deve ser rejeitado imediatamente;
- `CustomerId` nulo na criação deve ser rejeitado como argumento inválido antes de consultar repositories ou o relógio;
- `OrderId` nulo nas operações de consulta ou alteração deve ser rejeitado como argumento inválido antes de consultar o repository;
- cliente ausente na criação deve produzir `CustomerNotFoundException` e impedir chamada a `OrderRepository.save`;
- pedido ausente deve produzir `OrderNotFoundException` e impedir chamada a `OrderRepository.save`;
- `Product` ou `ProductId` nulo deve ser delegado ao comportamento público correspondente de `Order`, preservando a exception já definida pelo domínio;
- quantidade inválida ou item inexistente deve continuar produzindo a exception de domínio definida na SPEC-002;
- uma falha deve interromper o caso de uso e impedir uma chamada posterior a `save`;
- exceptions técnicas dos repositories ou do `Clock` não devem ser ocultadas, traduzidas ou envolvidas nesta etapa;
- não retornar `null`, valores sentinela ou mensagens textuais para representar falhas.

## 11. Arquivos afetados

A implementação futura desta spec poderá criar ou alterar somente:

```text
src/main/java/dev/manoelreis/ordermanagement/application/order/
src/test/java/dev/manoelreis/ordermanagement/application/order/
README.md
specs/SPEC-004-order-application.md
```

Esta criação da specification também autoriza atualizar o índice em `specs/README.md`.

Não alterar `domain`, `infrastructure`, `cli`, `pom.xml`, `application.customer` ou outros contextos de `application` para implementar esta spec.

## 12. Estratégia de testes

Usar JUnit Jupiter e objetos reais de `Customer`, `Order` e `Product`. Usar doubles manuais ou Mockito somente para isolar os contratos `OrderRepository` e `CustomerRepository`. Usar `Clock.fixed` como fonte de tempo.

Cobrir, no mínimo:

- rejeição de cada dependência nula na construção do service;
- criação de pedido para cliente existente com novo identificador, instante fixo, status `DRAFT`, itens vazios e chamada a `save`;
- `CustomerNotFoundException` na criação para cliente inexistente, sem chamada a `save`;
- rejeição de `CustomerId` nulo antes de consultar repositories ou o relógio;
- consulta bem-sucedida por identificador;
- `OrderNotFoundException` na consulta de identificador inexistente;
- listagem vazia;
- listagem que preserva a ordem fornecida pelo repository;
- impossibilidade de adicionar ou remover elementos da lista retornada;
- inclusão bem-sucedida de produto seguida de `save`;
- inclusão repetida do mesmo produto com acúmulo de quantidade definido pelo domínio;
- alteração de quantidade bem-sucedida seguida de `save`;
- remoção de item bem-sucedida seguida de `save`;
- falha das alterações quando o pedido não existe, sem chamada a `save`;
- propagação das exceptions de domínio para produto nulo, quantidade inválida ou item inexistente, sem chamada a `save`;
- rejeição de `OrderId` nulo antes de qualquer consulta ao repository.

Os testes devem observar resultados e interações relevantes, sem testar detalhes privados, reproduzir a implementação ou depender de uma futura classe de infraestrutura.

## 13. Critérios de aceite

- **CA-01:** um pedido pode ser criado para um cliente existente com novo `OrderId` e o instante fornecido pelo `Clock`.
- **CA-02:** a tentativa de criar pedido para cliente inexistente produz `CustomerNotFoundException` e não armazena pedido.
- **CA-03:** pedidos podem ser consultados por identificador sem expor `Optional` ao chamador do application service.
- **CA-04:** a ausência de pedido produz `OrderNotFoundException` com o identificador procurado.
- **CA-05:** pedidos podem ser listados na ordem fornecida pelo repository por meio de uma lista estruturalmente não modificável.
- **CA-06:** produtos podem ser adicionados e itens podem ter sua quantidade alterada ou ser removidos usando os comportamentos de `Order`.
- **CA-07:** cada alteração válida é enviada ao repository, e alterações rejeitadas não resultam em chamada a `save`.
- **CA-08:** regras de quantidade, snapshot, itens e subtotal permanecem no domínio.
- **CA-09:** as dependências são recebidas manualmente pelo construtor e a criação de pedidos é determinística em testes.
- **CA-10:** `application.order` não depende de infraestrutura, CLI ou Jackson.
- **CA-11:** nenhuma implementação concreta de repository ou abstração de repository para produtos é criada.
- **CA-12:** todos os testes e o build Maven passam.

## 14. Definition of Done

- [x] Requisitos funcionais atendidos
- [x] Requisitos técnicos atendidos
- [x] `OrderRepository` implementado como abstração específica
- [x] `OrderApplicationService` implementado e testado
- [x] `OrderNotFoundException` implementada e testada pelos casos de uso
- [x] Testes relevantes implementados e aprovados
- [x] Build aprovado com `./mvnw clean package`
- [x] Nenhum item fora do escopo implementado
- [x] Documentação afetada atualizada
- [x] Estado da spec alterado para `Concluída` somente após a validação

## 15. Critério de parada

Parar quando criação, consulta por identificador, listagem, inclusão de produto, alteração de quantidade e remoção de item estiverem coordenadas por `OrderApplicationService`, cobertas por testes e desacopladas de qualquer implementação de persistência.

Não iniciar implementações em memória ou JSON, casos de uso de produto, novos estados de pedido, descontos, pagamentos, relatórios ou CLI. Essas capacidades devem ser definidas e autorizadas por specifications futuras.
