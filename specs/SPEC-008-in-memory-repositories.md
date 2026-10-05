# SPEC-008 — Repositories em Memória

## 1. Metadados

- **Estado:** Concluída
- **Dependências:** SPEC-003, SPEC-004 e SPEC-005 concluídas
- **Entrega:** Implementações em memória dos repositories de clientes, produtos e pedidos, compatíveis com os contratos da camada `application`

## 2. Objetivo

Implementar a primeira forma concreta de armazenamento do projeto, limitada à memória do processo.

Esta spec deve fornecer implementações de `CustomerRepository`, `ProductRepository` e `OrderRepository` que permitam armazenar, consultar e listar entidades durante uma execução. As implementações devem satisfazer os contratos já definidos pela camada `application`, sem alterar domínio ou casos de uso e sem introduzir arquivos, JSON ou interface de terminal.

O resultado esperado é uma infraestrutura simples e testada que possa ser injetada manualmente nos application services em specifications futuras.

## 3. Contexto

As SPECS 003, 004 e 005 introduziram interfaces de repository orientadas pelas necessidades dos casos de uso:

```text
CustomerRepository
ProductRepository
OrderRepository
```

Até esta etapa, os testes da camada `application` utilizam doubles para esses contratos. Ainda não existe uma implementação concreta que mantenha os dados produzidos pelos casos de uso.

O fluxo desta etapa é:

```text
Application Service
       │
       ▼
Repository interface
       ▲
       │ implements
In-memory repository
       │
       ▼
Map<Id, Entity>
```

As interfaces permanecem em `application`; as implementações pertencem a `infrastructure.persistence.memory`. Essa direção permite que a infraestrutura dependa das abstrações exigidas pela aplicação, sem fazer a aplicação conhecer a implementação concreta.

O armazenamento em memória é transitório: seu conteúdo existe somente na instância do repository e é perdido quando o processo termina.

## 4. Pré-condições

- As SPECS 003, 004 e 005 devem estar concluídas.
- `CustomerRepository`, `ProductRepository` e `OrderRepository` devem estar disponíveis com seus contratos atuais.
- `Customer`, `Product`, `Order` e seus identificadores tipados devem estar implementados e testados.
- O projeto deve compilar e executar os testes com Java 21 pelo Maven Wrapper.
- Nenhuma dependência adicional deve ser necessária.

## 5. Escopo

Esta spec autoriza:

- implementar um repository em memória específico para clientes;
- implementar um repository em memória específico para produtos;
- implementar um repository em memória específico para pedidos;
- armazenar cada entidade associada ao seu identificador tipado;
- inserir uma entidade ainda não armazenada;
- substituir a referência armazenada quando outra entidade com o mesmo identificador for salva;
- consultar uma entidade por seu identificador;
- listar as entidades na ordem de primeira inserção de seus identificadores;
- proteger a estrutura interna usada para armazenamento;
- validar argumentos obrigatórios nas fronteiras das implementações;
- criar testes unitários das três implementações;
- criar testes de integração leves que confirmem a compatibilidade das implementações com os application services existentes, sem alterar esses services.

## 6. Fora do escopo

Não implementar nesta etapa:

- persistência em JSON, arquivos ou qualquer armazenamento durável;
- uso de Jackson;
- CLI, menus, comandos, leitura ou saída de terminal;
- ponto de entrada `main` ou composição completa da aplicação;
- alterações em interfaces de repository ou application services existentes;
- novas operações de consulta, exclusão, paginação, filtragem ou ordenação;
- unicidade de e-mail, nome ou qualquer regra de negócio adicional;
- cópias profundas, snapshots ou serialização de entidades;
- repository genérico, interface base, classe abstrata ou utilitário compartilhado de persistência;
- cache separado do armazenamento em memória;
- controle transacional, rollback ou unidade de trabalho;
- sincronização, acesso concorrente ou garantias de thread safety;
- compartilhamento global de estado, singleton ou campos `static` mutáveis;
- banco de dados, ORM ou qualquer tecnologia proibida pelo `AGENTS.md`.

## 7. Requisitos funcionais

### RF-01 — Armazenamento de entidade nova

Ao receber uma entidade válida ainda não armazenada, `save` deve associá-la ao seu identificador. Uma consulta posterior pelo mesmo identificador deve encontrar a entidade salva.

Cada instância de repository deve manter seu próprio estado. Dados salvos em uma instância não podem aparecer em outra instância recém-criada.

### RF-02 — Substituição por identificador

Ao receber uma entidade cujo identificador já esteja armazenado, `save` deve substituir a referência anteriormente associada àquele identificador, sem criar uma entrada duplicada.

A substituição não deve alterar a posição daquele identificador na ordem de listagem.

### RF-03 — Consulta por identificador

`findById` deve retornar `Optional` contendo a entidade associada ao identificador informado. Quando o identificador não estiver armazenado, deve retornar `Optional.empty()`.

Ausência é um resultado normal de consulta e não deve produzir exception.

### RF-04 — Listagem

`findAll` deve retornar todas as entidades armazenadas, sem duplicação de identificadores, na ordem em que cada identificador foi inserido pela primeira vez.

Um repository novo deve retornar uma lista vazia e não `null`.

### RF-05 — Proteção da estrutura interna

A lista retornada por `findAll` deve ser um snapshot estruturalmente não modificável. O chamador não pode adicionar ou remover elementos dessa lista, e operações posteriores de `save` não podem alterar uma lista obtida anteriormente.

Essa proteção se limita à estrutura da coleção. O repository armazena e retorna as próprias referências das entidades recebidas; não é necessário clonar entidades nem impedir alterações realizadas por seus comportamentos públicos.

### RF-06 — Uniformidade entre repositories

As regras de armazenamento, substituição, consulta, listagem e validação desta spec devem ser observadas igualmente por clientes, produtos e pedidos, respeitando os tipos específicos de cada contrato.

## 8. Requisitos técnicos

### RT-01 — Linguagem e packages

O código deve usar Java 21. As implementações devem permanecer no package:

```text
dev.manoelreis.ordermanagement.infrastructure.persistence.memory
```

Os testes devem acompanhar esse package na árvore de testes.

### RT-02 — Direção de dependências

`infrastructure.persistence.memory` pode depender:

- das interfaces de repository em `application.customer`, `application.product` e `application.order`;
- das entidades e identificadores correspondentes em `domain`;
- das APIs da Java Standard Library.

`domain` e `application` não podem depender das implementações em memória. Nenhuma alteração nesses packages é necessária para implementar esta spec.

### RT-03 — Implementações específicas

Devem ser criadas três classes concretas e finais:

```text
InMemoryCustomerRepository implements CustomerRepository
InMemoryProductRepository implements ProductRepository
InMemoryOrderRepository implements OrderRepository
```

Cada classe deve implementar diretamente sua interface específica. Não criar `GenericRepository`, `AbstractInMemoryRepository`, interface intermediária ou hierarquia compartilhada.

A pequena repetição entre as três implementações é preferível a uma abstração antecipada.

### RT-04 — Collections

Cada repository deve utilizar internamente um `Map` adequado para:

- associar identificador tipado à entidade;
- impedir duplicação de identificadores;
- oferecer lookup direto por identificador;
- preservar a ordem de primeira inserção exigida por `findAll`.

A implementação concreta da collection não deve ser exposta pelo contrato público. `findAll` deve criar um snapshot estruturalmente não modificável dos valores armazenados.

### RT-05 — Semântica de identidade e referência

A chave do armazenamento deve ser obtida pelo identificador público da entidade recebida em `save`.

Os repositories não devem reconstruir, serializar, clonar ou copiar profundamente entidades. `findById` e `findAll` devem devolver as referências armazenadas, embrulhadas ou agrupadas conforme os contratos existentes.

### RT-06 — Estado e ciclo de vida

O estado deve pertencer à instância concreta do repository. Não utilizar estado global nem persistir dados entre execuções.

Não é necessário oferecer operação pública de limpeza. Cada teste deve obter isolamento criando uma nova instância do repository.

### RT-07 — Dependências e simplicidade

Nenhuma dependência de produção deve ser adicionada. Jackson não deve ser utilizado nesta etapa.

Os testes devem usar JUnit Jupiter e, preferencialmente, objetos reais. Mockito não é necessário para testar estruturas em memória determinísticas e simples de construir.

## 9. Modelo e contratos

### 9.1 `InMemoryCustomerRepository`

Implementação concreta de `CustomerRepository` responsável por manter clientes em memória por `CustomerId`.

Deve cumprir:

```text
save(Customer customer)
findById(CustomerId id) -> Optional<Customer>
findAll() -> List<Customer>
```

### 9.2 `InMemoryProductRepository`

Implementação concreta de `ProductRepository` responsável por manter produtos em memória por `ProductId`.

Deve cumprir:

```text
save(Product product)
findById(ProductId id) -> Optional<Product>
findAll() -> List<Product>
```

### 9.3 `InMemoryOrderRepository`

Implementação concreta de `OrderRepository` responsável por manter pedidos em memória por `OrderId`.

Deve cumprir:

```text
save(Order order)
findById(OrderId id) -> Optional<Order>
findAll() -> List<Order>
```

### 9.4 Semântica de `save`

`save` representa um *upsert* simples por identificador:

- identificador ausente: inserir a associação;
- identificador existente: substituir o valor associado;
- nunca criar duas entradas para o mesmo identificador.

Essa semântica não representa transação, versionamento, merge de atributos nem detecção de concorrência.

### 9.5 Semântica de coleção

A ordem de `findAll` é a ordem da primeira inserção de cada identificador. Substituir uma entidade pelo mesmo identificador preserva sua posição. A inserção de um novo identificador acrescenta a entidade ao final.

Cada chamada a `findAll` deve produzir um novo snapshot da estrutura atual. As entidades contidas no snapshot não precisam ser copiadas.

## 10. Tratamento de erros

- entidade nula em `save` deve produzir `IllegalArgumentException` e não alterar o estado do repository;
- identificador nulo em `findById` deve produzir `IllegalArgumentException`;
- uma entidade válida possui identificador não nulo pelas invariantes do domínio; a infraestrutura não deve duplicar essas validações;
- identificador não encontrado deve retornar `Optional.empty()`, sem exception;
- `findAll` sobre repository vazio deve retornar lista vazia, sem exception;
- tentativa de modificar a lista retornada por `findAll` deve falhar conforme o contrato de collection não modificável da JDK;
- nenhuma exception específica de infraestrutura deve ser criada para esta implementação em memória;
- falhas não devem ser convertidas em `null`, valores sentinela ou mensagens textuais.

## 11. Arquivos afetados

A implementação futura desta spec poderá criar ou alterar somente:

```text
src/main/java/dev/manoelreis/ordermanagement/infrastructure/persistence/memory/
src/test/java/dev/manoelreis/ordermanagement/infrastructure/persistence/memory/
README.md
specs/SPEC-008-in-memory-repositories.md
```

A criação desta specification também autoriza atualizar o índice em `specs/README.md`.

Não são necessárias mudanças em `domain`, `application`, `cli`, `infrastructure.persistence.json` ou `pom.xml`.

## 12. Estratégia de testes

Criar testes equivalentes para cada implementação, usando entidades reais e uma nova instância de repository por teste. Cobrir, no mínimo:

- repository novo retorna `Optional.empty()` para identificador válido desconhecido;
- repository novo retorna lista vazia e não nula;
- entidade salva pode ser encontrada pelo mesmo identificador e pela mesma referência;
- múltiplas entidades são listadas na ordem de primeira inserção;
- salvar outra entidade com o mesmo identificador substitui a referência anterior sem aumentar a quantidade;
- substituir uma entidade preserva a posição original na listagem;
- listas obtidas antes de um novo `save` permanecem inalteradas;
- lista retornada por `findAll` rejeita adição e remoção;
- entidades presentes na lista são as referências armazenadas, sem exigência de cópia profunda;
- `save(null)` e `findById(null)` são rejeitados sem modificar o estado existente;
- duas instâncias do mesmo tipo de repository não compartilham dados.

Adicionar ao menos um teste de integração leve por contrato ou um conjunto representativo que injete as implementações reais nos application services correspondentes e confirme um fluxo de cadastro seguido de consulta/listagem. Esses testes não devem duplicar toda a suíte da camada `application` nem testar CLI.

Testar comportamentos observáveis dos contratos. Não acessar collections internas por reflexão nem acoplar testes ao nome da implementação concreta de `Map`.

## 13. Critérios de aceite

- **CA-01:** existem implementações concretas e específicas para `CustomerRepository`, `ProductRepository` e `OrderRepository` em `infrastructure.persistence.memory`.
- **CA-02:** `save` insere entidades novas e substitui entidades com o mesmo identificador sem duplicação.
- **CA-03:** `findById` retorna a entidade armazenada ou `Optional.empty()` conforme a presença do identificador.
- **CA-04:** `findAll` retorna snapshot estruturalmente não modificável, independente de alterações estruturais posteriores e em ordem de primeira inserção.
- **CA-05:** os repositories armazenam referências de entidades sem serialização, reconstrução ou cópia profunda.
- **CA-06:** argumentos nulos são rejeitados conforme esta spec e não corrompem o estado existente.
- **CA-07:** instâncias distintas não compartilham estado e nenhuma garantia de persistência entre execuções é introduzida.
- **CA-08:** nenhum repository genérico, classe base, estado global, Jackson, File I/O ou alteração nas interfaces existentes é introduzido.
- **CA-09:** as implementações podem ser injetadas manualmente nos application services existentes.
- **CA-10:** testes relevantes e `./mvnw clean package` passam quando a spec for implementada.

## 14. Definition of Done

- [x] Requisitos funcionais atendidos
- [x] Requisitos técnicos atendidos
- [x] `InMemoryCustomerRepository` implementado e testado
- [x] `InMemoryProductRepository` implementado e testado
- [x] `InMemoryOrderRepository` implementado e testado
- [x] Compatibilidade com os application services verificada
- [x] Testes relevantes implementados e aprovados
- [x] Build aprovado com `./mvnw clean package`
- [x] Nenhum item fora do escopo implementado
- [x] Documentação afetada atualizada
- [x] Estado da spec alterado para `Concluída` somente após a validação

Validação realizada com Java 21: `./mvnw test` e `./mvnw clean package` aprovados, com 117 testes sem falhas, erros ou testes ignorados. A cobertura desta etapa inclui 27 testes unitários dos repositories e três testes de integração com os application services.

## 15. Critério de parada

Parar quando clientes, produtos e pedidos puderem ser armazenados, consultados e listados por implementações em memória dos repositories existentes, com contratos e isolamento cobertos por testes e build limpo.

Não iniciar CLI, composição completa da aplicação, persistência JSON, File I/O, novos casos de uso, novas consultas ou abstrações genéricas. Essas evoluções exigem specifications próprias.
