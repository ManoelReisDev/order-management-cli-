# SPEC-003 — Casos de Uso de Customer

## 1. Metadados

- **Estado:** Concluída
- **Dependências:** SPEC-002 concluída
- **Entrega:** Casos de uso de cadastro, consulta, listagem e alteração de clientes, coordenados pela camada `application` por meio de uma abstração de repository

## 2. Objetivo

Introduzir a primeira parte da camada `application`, limitada ao contexto de clientes.

Esta spec deve permitir cadastrar um cliente, consultá-lo por identificador, listar os clientes existentes, renomeá-lo e alterar seu e-mail. A aplicação deve coordenar o modelo de domínio criado na SPEC-002 e uma abstração de persistência, sem conhecer terminal, arquivos, JSON ou qualquer implementação concreta de repository.

O resultado esperado é uma API de aplicação utilizável diretamente por código Java e coberta por testes unitários. Nenhuma interface de usuário ou persistência real deve ser criada nesta etapa.

## 3. Contexto

A SPEC-002 introduziu `Customer` e `CustomerId`, incluindo as regras de nome e e-mail. A camada `application` deve agora coordenar esses objetos em operações completas, preservando as regras dentro do domínio.

O fluxo desta etapa é:

```text
Caller
   │
   ▼
CustomerApplicationService
   ├──► Customer
   └──► CustomerRepository
```

`CustomerRepository` representa apenas a necessidade de armazenamento observada pelos casos de uso. Sua implementação pertence a uma specification futura.

## 4. Pré-condições

- A SPEC-002 deve estar concluída.
- `Customer`, `CustomerId` e `InvalidCustomerException` devem estar implementados e testados.
- O projeto deve compilar e executar os testes com Java 21 pelo Maven Wrapper.
- Nenhuma dependência adicional deve ser necessária.

## 5. Escopo

Esta spec autoriza:

- criar uma abstração de repository específica para `Customer`;
- criar um application service específico para os casos de uso de cliente;
- cadastrar um cliente com identificador novo;
- buscar um cliente por identificador;
- listar os clientes fornecidos pelo repository;
- renomear um cliente existente;
- alterar o e-mail de um cliente existente;
- representar explicitamente a tentativa de operar sobre um cliente inexistente;
- aplicar injeção de dependência manual por construtor;
- criar testes unitários da camada `application.customer`.

## 6. Fora do escopo

Não implementar nesta etapa:

- implementações de repository em memória, JSON ou qualquer outro mecanismo;
- leitura ou escrita de arquivos;
- Jackson na camada `application`;
- menus, comandos, entrada ou saída de terminal;
- casos de uso de produto, pedido, pagamento ou relatório;
- exclusão ou desativação de clientes;
- paginação, filtros, ordenação ou busca por nome e e-mail;
- regra de unicidade global de e-mail;
- DTOs, mappers, controllers ou presenters;
- interfaces separadas para cada operação do application service;
- repositories genéricos, services genéricos ou classes base;
- alterações no modelo de domínio criado pela SPEC-002;
- Spring, container de injeção de dependçncia ou qualquer tecnologia proibida pelo `AGENTS.md`.

## 7. Requisitos funcionais

### RF-01 — Cadastro de cliente

A aplicação deve receber nome e e-mail, gerar um novo `CustomerId`, criar um `Customer` e solicitar seu armazenamento ao repository.

O cliente cadastrado deve ser retornado ao chamador com os dados normalizados pelo domínio.

### RF-02 — Consulta por identificador

A aplicação deve consultar um cliente por `CustomerId`.

Quando o repository encontrar o cliente, a aplicação deve retorná-lo. Quando não encontrar, deve sinalizar `CustomerNotFoundException`.

### RF-03 — Listagem de clientes

A aplicação deve retornar todos os clientes fornecidos pelo repository, preservando a ordem recebida.

Quando não houver clientes, deve retornar uma lista vazia. O chamador não deve conseguir adicionar ou remover elementos da lista retornada.

### RF-04 — Renomeação de cliente

A aplicação deve localizar o cliente pelo identificador, solicitar ao próprio objeto de domínio a alteração do nome e solicitar ao repository o armazenamento do estado alterado.

A operação deve retornar o cliente alterado.

### RF-05 — Alteração de e-mail

A aplicação deve localizar o cliente pelo identificador, solicitar ao próprio objeto de domínio a alteração do e-mail e solicitar ao repository o armazenamento do estado alterado.

A operação deve retornar o cliente alterado.

### RF-06 — Preservação das regras de domínio

Nome e e-mail não devem ser validados ou normalizados novamente pela camada `application`. As operações devem utilizar os comportamentos públicos de `Customer` e propagar `InvalidCustomerException` quando os dados violarem as invariantes definidas na SPEC-002.

Se a criação ou alteração for rejeitada pelo domínio, a aplicação não deve solicitar o armazenamento daquele estado inválido.

## 8. Requisitos técnicos

### RT-01 — Linguagem e package

O código deve usar Java 21 e permanecer no package:

```text
dev.manoelreis.ordermanagement.application.customer
```

Os testes devem acompanhar esse package na árvore de testes.

### RT-02 — Direção de dependências

`application.customer` pode depender de `domain.customer` e das APIs da Java Standard Library.

O domínio não pode depender de `application`. A camada `application` não pode depender de `infrastructure`, `cli`, Jackson ou detalhes de persistência.

### RT-03 — Abstração de repository

`CustomerRepository` deve ser uma interface específica para clientes e declarar somente as operações exigidas nesta spec:

```text
save(Customer customer)
findById(CustomerId id) -> Optional<Customer>
findAll() -> List<Customer>
```

O contrato não deve revelar estruturas ou detalhes de uma futura implementação de persistência.

### RT-04 — Application service

`CustomerApplicationService` deve ser uma classe concreta e final. Ela deve receber `CustomerRepository` obrigatoriamente pelo construtor e não deve criar, localizar ou escolher implementações concretas dessa dependência.

O service deve oferecer operações com nomes que expressem os casos de uso:

```text
registerCustomer(name, email)
findCustomerById(customerId)
listCustomers()
renameCustomer(customerId, name)
changeCustomerEmail(customerId, email)
```

### RT-05 — Optional

`CustomerRepository.findById` deve usar `Optional<Customer>` para representar a ausência legítima de resultado. O application service deve resolver essa ausência e não deve retornar `null` nem `Optional` nos casos de uso definidos nesta spec.

### RT-06 — Collections

`CustomerRepository.findAll` deve retornar `List<Customer>`. `CustomerApplicationService.listCustomers` deve fornecer uma cópia estrutural não modificável da lista recebida, preservando sua ordem e sem expor a collection do repository.

Esta proteção se refere à estrutura da lista; não exige copiar as entidades contidas nela.

### RT-07 — Dependências e simplicidade

Nenhuma dependência de produção deve ser adicionada. JUnit Jupiter deve ser usado nos testes, e Mockito pode ser usado somente para isolar `CustomerRepository`.

Não criar commands, DTOs ou results enquanto os parâmetros e retornos do domínio forem suficientes para estes casos de uso.

## 9. Modelo e contratos

### 9.1 `CustomerRepository`

Porta de saída utilizada pela aplicação para armazenar e recuperar clientes.

Responsabilidades:

- armazenar o cliente informado por `save`;
- representar o resultado de `findById` com `Optional`;
- fornecer por `findAll` uma lista não nula, possivelmente vazia.

Esta interface não define nesta etapa:

- tecnologia de persistência;
- comportamento entre execuções do processo;
- ordenação própria;
- controle transacional;
- consultas além das estritamente necessárias.

### 9.2 `CustomerApplicationService`

Coordenador dos casos de uso de cliente.

Responsabilidades:

- gerar a identidade no cadastro;
- criar e modificar clientes por meio do modelo de domínio;
- usar `CustomerRepository` para consulta e armazenamento;
- converter a ausência de um cliente em erro explícito de aplicação;
- proteger a estrutura da lista devolvida ao chamador.

O service não deve conter regras de formato de nome ou e-mail, nem conhecer como os clientes são armazenados.

### 9.3 `CustomerNotFoundException`

Exception não verificada da camada `application.customer`, utilizada quando um caso de uso exige um cliente existente e `CustomerRepository.findById` não o encontra.

A exception deve identificar o `CustomerId` procurado em sua mensagem e não deve ser adicionada à hierarquia de exceptions de domínio.

## 10. Tratamento de erros

- repository ausente na construção do application service deve ser rejeitado imediatamente;
- `CustomerId` nulo em operações de consulta ou alteração deve ser rejeitado como argumento inválido antes de consultar o repository;
- cliente ausente deve produzir `CustomerNotFoundException`;
- nome ou e-mail inválido deve continuar produzindo `InvalidCustomerException`, conforme o domínio;
- uma falha deve interromper o caso de uso e impedir uma chamada posterior a `save`;
- exceptions técnicas do repository não devem ser ocultadas, traduzidas ou envolvidas nesta etapa;
- não retornar `null`, valores sentinela ou mensagens textuais para representar falhas.

## 11. Arquivos afetados

A implementação futura desta spec poderá criar ou alterar somente:

```text
src/main/java/dev/manoelreis/ordermanagement/application/customer/
src/test/java/dev/manoelreis/ordermanagement/application/customer/
README.md
specs/SPEC-003-customer-application.md
```

Esta criação da specification também autoriza atualizar o índice em `specs/README.md`.

Não alterar `domain`, `infrastructure`, `cli`, `pom.xml` ou outros contextos de `application` para implementar esta spec.

## 12. Estratégia de testes

Usar JUnit Jupiter e objetos reais de `Customer`. Usar Mockito para isolar exclusivamente o contrato `CustomerRepository`.

Cobrir, no mínimo:

- rejeição de repository nulo na construção do service;
- cadastro de cliente com novo identificador, dados normalizados e chamada a `save`;
- propagação de `InvalidCustomerException` no cadastro sem chamada a `save`;
- consulta bem-sucedida por identificador;
- `CustomerNotFoundException` na consulta de identificador inexistente;
- listagem vazia;
- listagem que preserva a ordem fornecida pelo repository;
- impossibilidade de adicionar ou remover elementos da lista retornada;
- renomeação bem-sucedida seguida de `save`;
- alteração de e-mail bem-sucedida seguida de `save`;
- falha das alterações quando o cliente não existe, sem chamada a `save`;
- propagação de `InvalidCustomerException` nas alterações inválidas, sem chamada a `save`;
- rejeição de identificador nulo antes de qualquer consulta ao repository.

Os testes devem observar resultados e interações relevantes, sem testar detalhes privados, reproduzir a implementação ou depender de uma futura classe de infraestrutura.

## 13. Critérios de aceite

- **CA-01:** um cliente válido pode ser cadastrado com novo `CustomerId` e enviado ao repository.
- **CA-02:** clientes podem ser consultados por identificador sem expor `Optional` ao chamador do application service.
- **CA-03:** a ausência de cliente produz `CustomerNotFoundException` com o identificador procurado.
- **CA-04:** clientes podem ser listados na ordem fornecida pelo repository por meio de uma lista estruturalmente não modificável.
- **CA-05:** nome e e-mail de um cliente existente podem ser alterados usando os comportamentos do domínio e o estado resultante é enviado ao repository.
- **CA-06:** dados inválidos continuam sendo rejeitados pelo domínio e não são enviados ao repository.
- **CA-07:** a dependência de `CustomerRepository` é recebida manualmente pelo construtor.
- **CA-08:** `application.customer` não depende de infraestrutura, CLI ou Jackson.
- **CA-09:** nenhuma implementação concreta de repository é criada.
- **CA-10:** todos os testes e o build Maven passam.

## 14. Definition of Done

- [x] Requisitos funcionais atendidos
- [x] Requisitos técnicos atendidos
- [x] `CustomerRepository` implementado como abstração específica
- [x] `CustomerApplicationService` implementado e testado
- [x] `CustomerNotFoundException` implementada e testada pelos casos de uso
- [x] Testes relevantes implementados e aprovados
- [x] Build aprovado com `./mvnw clean package`
- [x] Nenhum item fora do escopo implementado
- [x] Documentação afetada atualizada
- [x] Estado da spec alterado para `Concluída` somente após a validação

## 15. Critério de parada

Parar quando cadastro, consulta por identificador, listagem, renomeação e alteração de e-mail estiverem coordenados por `CustomerApplicationService`, cobertos por testes e desacoplados de qualquer implementação de persistência.

Não iniciar implementações em memória ou JSON, CLI, exclusão de clientes, unicidade global de e-mail nem casos de uso de outros contextos. Essas capacidades devem ser definidas e autorizadas por specifications futuras.
