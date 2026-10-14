# SPEC-012 — Persistência em JSON

## 1. Metadados

- **Estado:** Concluída
- **Dependências:** SPEC-002 a SPEC-006, SPEC-008 e SPEC-011 concluídas
- **Entrega:** Clientes, produtos e pedidos persistidos em arquivos JSON e disponíveis em novas execuções da CLI, preservando itens e status de pagamento

## 2. Objetivo

Implementar armazenamento durável para os três contratos de repository existentes, usando Jackson para JSON e a JDK para operações de arquivos.

O usuário deve poder cadastrar clientes e produtos, criar e editar pedidos, aprovar um pagamento, encerrar a CLI e consultar esses mesmos dados em outra execução. Cada `save` bem-sucedido deve concluir a gravação; a durabilidade não pode depender de uma operação de exportação ou do encerramento normal da sessão.

O incremento deve exercitar File I/O, tratamento de exceptions, mapeamento explícito de dados, reconstrução de objetos e substituição de implementações por injeção manual.

## 3. Contexto

A SPEC-008 implementou repositories em memória. A CLI atual os compõe em `Main`, e os application services já chamam `save` nas operações de cadastro, criação e edição de pedidos e na aprovação de pagamento. Uma recusa não exige gravação, pois não altera o pedido.

As interfaces estão em `application` e oferecem `save`, `findById` e `findAll`. Essa fronteira permite introduzir persistência sem alterar os contratos ou os casos de uso.

`Order` possui estado encapsulado: o construtor cria um rascunho e `pay` realiza a transição para `PAID`. `OrderItem` guarda um snapshot do nome e preço do produto. A restauração deve recuperar esse estado histórico sem processar um novo pagamento nem buscar os valores atuais no catálogo.

Jackson Databind e o módulo Java Time já estão declarados no `pom.xml`. O comando atual da CLI inclui somente o JAR do projeto no classpath; ele deverá incluir as dependências de runtime quando Jackson passar a ser usado.

## 4. Pré-condições

- As specs indicadas nas dependências devem estar concluídas.
- Os contratos atuais de repository, entidades e identificadores tipados devem estar disponíveis e testados.
- Os services devem continuar salvando alterações pelas interfaces existentes.
- A CLI deve compartilhar o mesmo repository de pedidos entre os services de pedidos e pagamentos.
- O projeto deve compilar e executar os testes com Java 21 pelo Maven Wrapper.

## 5. Escopo

Esta spec autoriza:

- implementar `JsonCustomerRepository`, `JsonProductRepository` e `JsonOrderRepository`;
- definir representações JSON específicas e mapeamento explícito na infraestrutura;
- ler e gravar arquivos UTF-8 usando `Path` e `Files`;
- restaurar clientes, produtos, pedidos e snapshots dos itens;
- adicionar operações de reconstrução validadas em `Order` e `OrderItem`;
- preservar ordem de inserção, substituição por ID e snapshots de listagem;
- gravar por arquivo temporário e substituição atômica;
- criar uma exception técnica específica de persistência;
- substituir a composição padrão de `Main` pelos repositories JSON;
- ajustar a mensagem inicial da sessão e tratar falhas de persistência no ponto de entrada;
- ajustar o empacotamento e documentar um comando de execução com dependências;
- criar testes com arquivos temporários, incluindo reinício da composição e integração com a CLI;
- atualizar a documentação afetada ao concluir a implementação.

## 6. Fora do escopo

Não implementar nesta etapa:

- banco de dados, ORM, Spring, containers de DI ou tecnologias proibidas pelo `AGENTS.md`;
- novos casos de uso, opções de menu, exclusão de entidades ou novas consultas;
- descontos, cancelamento, novos status ou histórico de pagamentos;
- importação/exportação pelo menu ou escolha de backend pelo usuário;
- migração de formatos anteriores, schema registry ou versionamento de documentos;
- backup automático, recuperação automática de arquivos corrompidos ou retentativas;
- acesso simultâneo ao mesmo arquivo por múltiplas instâncias ou processos;
- locks, sincronização, cache de entidades, estado global ou monitoramento de arquivos;
- transações entre arquivos, unidade de trabalho, journal ou event sourcing;
- garantia de sobrevivência a falha de energia por `fsync`;
- repository genérico, classe base de repository, mapper genérico ou framework de persistência;
- alterações nos contratos e services de `application` ou nas implementações em memória.

## 7. Requisitos funcionais

### RF-01 — Armazenamento por entidade

Cada repository deve receber por construtor o `Path` do seu arquivo. A composição padrão da CLI deve usar caminhos relativos ao diretório de trabalho:

```text
data/customers.json
data/products.json
data/orders.json
```

Os testes devem fornecer caminhos isolados. Os repositories não devem conhecer um diretório global nem acessar os arquivos de outro repository.

### RF-02 — Arquivo ausente e coleção vazia

Arquivo inexistente deve representar armazenamento vazio: `findAll` retorna lista vazia e `findById` retorna `Optional.empty()` para um ID válido. A leitura não deve criar arquivos ou diretórios.

No primeiro `save`, criar os diretórios pais necessários e o arquivo correspondente. Um arquivo contendo `[]` também representa armazenamento vazio. Arquivo vazio, com apenas espaços ou contendo `null` não equivale a arquivo inexistente e deve ser rejeitado.

### RF-03 — Gravação e substituição

`save` deve ler e validar a coleção existente, inserir ou substituir pelo identificador e gravar a coleção resultante inteira. Uma substituição preserva a posição do ID; um ID novo é acrescentado ao final.

Quando `save` retorna normalmente, uma nova instância apontando para o mesmo arquivo deve recuperar o estado salvo. Não deve haver `flush`, salvamento no encerramento ou dependência de referências mantidas em memória.

### RF-04 — Consultas e isolamento

Cada operação deve ler o arquivo atual. `findById` retorna a entidade encontrada ou `Optional.empty()`; `findAll` retorna um snapshot estruturalmente não modificável, em ordem de primeira inserção.

As consultas devem reconstruir os objetos. Não há garantia de identidade de referência entre consultas, nem em relação ao objeto recebido por `save`. Modificar um objeto consultado ou previamente salvo não deve alterar o arquivo antes de outro `save` bem-sucedido. Objetos e listas já retornados não devem mudar em consequência de operações posteriores do repository.

Instâncias distintas podem acessar sequencialmente o mesmo arquivo e observar as gravações anteriores. Não há suporte a operações simultâneas; caminhos diferentes devem permanecer isolados.

### RF-05 — Preservação de dados

O round-trip deve preservar:

- IDs e dados de clientes e produtos;
- preço do produto como valor monetário exato;
- ID do pedido, `customerId`, `createdAt` e `status`;
- ordem dos itens, `productId`, nome histórico, preço unitário e quantidade;
- subtotal calculado e restrições comportamentais do pedido restaurado.

Um pedido `DRAFT` pode estar vazio e continuar editável. Um pedido `PAID` deve possuir itens e continuar rejeitando edição e novo pagamento. Restaurar um pedido pago não pode invocar `PaymentMethod`, `Order.pay` ou registrar outro pagamento.

Os snapshots de itens não podem ser reconstruídos a partir do catálogo atual. Alterações posteriores no nome ou preço do produto não devem modificar pedidos previamente salvos. O repository de pedidos não deve depender dos repositories de clientes ou produtos; validação de existência entre arquivos fica fora desta etapa.

### RF-06 — Conteúdo inválido

Todo documento deve ser validado antes de produzir um resultado ou substituir um arquivo. Não aceitar parcialmente a coleção, ignorar registros inválidos nem sobrescrever dados corrompidos como se a coleção estivesse vazia.

Rejeitar JSON malformado, raiz diferente de array, registros nulos, campos obrigatórios ausentes ou nulos, tipos incompatíveis, campos desconhecidos, propriedades JSON repetidas, conteúdo extra após o documento, IDs inválidos ou repetidos, enum desconhecido e valores que violem as invariantes do domínio.

Quantidades devem ser números JSON inteiros, positivos e dentro do limite de `int`; não aceitar coerção de texto, booleanos ou números fracionários. IDs devem ter formato UUID completo. Valores monetários e instantes seguem os formatos definidos na seção 9.

Mesmo `findById` deve rejeitar um documento que contenha outro registro inválido, em vez de retornar antecipadamente um resultado parcial.

### RF-07 — Proteção na gravação

Serializar a coleção candidata em arquivo temporário no mesmo diretório do destino, fechar os recursos e então substituir o destino por movimento atômico. Nunca truncar ou escrever diretamente sobre o arquivo válido existente.

Se leitura, validação, serialização ou publicação falhar, lançar a exception de persistência e preservar o arquivo anterior. Quando não houver arquivo anterior, uma gravação malsucedida não deve publicar um arquivo parcial.

Se o filesystem não suportar a substituição atômica, a operação deve falhar explicitamente; não usar fallback silencioso de escrita direta ou movimento não atômico. Remover o temporário da operação quando possível, preservando a causa original se a limpeza também falhar. Não realizar limpeza indiscriminada de outros arquivos.

### RF-08 — Uso pela CLI

`Main` deve compor os três repositories JSON e injetá-los nos services existentes. Os services de pedidos e pagamentos devem receber a mesma instância de `JsonOrderRepository`.

Remover a mensagem de que todos os dados serão perdidos ao encerrar. A mensagem de `TerminalSession` deve ser neutra quanto ao backend, pois os testes e outros chamadores ainda podem usar repositories em memória. O README deve explicar os caminhos e a persistência automática em cada operação.

Falha de persistência deve interromper a sessão e produzir uma mensagem compreensível em `System.err`, incluindo o caminho afetado, sem stack trace. Capturar essa exception específica no ponto de entrada, abrangendo composição e execução; não converter a falha em sucesso, ausência de entidade ou recusa de pagamento. O encerramento evita continuar uma sessão cuja operação de armazenamento falhou.

Operações concluídas antes de `0`, EOF ou interrupção posterior já devem estar persistidas. EOF antes da conclusão da coleta de uma operação deve preservar o comportamento atual de não aplicar uma alteração parcial.

## 8. Requisitos técnicos

### RT-01 — Packages e dependências

Implementações, representações de armazenamento, mapeamento e exception técnica devem permanecer em:

```text
dev.manoelreis.ordermanagement.infrastructure.persistence.json
```

Jackson e File I/O devem ficar nesse package. O domínio e os services não podem importar Jackson, `Path`, `Files` ou classes de infraestrutura. `Main`, como raiz de composição, pode fornecer caminhos e conhecer as implementações e a exception técnica.

Usar Java 21, Jackson já declarado e a Java Standard Library. Não adicionar biblioteca de produção para resolver File I/O ou mapeamento.

### RT-02 — Repositories específicos

Criar três classes concretas e finais, implementando diretamente as interfaces atuais:

```text
JsonCustomerRepository implements CustomerRepository
JsonProductRepository implements ProductRepository
JsonOrderRepository implements OrderRepository
```

Não herdar dos repositories em memória nem modificar suas garantias de referência. Nas implementações JSON, um `LinkedHashMap` local à operação pode organizar os registros e preservar a ordem. Não manter entidades em cache entre chamadas.

Um pequeno componente interno de leitura/gravação atômica pode ser compartilhado se a implementação demonstrar repetição efetiva. Ele deve se limitar a I/O e Jackson, permanecer no contexto JSON e não criar uma hierarquia ou contrato genérico de repository.

### RT-03 — Mapeamento explícito

Serializar representações específicas da infraestrutura, preferencialmente records, com os campos descritos na seção 9. Converter explicitamente entre essas representações e os objetos de domínio.

Não serializar diretamente entidades de domínio, adicionar anotações Jackson a elas, criar setters para desserialização, expor collections internas ou usar reflexão para alterar campos privados. Não habilitar tipagem polimórfica automática ou armazenar nomes de classes Java nos documentos.

Configurar Jackson para cumprir as validações estruturais de RF-06; completar explicitamente as validações que não forem cobertas pela configuração. Usar UTF-8 e formato legível, sem depender da formatação para a correção.

### RT-04 — Dinheiro e tempo

Todo cálculo e conversão monetária deve usar `BigDecimal`, sem passagem por `float` ou `double`. Valores monetários devem ser escritos como strings decimais com duas casas e sem notação científica.

`createdAt` deve preservar o `Instant` original, inclusive precisão fracionária, como string ISO-8601 em UTC. Usar o módulo Java Time existente se a representação utilizar `Instant`; conversão explícita com a JDK também é permitida.

### RT-05 — Reconstrução do domínio

Adicionar somente as operações de restauração necessárias em `Order` e `OrderItem`, conforme a seção 9. Essas operações devem validar os dados e preservar o encapsulamento, sem conhecer a origem JSON.

Manter o construtor de novos pedidos e os comportamentos atuais. Não introduzir setter público de status, construtor sem argumentos para Jackson ou atalho de aprovação de pagamento na infraestrutura.

### RT-06 — Recursos e falhas

Fechar streams e readers/writers próprios por `try-with-resources` quando aplicável. Nunca fechar os streams de terminal pertencentes ao chamador. Não capturar genericamente toda `RuntimeException` para ocultar defeitos de programação.

Atomicidade é por arquivo e por `save`. Não há commit conjunto dos três arquivos nem garantia de sincronização física em caso de falha de energia. O encerramento da CLI em falha não exige rollback dos objetos locais já alterados pelo service; o arquivo anterior deve permanecer íntegro.

### RT-07 — Empacotamento e execução

Manter o JAR do projeto sem dependências embutidas. Configurar a cópia de dependências de runtime para `target/dependency/` durante `package`, usando Maven Dependency Plugin com versão explícita e somente o necessário para esse objetivo.

Após `./mvnw clean package`, este comando deve funcionar em um shell Unix, incluindo Jackson no classpath:

```bash
java -cp 'target/order-management-cli-0.1.0-SNAPSHOT.jar:target/dependency/*' dev.manoelreis.ordermanagement.cli.Main
```

Documentar o comando atualizado e o uso do diretório de trabalho para resolver `data/`. Não introduzir fat JAR, instalador ou novos scripts de distribuição nesta etapa. Manter dados de runtime e temporários fora do Git; a regra atual de `data/` já atende a esse requisito.

## 9. Modelo e contratos

### 9.1 Contratos de repository

Preservar, sem alterações:

```text
CustomerRepository
├── save(Customer customer) -> void
├── findById(CustomerId id) -> Optional<Customer>
└── findAll() -> List<Customer>

ProductRepository
├── save(Product product) -> void
├── findById(ProductId id) -> Optional<Product>
└── findAll() -> List<Product>

OrderRepository
├── save(Order order) -> void
├── findById(OrderId id) -> Optional<Order>
└── findAll() -> List<Order>
```

Cada implementação JSON deve oferecer um construtor com `Path file`. Ausência de arquivo é avaliada durante a operação; não é necessário carregar dados no construtor. O objeto de configuração Jackson é um detalhe interno, sem estado global mutável.

### 9.2 Formato dos documentos

A raiz de cada arquivo é um array. Todos os campos apresentados são obrigatórios; a descrição de produto pode ser a string vazia e `items` pode ser um array vazio somente para rascunhos. Não adicionar envelope, versão ou campos calculados.

`customers.json`:

```json
[
  {
    "id": "11111111-1111-4111-8111-111111111111",
    "name": "Ada",
    "email": "ada@example.com"
  }
]
```

`products.json`:

```json
[
  {
    "id": "22222222-2222-4222-8222-222222222222",
    "name": "Keyboard",
    "description": "Mechanical",
    "price": "100.00"
  }
]
```

`orders.json`:

```json
[
  {
    "id": "33333333-3333-4333-8333-333333333333",
    "customerId": "11111111-1111-4111-8111-111111111111",
    "createdAt": "2026-01-01T10:00:00.123456789Z",
    "status": "PAID",
    "items": [
      {
        "productId": "22222222-2222-4222-8222-222222222222",
        "productName": "Keyboard",
        "unitPrice": "100.00",
        "quantity": 2
      }
    ]
  }
]
```

Strings de preço devem conter dígitos, ponto e exatamente duas casas decimais, representando valor positivo. Não aceitar número JSON, vírgula, sinal, símbolo monetário ou notação científica. Valores de texto devem passar pelas normalizações e validações já existentes no domínio.

`createdAt` deve ser uma string aceita por `Instant.parse` com representação UTC terminada em `Z`. `status` aceita somente `DRAFT` e `PAID`. A ordem das propriedades do objeto não é contratual; a ordem dos arrays é.

Não persistir subtotal, total, comportamento de `PaymentMethod`, resultado de recusa ou dados de pagamento inexistentes no modelo atual. O subtotal deve ser calculado pelos itens restaurados.

### 9.3 Restauração de itens

Adicionar a operação:

```text
OrderItem.restore(ProductId productId, String productName,
                  BigDecimal unitPrice, int quantity) -> OrderItem
```

Ela deve produzir um item imutável e validar identificador não nulo, nome não vazio, preço positivo representável com duas casas sem arredondamento e quantidade positiva. Aplicar a mesma normalização de nome e preço dos snapshots originados de `Product`, sem criar um produto fictício para reconstruir o item. As operações existentes de criação e alteração devem continuar válidas.

### 9.4 Restauração de pedidos

Adicionar a operação:

```text
Order.restore(OrderId id, CustomerId customerId, Instant createdAt,
              OrderStatus status, List<OrderItem> items) -> Order
```

Ela deve validar os campos obrigatórios, lista e elementos não nulos, ausência de `productId` repetido e pedido pago não vazio. Deve copiar a estrutura dos itens, preservar sua ordem e restaurar o status informado sem executar pagamento. A lista fornecida não pode permanecer exposta ou compartilhada como estrutura mutável interna.

Repetição de produto no documento deve ser rejeitada, nunca somada ou substituída silenciosamente. O objeto retornado deve cumprir as mesmas restrições de edição e pagamento de um pedido construído pelos fluxos normais.

### 9.5 Exception técnica

Criar `JsonPersistenceException`, derivada de `RuntimeException`, no package JSON. Ela deve informar a operação e o caminho afetado e preservar a causa quando houver uma exception subjacente. A mesma categoria cobre conteúdo inválido, falha de leitura, escrita e publicação.

Essa exception não representa regra de negócio e não deve herdar de `DomainException`. Os services podem propagá-la sem depender de seu tipo concreto.

## 10. Tratamento de erros

- `Path` nulo no construtor, entidade nula em `save` e ID nulo em `findById` devem produzir `IllegalArgumentException`, antes de I/O.
- ID desconhecido e arquivo ausente são resultados normais; não lançar exception nesses casos.
- Caminho que represente diretório em vez de arquivo, arquivo inacessível, JSON inválido e falha de gravação devem produzir `JsonPersistenceException`.
- Violações das invariantes durante restauração devem ser traduzidas pela infraestrutura para `JsonPersistenceException`, preservando a causa e identificando o arquivo; chamadas diretas às operações de domínio devem usar as domain exceptions existentes.
- `save` sobre documento inválido deve falhar sem modificá-lo, mesmo que a entidade recebida seja válida.
- Falha de movimento atômico deve preservar o destino anterior e ser apresentada como falha de persistência.
- A CLI deve encerrar em falha de persistência. Falhas de I/O do terminal continuam sendo tratadas como `IOException`, com mensagem distinta.
- Não exibir conteúdo completo dos registros ou stack trace nas mensagens usuais do terminal.

## 11. Arquivos afetados

A implementação futura poderá criar ou alterar:

```text
src/main/java/dev/manoelreis/ordermanagement/infrastructure/persistence/json/
src/main/java/dev/manoelreis/ordermanagement/domain/order/Order.java
src/main/java/dev/manoelreis/ordermanagement/domain/order/OrderItem.java
src/main/java/dev/manoelreis/ordermanagement/cli/Main.java
src/main/java/dev/manoelreis/ordermanagement/cli/menu/TerminalSession.java
src/test/java/dev/manoelreis/ordermanagement/infrastructure/persistence/json/
src/test/java/dev/manoelreis/ordermanagement/domain/order/
src/test/java/dev/manoelreis/ordermanagement/cli/
src/test/resources/
pom.xml
README.md
specs/SPEC-012-json-persistence.md
specs/README.md
```

Fixtures em `src/test/resources/` são permitidas quando necessárias; preferir documentos pequenos criados em diretórios temporários dos testes. Arquivos reais de `data/` não devem ser versionados.

A criação desta specification autoriza somente este documento e a atualização do índice em `specs/README.md`. A implementação descrita será uma tarefa posterior. Não criar código de persistência, alterar `pom.xml` ou mudar o estado para `Concluída` durante a elaboração da spec.

## 12. Estratégia de testes

Usar JUnit Jupiter, entidades e services reais e `@TempDir`. Preferir operações reais de arquivos; Mockito não é necessário nos fluxos normais. Não ler nem gravar `data/` nos testes.

### 12.1 Contratos dos três repositories

Cobrir para cada implementação:

- arquivo ausente: lista vazia e busca ausente, sem criação pelo leitor;
- `[]` como coleção vazia;
- primeiro `save`, incluindo criação de diretórios pais;
- round-trip em nova instância apontando para o mesmo arquivo;
- inserção de múltiplas entidades, substituição por ID e preservação da ordem;
- snapshot não modificável e consultas independentes de mutações em objetos retornados;
- alteração de objeto salvo sem novo `save` não altera o arquivo; com `save`, é recuperada em outra consulta;
- leitura sequencial por instâncias com o mesmo caminho e isolamento por caminhos diferentes;
- argumentos nulos rejeitados antes de I/O;
- caracteres acentuados e caracteres que exigem escape JSON;
- falhas estruturais e de domínio de RF-06, verificando ausência de resultados parciais;
- `save` em arquivo inválido preserva exatamente os bytes anteriores;
- caminho de diretório e falhas de I/O apresentadas com caminho e causa.

### 12.2 Domínio e estado histórico

Testar as operações de restauração, incluindo campos nulos, item inválido, duplicação de produto, cópia defensiva da lista e pedido pago vazio. Preservar os testes atuais do domínio.

No repository de pedidos, verificar rascunho vazio, rascunho com itens, pedido pago, ordem dos itens, quantidade, precisão do `Instant`, dinheiro de valor elevado e subtotal exato. Comparar os atributos e comportamentos: `equals` das entidades compara somente identidade e não prova round-trip.

Salvar um pedido e depois alterar e salvar o produto do catálogo. A restauração do pedido deve manter nome e preço históricos. Pedido pago restaurado deve rejeitar edição e novo pagamento; rascunho restaurado deve permitir edição válida. Nenhum método de pagamento deve ser chamado durante a leitura.

### 12.3 Gravação atômica

Verificar substituição bem-sucedida e limpeza do temporário. Exercitar uma falha antes da publicação e comprovar que o destino anterior permanece byte a byte intacto, que a exception mantém sua causa e que nenhum arquivo parcial é publicado.

O teste de falha deve ser determinístico, sem depender de permissões Unix que possam ser ignoradas em execução como administrador. Se necessário, permitir um pequeno ponto interno de substituição da operação de publicação para o teste, sem expô-lo nas interfaces de repository nem construir um framework de filesystem. Cobrir também movimento atômico não suportado e ausência de fallback.

### 12.4 Integração e CLI

Compor services reais com repositories JSON, `Clock.fixed` e caminhos temporários. Cadastrar cliente e produto, criar pedido, adicionar e alterar itens, processar recusa e depois aprovação. Recriar todos os repositories e services, verificando dados e status `PAID` persistidos. Confirmar também persistência de remoção de item e de rascunho após recusa.

Executar duas sessões da CLI com nova composição e os mesmos caminhos temporários: a segunda deve consultar os dados da primeira e rejeitar edição de pedido pago. Preservar as opções existentes, EOF e testes com repositories em memória. A sessão não deve anunciar perda dos dados ao encerrar.

Cobrir a tradução de falha de persistência no ponto de entrada, tanto na leitura quanto no `save`, com saída capturada: mensagem distinta, caminho afetado, ausência de stack trace e de confirmação de sucesso da operação malsucedida, seguida de encerramento.

Na implementação, executar `./mvnw test` e `./mvnw clean package`. Fazer smoke test do comando documentado a partir de diretório de trabalho temporário, usando o classpath do build com caminhos absolutos: criar dados, encerrar, iniciar outro processo e confirmar consulta do pedido pago. Não usar ou sobrescrever dados pessoais de `data/` para esse teste.

## 13. Critérios de aceite

- **CA-01:** os três repositories JSON implementam os contratos atuais, sem alterações em `application` ou nos repositories em memória.
- **CA-02:** arquivo ausente é vazio; primeiro `save` cria o armazenamento; todo `save` bem-sucedido pode ser recuperado em nova instância.
- **CA-03:** inserção, substituição, ordem e snapshots obedecem ao contrato, e mutações sem `save` não alteram arquivos.
- **CA-04:** clientes, produtos, UUIDs, valores monetários e instantes são preservados sem perda de precisão.
- **CA-05:** pedidos mantêm itens históricos, subtotal e status; restaurar `PAID` não processa pagamento e preserva as restrições do domínio.
- **CA-06:** documentos inválidos são rejeitados integralmente, sem correção silenciosa ou sobrescrita.
- **CA-07:** gravação usa temporário e substituição atômica; falhas preservam o destino anterior e não publicam conteúdo parcial.
- **CA-08:** Jackson, representações de arquivo e I/O ficam na infraestrutura; operações de restauração do domínio validam suas invariantes.
- **CA-09:** a CLI usa JSON por padrão, compartilha o repository de pedidos e recupera dados de execuções anteriores.
- **CA-10:** falhas de persistência produzem mensagem com caminho e encerram a sessão, sem sucesso falso ou stack trace.
- **CA-11:** o comando com dependências funciona após `package`, e dados de runtime permanecem fora do Git.
- **CA-12:** testes, build e smoke test entre processos passam; a documentação explica formato, caminhos e limites de uso.

## 14. Definition of Done

- [x] Requisitos funcionais atendidos
- [x] Requisitos técnicos atendidos
- [x] Três repositories JSON implementados e testados
- [x] Restauração validada de pedidos e itens implementada e testada
- [x] Round-trip e preservação de snapshots históricos verificados
- [x] Documentos inválidos e falhas de gravação cobertos pela validação estrita de infraestrutura
- [x] Composição JSON e tratamento de falhas na CLI verificados
- [x] Testes relevantes aprovados com `./mvnw test`
- [x] Build aprovado com `./mvnw clean package`
- [x] Smoke test entre execuções com o classpath documentado aprovado
- [x] Nenhum item fora do escopo implementado
- [x] Documentação afetada atualizada
- [x] Estado alterado para `Concluída` somente após validar a implementação

## 15. Critério de parada

Parar quando os três repositories JSON preservarem os dados e comportamentos atuais entre execuções, a CLI usar essa composição com tratamento explícito de falhas, e testes, build e smoke test estiverem aprovados.

Concorrência, migrações, backups, transações entre arquivos, novos casos de uso, descontos e relatórios na CLI exigem specs próprias. A elaboração deste documento termina com a spec planejada e indexada; não inicia sua implementação.
