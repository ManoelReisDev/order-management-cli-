# SPEC-010 — Pedidos na CLI

## 1. Metadados

- **Estado:** Concluída
- **Dependências:** SPEC-004, SPEC-005, SPEC-008 e SPEC-009 concluídas
- **Entrega:** Criação, consulta, listagem e manutenção de itens de pedidos pelo terminal, reutilizando os casos de uso existentes e armazenamento em memória

## 2. Objetivo

Disponibilizar o contexto de pedidos na interface de terminal. O usuário deve conseguir criar um pedido para um cliente cadastrado, incluir produtos cadastrados, alterar quantidades, remover itens e consultar o estado e o subtotal resultantes.

A CLI deve coordenar entrada e apresentação por meio dos application services. As regras de pedidos continuam no domínio, sem duplicação no terminal.

## 3. Contexto

`Order`, `OrderId`, `OrderItem` e `OrderStatus` já estão implementados. `OrderApplicationService` oferece criação, consulta, listagem e manutenção de itens, e `InMemoryOrderRepository` fornece armazenamento durante a execução.

A SPEC-009 introduziu `Main`, `TerminalSession`, `TerminalInput` e `TerminalOutput`, expondo somente clientes e produtos. Esta etapa amplia essa mesma sessão para pedidos.

O contrato atual de `addProductToOrder` recebe um `Product`, não um `ProductId`. A CLI deve obter esse produto pelo `ProductApplicationService` antes de chamar o service de pedidos. Não é necessário alterar os contratos existentes.

O domínio permite editar apenas pedidos em `DRAFT`; pedidos em `PAID` rejeitam alterações. Pagamentos não serão expostos nesta etapa, mas essa restrição existente deve ser respeitada e testada.

## 4. Pré-condições

- Dependências desta spec concluídas e validadas.
- Services, entidades, identificadores tipados e repositories em memória disponíveis com seus contratos atuais.
- CLI de clientes e produtos funcional, com entrada e saída injetáveis.
- Projeto compilando e executando testes com Java 21 e Maven Wrapper.
- Nenhuma dependência adicional necessária.

## 5. Escopo

- Ampliar a composição manual em `Main` para incluir pedidos e um `Clock`.
- Reutilizar o mesmo repository de clientes nos services de clientes e pedidos.
- Adicionar opções de pedidos ao menu existente.
- Criar pedido para cliente existente, buscar por ID e listar pedidos.
- Adicionar produto, substituir quantidade e remover item de pedido.
- Converter identificadores e quantidades recebidos pelo terminal.
- Apresentar dados do pedido, itens e subtotais devolvidos pelo domínio.
- Tratar erros esperados e EOF nos novos fluxos.
- Ampliar testes da CLI e documentação de execução.

## 6. Fora do escopo

- Alterações no domínio, application services ou repositories existentes.
- Pagamentos, descontos, cancelamento ou novos estados de pedido.
- Total com descontos, frete, estoque ou relatórios pelo terminal.
- Exclusão de pedidos, busca por cliente, filtros, ordenação ou paginação.
- Alteração ou exclusão de clientes e produtos.
- Persistência JSON, arquivos, banco de dados ou dados de exemplo carregados na inicialização.
- Novas dependências, frameworks, DTOs, mappers ou abstrações genéricas de comandos e CRUD.
- Submenus, histórico, autocomplete, cores ANSI ou interface de tela inteira.
- Alterações de empacotamento ou do `pom.xml`.

## 7. Requisitos funcionais

### RF-01 — Menu de pedidos

Preservar as opções `0` a `6` e acrescentar ao menu principal um grupo de pedidos com:

```text
[7] Criar pedido
[8] Listar pedidos
[9] Buscar pedido por ID
[10] Adicionar produto ao pedido
[11] Alterar quantidade de item
[12] Remover item do pedido
```

Após sucesso ou erro recuperável, apresentar o menu novamente. Atualizar a mensagem de opção inválida para os códigos disponíveis. Usar o loop existente, sem menus recursivos.

### RF-02 — Criação de pedido

Solicitar o UUID completo do cliente, convertê-lo em `CustomerId` e chamar `createOrder`. O service verifica a existência do cliente e gera identidade e instante de criação.

Apresentar confirmação e os dados do pedido retornado: ID, ID do cliente, instante, status `DRAFT`, ausência de itens e subtotal `0.00`. Cliente inexistente deve produzir mensagem correspondente e não gerar pedido.

### RF-03 — Consulta e listagem

A consulta solicita UUID completo do pedido, converte em `OrderId` e chama `findOrderById`. A listagem chama `listOrders`, preservando a ordem recebida e apresentando mensagem explícita quando vazia.

Cada pedido deve ser apresentado com ID, ID do cliente, instante de criação, status e subtotal. Apresentar também seus itens, na ordem fornecida por `getItems()`, com ID e nome do produto, preço unitário, quantidade e subtotal do item. Pedido vazio deve indicar ausência de itens.

Usar os dados do snapshot de `OrderItem`, sem consultar produtos para reconstruir nomes ou preços históricos. Não exigir consultas adicionais de clientes para apresentar o pedido.

### RF-04 — Inclusão de produto

Solicitar, nesta ordem, ID do pedido, ID do produto e quantidade. Após receber e converter os campos, obter o produto por `ProductApplicationService.findProductById` e chamar `OrderApplicationService.addProductToOrder` com o produto retornado.

Apresentar confirmação e o pedido atualizado. Inclusões repetidas do mesmo produto devem refletir o acúmulo de quantidade e a preservação do snapshot definidos pelo domínio, mantendo um único item por produto.

Produto ou pedido inexistente, quantidade inválida e pedido pago devem impedir a alteração e produzir mensagem compreensível. Se produto e pedido estiverem ausentes, a consulta de produto ocorre primeiro; não exigir uma consulta prévia de pedido apenas para priorizar o erro.

### RF-05 — Alteração de quantidade

Solicitar, nesta ordem, ID do pedido, ID do produto que identifica o item e nova quantidade. Chamar `changeOrderItemQuantity`, apresentar confirmação e o pedido atualizado.

A quantidade informada substitui a anterior; não representa um incremento. Não consultar o cadastro de produtos para esta operação: o item é localizado pelo domínio dentro do pedido.

Quantidade zero não remove item. A rejeição de zero ou de valores negativos pertence ao domínio.

### RF-06 — Remoção de item

Solicitar ID do pedido e ID do produto que identifica o item, nessa ordem, e chamar `removeOrderItem`. Apresentar confirmação e o pedido atualizado, inclusive quando a remoção deixar o pedido vazio.

Não consultar o cadastro de produtos para remover um item. Item inexistente deve ser apresentado como erro, preservando o pedido e a sessão.

### RF-07 — Formatos de entrada

Reutilizar a conversão de UUID completo da SPEC-009, aceitando letras hexadecimais maiúsculas ou minúsculas e ignorando espaços externos.

Quantidade aceita sinal opcional e dígitos decimais, sem espaços internos, casas decimais, notação científica ou separadores. Ignorar espaços externos e converter para `int`. Valores fora do intervalo de `int` são erros de formato/representação; zero e negativos representáveis devem chegar ao domínio para validação da regra de negócio.

Falha de conversão encerra a operação atual e retorna ao menu, sem mutação. Não implementar novas tentativas por campo.

### RF-08 — EOF e estado da sessão

EOF em qualquer etapa deve encerrar normalmente. Nenhuma criação ou mutação deve ser chamada com campos incompletos. Distinguir EOF de linha vazia, que permanece uma entrada presente e pode ter formato inválido.

Os mesmos repositories e services devem atender toda a sessão. Pedidos criados e alterados devem permanecer disponíveis para consulta e listagem até o encerramento. Uma nova execução começa sem dados.

## 8. Requisitos técnicos

### RT-01 — Composição manual

`Main` deve criar uma única instância de cada repository de clientes, produtos e pedidos. Injetar o mesmo `InMemoryCustomerRepository` em `CustomerApplicationService` e `OrderApplicationService`, para que clientes cadastrados pelo terminal possam receber pedidos.

Construir `OrderApplicationService` com o repository de pedidos, o de clientes e `Clock.systemUTC()`. Os testes devem usar `Clock.fixed`. A CLI não solicita instante de criação nem gera identificadores.

### RT-02 — Contratos e dependências

`TerminalSession` deve receber também `OrderApplicationService` por construtor, rejeitando dependência nula com `IllegalArgumentException`. Atualizar os chamadores e testes existentes; não é necessário manter um construtor alternativo sem pedidos.

Os fluxos devem chamar services, sem acesso direto a repositories ou mutação direta de entidades. A CLI pode utilizar entidades e identificadores para entrada e apresentação. Não importar CLI em `domain` ou `application`.

### RT-03 — Entrada e saída

Reutilizar `TerminalInput` e `TerminalOutput`, mantendo leitura por linha, uma única fonte de entrada, descarga de prompts e propriedade dos recursos pelo chamador. Não fechar `System.in` ou `System.out`.

Valores monetários devem ser apresentados a partir de `BigDecimal`, com duas casas decimais e ponto, sem conversão para `float` ou `double`, sem moeda presumida e sem recálculo de subtotais na CLI. Apresentar o instante em ISO-8601, usando a representação do `Instant` existente.

### RT-04 — Simplicidade e linguagem

Usar Java 21, Java Standard Library e as dependências já disponíveis para testes. Código e testes em inglês; mensagens do terminal em português. Manter despacho explícito das opções e divisão de responsabilidades existente, sem criar interfaces apenas para mocks.

### RT-05 — Execução

Preservar o comando existente após `./mvnw clean package`:

```bash
java -cp target/order-management-cli-0.1.0-SNAPSHOT.jar dev.manoelreis.ordermanagement.cli.Main
```

## 9. Modelo e contratos

Contratos públicos utilizados, sem alterações:

```text
OrderApplicationService
├── createOrder(customerId)
├── findOrderById(orderId)
├── listOrders()
├── addProductToOrder(orderId, product, quantity)
├── changeOrderItemQuantity(orderId, productId, quantity)
└── removeOrderItem(orderId, productId)

ProductApplicationService
└── findProductById(productId)
```

`Main` compõe as dependências; `TerminalSession` coordena os fluxos; `TerminalInput` converte formatos; `TerminalOutput` apresenta pedidos e itens.

`Order` permanece responsável por estado permitido, quantidades, acúmulo, snapshot e subtotal. A CLI não mantém uma coleção paralela de pedidos nem oferece setters para seus dados.

## 10. Tratamento de erros

- UUID e quantidade malformados ou fora do intervalo de `int`: informar formato inválido e retornar ao menu.
- `CustomerNotFoundException`, `ProductNotFoundException` e `OrderNotFoundException`: informar qual registro não foi encontrado e seu identificador.
- `InvalidQuantityException`: informar quantidade rejeitada, inclusive estouro na soma de quantidades.
- `OrderItemNotFoundException`: informar que o produto não identifica um item daquele pedido.
- `DomainException` ao editar pedido pago: apresentar mensagem em português de operação rejeitada pelo estado do pedido. Não reproduzir a validação de status antes da chamada ao service.
- Tratar exceptions específicas antes de `DomainException`, sua classe base. Capturar erros esperados nos fluxos correspondentes, sem capturar indiscriminadamente `Exception` ou `RuntimeException`.
- Erros esperados não devem apresentar stack traces nem encerrar a sessão; operações rejeitadas devem preservar o estado anterior.
- EOF encerra normalmente. Falhas reais de leitura/escrita continuam propagando `IOException` até o ponto de entrada, sem serem confundidas com EOF ou erro de dados.
- Não criar novas exceptions para atender a esta spec.

## 11. Arquivos afetados

A implementação futura poderá alterar:

```text
src/main/java/dev/manoelreis/ordermanagement/cli/Main.java
src/main/java/dev/manoelreis/ordermanagement/cli/menu/TerminalSession.java
src/main/java/dev/manoelreis/ordermanagement/cli/input/TerminalInput.java
src/main/java/dev/manoelreis/ordermanagement/cli/output/TerminalOutput.java
src/test/java/dev/manoelreis/ordermanagement/cli/
README.md
specs/SPEC-010-order-cli.md
specs/README.md
```

A criação desta specification autoriza somente este documento e a atualização do índice. Não autoriza implementar os fluxos nesta tarefa de documentação.

Não alterar `domain`, `application`, `infrastructure`, `shared`, `pom.xml` ou Maven Wrapper.

## 12. Estratégia de testes

Usar JUnit Jupiter, services e repositories reais, `Clock.fixed`, entrada em memória e saída capturada. Cada teste deve compor dependências novas, compartilhando o repository de clientes entre os services apropriados.

Cobrir, no mínimo:

- Preservação dos fluxos da SPEC-009 e exibição das opções `7` a `12`.
- Cadastro de cliente pelo terminal seguido de criação de pedido na mesma sessão; usar entrada controlada que aproveite o ID gerado, sem trocar globalmente `System.in` ou criar geradores de ID em produção.
- Criação para cliente previamente cadastrado, com ID gerado, instante fixo, `DRAFT`, itens vazios e subtotal `0.00`.
- Cliente inexistente sem criação parcial e com continuidade da sessão.
- Consulta por ID existente e ausente; listagem vazia e com múltiplos pedidos na ordem do service.
- Inclusão, inclusão repetida com acúmulo, substituição de quantidade e remoção até pedido vazio; verificar estado armazenado e subtotais apresentados.
- Produto e pedido inexistentes, item ausente e tentativas de alteração em pedido pago, sem mudança de estado e com retorno ao menu.
- Snapshot preservado: apresentar nome e preço do item mesmo se o cadastro do produto for substituído na preparação do teste.
- Quantidades válidas com espaços externos e sinal; limites de `int`, texto vazio, letras, decimal, vírgula, notação científica e estouro de representação.
- Quantidade zero e negativa rejeitadas pelo domínio; soma acima de `Integer.MAX_VALUE` rejeitada sem alterar item.
- UUIDs inválidos nos novos campos e UUIDs completos com maiúsculas e espaços externos.
- EOF em cada campo de criação, consulta, inclusão, alteração e remoção, sem mutação parcial, mensagem de formato inválido ou novas leituras.
- Rejeição de service de pedidos nulo, isolamento entre sessões com repositories distintos e preservação dos testes de recursos e I/O existentes.

Pedidos pagos podem ser preparados usando `order.pay` com uma implementação simples de `PaymentMethod` que retorne `PaymentResult.APPROVED`, sem adicionar fluxo de pagamento ao terminal.

Para operações com IDs gerados, usar registros preparados pelos services ou entrada controlada. Não fixar UUIDs aleatórios, testar métodos privados ou depender do alinhamento literal do menu. Verificar dados, mutações e continuidade.

Na implementação, executar `./mvnw test` e `./mvnw clean package`. Realizar smoke test do comando documentado com entrada controlada, cobrindo cadastro de cliente/produto, criação de pedido, inclusão, consulta e saída.

## 13. Critérios de aceite

- **CA-01:** o menu mantém as operações existentes e oferece as seis operações de pedidos com os códigos definidos.
- **CA-02:** cliente cadastrado na sessão pode receber um pedido em `DRAFT`, vazio e com instante fornecido pelo `Clock`.
- **CA-03:** pedidos podem ser consultados e listados com dados, itens e subtotais, preservando a ordem recebida.
- **CA-04:** produtos cadastrados podem ser incluídos, quantidades substituídas e itens removidos pelos services existentes.
- **CA-05:** inclusão repetida acumula quantidade sem duplicar item, preservando o snapshot do domínio.
- **CA-06:** entradas inválidas, registros ausentes, quantidades rejeitadas e pedidos pagos produzem mensagens compreensíveis e preservam estado e sessão.
- **CA-07:** EOF encerra em qualquer etapa sem criação ou mutação parcial.
- **CA-08:** composição compartilha o repository de clientes, mantém os dados durante a sessão e usa injeção manual.
- **CA-09:** regras de negócio e contratos das camadas centrais permanecem preservados, sem dependências adicionais.
- **CA-10:** testes, build e smoke test passam; README documenta os novos fluxos e armazenamento transitório.

## 14. Definition of Done

- [x] Requisitos funcionais atendidos
- [x] Requisitos técnicos atendidos
- [x] Composição manual ampliada e compartilhamento do repository de clientes verificado
- [x] Fluxos de pedidos implementados e testados
- [x] Erros esperados e EOF verificados nos novos fluxos
- [x] Testes relevantes implementados e aprovados
- [x] Build aprovado com `./mvnw clean package`
- [x] Smoke test do comando de execução aprovado
- [x] Nenhum item fora do escopo implementado
- [x] Documentação afetada atualizada
- [x] Estado alterado para `Concluída` somente após a validação da implementação

## 15. Critério de parada

Parar quando a CLI permitir criar, consultar e listar pedidos, adicionar produtos, substituir quantidades e remover itens em memória, com erros recuperáveis, EOF seguro, testes e build aprovados.

Não iniciar pagamentos pelo terminal, cancelamento, descontos, relatórios, persistência JSON ou mudanças nos contratos centrais. Essas evoluções exigem specifications próprias.
