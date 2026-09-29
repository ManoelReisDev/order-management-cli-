# SPEC-005 — Casos de Uso de Product

## 1. Metadados

- **Estado:** Concluída
- **Dependências:** SPEC-002 concluída; SPEC-003 e SPEC-004 concluídas como contexto da camada `application`
- **Entrega:** Casos de uso de cadastro, consulta, listagem e alteração de produtos, coordenados pela camada `application` por meio de uma abstração de repository

## 2. Objetivo

Introduzir os casos de uso da camada `application` para o contexto de produtos.

Esta spec deve permitir cadastrar um produto, consultá-lo por identificador, listar os produtos existentes, renomeá-lo, alterar sua descrição e alterar seu preço. A aplicação deve coordenar o modelo de domínio criado na SPEC-002 e uma abstração de armazenamento, sem conhecer terminal, arquivos, JSON ou uma implementação concreta de repository.

O resultado esperado é uma API de aplicação utilizável diretamente por código Java e coberta por testes unitários. Nenhuma interface de usuário ou implementação concreta de persistência deve ser criada nesta etapa.

## 3. Contexto

A SPEC-002 introduziu `Product` e `ProductId`, incluindo as regras de nome, descrição e preço. As SPEC-003 e SPEC-004 estabeleceram o padrão de application service e repository específicos para clientes e pedidos. `OrderApplicationService.addProductToOrder` recebe um `Product` já resolvido pelo chamador; esta spec não modifica esse contrato.

O fluxo desta etapa é:

```text
Caller
   │
   ▼
ProductApplicationService
   ├──► Product
   └──► ProductRepository
```

`ProductRepository` representa somente a necessidade de armazenamento observada pelos casos de uso. Sua implementação pertence a uma specification futura.

## 4. Pré-condições

- A SPEC-002 deve estar concluída; as SPEC-003 e SPEC-004 devem estar disponíveis como referência dos contratos da camada `application`.
- `Product`, `ProductId` e `InvalidProductException` devem estar implementados e testados.
- O projeto deve compilar e executar os testes com Java 21 pelo Maven Wrapper.
- Nenhuma dependência adicional deve ser necessária.

## 5. Escopo

Esta spec autoriza:

- criar uma abstração de repository específica para `Product`;
- criar um application service específico para os casos de uso de produto;
- cadastrar um produto com identificador novo;
- buscar um produto por identificador;
- listar os produtos fornecidos pelo repository;
- renomear um produto existente;
- alterar a descrição de um produto existente;
- alterar o preço de um produto existente;
- representar explicitamente a tentativa de operar sobre um produto inexistente;
- aplicar injeção de dependência manual por construtor;
- criar testes unitários da camada `application.product`.

## 6. Fora do escopo

Não implementar nesta etapa:

- implementações de repository em memória, JSON ou qualquer outro mecanismo;
- leitura ou escrita de arquivos;
- Jackson na camada `application`;
- menus, comandos, entrada ou saída de terminal;
- busca automática de produto ou mudanças nos casos de uso de pedidos e seus itens;
- exclusão, desativação ou controle de estoque de produtos;
- paginação, filtros, ordenação ou busca por nome, descrição ou faixa de preço;
- regra de unicidade global do nome do produto;
- descontos, pagamentos, relatórios ou recálculo de pedidos após mudanças em produtos;
- DTOs, mappers, controllers, presenters, commands ou results;
- repositories genéricos, services genéricos ou classes base;
- alterações no modelo de domínio criado pela SPEC-002;
- tratamento transacional ou concorrência;
- Spring, container de injeção de dependência ou qualquer tecnologia proibida pelo `AGENTS.md`.

## 7. Requisitos funcionais

### RF-01 — Cadastro de produto

A aplicação deve receber nome, descrição e preço (`BigDecimal`), gerar um novo `ProductId`, criar um `Product` e solicitar seu armazenamento ao repository.

O produto cadastrado deve ser retornado ao chamador com os dados normalizados pelo domínio.

### RF-02 — Consulta por identificador

A aplicação deve consultar um produto por `ProductId`.

Quando o repository encontrar o produto, a aplicação deve retorná-lo. Quando não encontrar, deve sinalizar `ProductNotFoundException`.

### RF-03 — Listagem de produtos

A aplicação deve retornar todos os produtos fornecidos pelo repository, preservando a ordem recebida.

Quando não houver produtos, deve retornar uma lista vazia. O chamador não deve conseguir adicionar ou remover elementos da lista retornada.

### RF-04 — Renomeação de produto

A aplicação deve localizar o produto pelo identificador, solicitar ao próprio objeto de domínio a alteração do nome e solicitar ao repository o armazenamento do estado alterado.

A operação deve retornar o produto alterado.

### RF-05 — Alteração de descrição

A aplicação deve localizar o produto pelo identificador, solicitar ao próprio objeto de domínio a alteração da descrição e solicitar ao repository o armazenamento do estado alterado.

A operação deve retornar o produto alterado. Uma descrição vazia é válida conforme o domínio; uma descrição nula não é.

### RF-06 — Alteração de preço

A aplicação deve localizar o produto pelo identificador, solicitar ao próprio objeto de domínio a alteração do preço e solicitar ao repository o armazenamento do estado alterado.

A operação deve retornar o produto alterado. O preço deve permanecer sob as invariantes de `Product`: positivo e representável com duas casas decimais sem arredondamento.

### RF-07 — Preservação das regras de domínio

Nome, descrição e preço não devem ser validados ou normalizados novamente pela camada `application`. As operações devem usar o construtor e os comportamentos públicos de `Product` e propagar `InvalidProductException` quando os dados violarem as invariantes definidas na SPEC-002.

Se a criação ou alteração for rejeitada pelo domínio, a aplicação não deve solicitar o armazenamento daquele estado inválido.

## 8. Requisitos técnicos

### RT-01 — Linguagem e package

O código deve usar Java 21 e permanecer no package:

```text
dev.manoelreis.ordermanagement.application.product
```

Os testes devem acompanhar esse package na árvore de testes.

### RT-02 — Direção de dependências

`application.product` pode depender de `domain.product`, `domain.exception` e das APIs da Java Standard Library.

O domínio não pode depender de `application`. A camada `application` não pode depender de `infrastructure`, `cli`, Jackson ou detalhes de persistência.

### RT-03 — Abstração de repository

`ProductRepository` deve ser uma interface específica para produtos e declarar somente as operações exigidas nesta spec:

```text
save(Product product)
findById(ProductId id) -> Optional<Product>
findAll() -> List<Product>
```

O contrato não deve revelar estruturas ou detalhes de uma futura implementação de persistência.

### RT-04 — Application service

`ProductApplicationService` deve ser uma classe concreta e final. Ela deve receber `ProductRepository` obrigatoriamente pelo construtor e não deve criar, localizar ou escolher implementações concretas dessa dependência.

O service deve oferecer operações com nomes que expressem os casos de uso:

```text
registerProduct(name, description, price) -> Product
findProductById(productId) -> Product
listProducts() -> List<Product>
renameProduct(productId, name) -> Product
changeProductDescription(productId, description) -> Product
changeProductPrice(productId, price) -> Product
```

O parâmetro `price` deve ser do tipo `BigDecimal`.

### RT-05 — Optional

`ProductRepository.findById` deve usar `Optional<Product>` para representar ausência legítima de resultado. O application service deve resolver essa ausência e não deve retornar `null` nem `Optional` nos casos de uso definidos nesta spec.

### RT-06 — Collections

`ProductRepository.findAll` deve retornar `List<Product>`. `ProductApplicationService.listProducts` deve fornecer uma cópia estrutural não modificável da lista recebida, preservando sua ordem e sem expor a collection do repository.

Esta proteção se refere à estrutura da lista; não exige copiar os produtos contidos nela.

### RT-07 — Dependências e simplicidade

Nenhuma dependência de produção deve ser adicionada. JUnit Jupiter deve ser usado nos testes, e Mockito pode ser usado somente para isolar `ProductRepository`.

Não criar DTOs ou abstrações adicionais enquanto os parâmetros e retornos do domínio forem suficientes para estes casos de uso.

## 9. Modelo e contratos

### 9.1 `ProductRepository`

Porta de saída utilizada pela aplicação para armazenar e recuperar produtos.

Responsabilidades:

- armazenar o produto informado por `save`;
- representar o resultado de `findById` com `Optional`;
- fornecer por `findAll` uma lista não nula, possivelmente vazia.

Esta interface não define nesta etapa:

- tecnologia de persistência;
- comportamento entre execuções do processo;
- ordenação própria;
- controle transacional;
- consultas além das estritamente necessárias.

### 9.2 `ProductApplicationService`

Coordenador dos casos de uso de produto.

Responsabilidades:

- gerar a identidade no cadastro;
- criar e modificar produtos por meio do modelo de domínio;
- usar `ProductRepository` para consulta e armazenamento;
- converter a ausência de um produto em erro explícito de aplicação;
- proteger a estrutura da lista devolvida ao chamador.

O service não deve conter regras de formato de nome, descrição ou preço, nem conhecer como os produtos são armazenados.

### 9.3 `ProductNotFoundException`

Exception não verificada da camada `application.product`, utilizada quando um caso de uso exige um produto existente e `ProductRepository.findById` não o encontra.

A exception deve identificar o `ProductId` procurado em sua mensagem e não deve ser adicionada à hierarquia de exceptions de domínio.

### 9.4 Colaboração com pedidos

`Order` preserva um snapshot do produto no momento em que ele é adicionado ao pedido, conforme a SPEC-002. Alterar o cadastro de um produto nesta spec não deve atualizar itens de pedidos existentes. Nenhuma colaboração com `OrderApplicationService` ou `OrderRepository` é necessária para os casos de uso aqui definidos.

## 10. Tratamento de erros

- repository ausente na construção do application service deve ser rejeitado imediatamente;
- `ProductId` nulo em operações de consulta ou alteração deve ser rejeitado como argumento inválido antes de consultar o repository;
- produto ausente deve produzir `ProductNotFoundException` e impedir chamada a `save`;
- nome inválido, descrição nula, preço nulo, não positivo ou com mais de duas casas decimais não representáveis sem arredondamento devem continuar produzindo `InvalidProductException`, conforme o domínio;
- uma falha deve interromper o caso de uso e impedir uma chamada posterior a `save`;
- exceptions técnicas do repository não devem ser ocultadas, traduzidas ou envolvidas nesta etapa;
- não retornar `null`, valores sentinela ou mensagens textuais para representar falhas.

## 11. Arquivos afetados

A implementação futura desta spec poderá criar ou alterar somente:

```text
src/main/java/dev/manoelreis/ordermanagement/application/product/
src/test/java/dev/manoelreis/ordermanagement/application/product/
README.md
specs/SPEC-005-product-application.md
```

A criação desta specification também autoriza atualizar o índice em `specs/README.md`.

Não alterar `domain`, `infrastructure`, `cli`, `pom.xml`, `application.customer`, `application.order` ou outros contextos de `application` para implementar esta spec.

## 12. Estratégia de testes

Usar JUnit Jupiter e objetos reais de `Product`. Usar doubles manuais ou Mockito somente para isolar o contrato `ProductRepository`.

Cobrir, no mínimo:

- rejeição de repository nulo na construção do service;
- cadastro de produto com novo identificador, dados normalizados pelo domínio e chamada a `save`;
- cadastro com descrição vazia válida;
- propagação de `InvalidProductException` no cadastro com nome, descrição ou preço inválido, sem chamada a `save`;
- consulta bem-sucedida por identificador;
- `ProductNotFoundException` na consulta de identificador inexistente, com o identificador na mensagem;
- listagem vazia;
- listagem que preserva a ordem fornecida pelo repository;
- impossibilidade de adicionar ou remover elementos da lista retornada e ausência de exposição da lista do repository;
- renomeação, alteração de descrição e alteração de preço bem-sucedidas, cada uma seguida de `save`;
- falha das alterações quando o produto não existe, sem chamada a `save`;
- propagação de `InvalidProductException` em alterações inválidas, sem chamada a `save`;
- rejeição de `ProductId` nulo antes de qualquer consulta ao repository.

Os testes devem observar resultados e interações relevantes, sem testar detalhes privados, reproduzir a implementação ou depender de uma futura classe de infraestrutura. As invariantes detalhadas de preço, nome e descrição já são testadas no domínio e não precisam ser duplicadas integralmente aqui.

## 13. Critérios de aceite

- **CA-01:** um produto válido pode ser cadastrado com novo `ProductId` e enviado ao repository.
- **CA-02:** produtos podem ser consultados por identificador sem expor `Optional` ao chamador do application service.
- **CA-03:** a ausência de produto produz `ProductNotFoundException` com o identificador procurado.
- **CA-04:** produtos podem ser listados na ordem fornecida pelo repository por meio de uma lista estruturalmente não modificável.
- **CA-05:** nome, descrição e preço de um produto existente podem ser alterados usando os comportamentos do domínio, com o estado resultante enviado ao repository.
- **CA-06:** dados inválidos continuam sendo rejeitados pelo domínio e não são enviados ao repository.
- **CA-07:** a dependência de `ProductRepository` é recebida manualmente pelo construtor.
- **CA-08:** `application.product` não depende de infraestrutura, CLI ou Jackson.
- **CA-09:** nenhuma implementação concreta de repository ou alteração nos casos de uso de pedidos é criada.
- **CA-10:** todos os testes e o build Maven passam.

## 14. Definition of Done

- [x] Requisitos funcionais atendidos
- [x] Requisitos técnicos atendidos
- [x] `ProductRepository` implementado como abstração específica
- [x] `ProductApplicationService` implementado e testado
- [x] `ProductNotFoundException` implementada e testada pelos casos de uso
- [x] Testes relevantes implementados e aprovados
- [x] Build aprovado com `./mvnw clean package`
- [x] Nenhum item fora do escopo implementado
- [x] Documentação afetada atualizada
- [x] Estado da spec alterado para `Concluída` somente após a validação

## 15. Critério de parada

Parar quando cadastro, consulta por identificador, listagem, renomeação, alteração de descrição e alteração de preço estiverem coordenados por `ProductApplicationService`, cobertos por testes e desacoplados de qualquer implementação de persistência.

Não iniciar implementações em memória ou JSON, CLI, exclusão ou estoque de produtos, nem alterações nos casos de uso de pedidos. Essas capacidades devem ser definidas e autorizadas por specifications futuras.
