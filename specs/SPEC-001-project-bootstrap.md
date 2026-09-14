# SPEC-001 — Bootstrap e Arquitetura Inicial do Order Management CLI

## 1. Objetivo

Preparar a estrutura inicial do projeto **Order Management CLI**, um sistema de gestão de pedidos desenvolvido em Java puro.

Esta especificação trata exclusivamente de:

- criação do projeto;
- configuração do Maven;
- definição da versão do Java;
- organização de packages;
- criação da estrutura de diretórios;
- instalação das dependências;
- configuração do ambiente de testes;
- preparação da persistência futura em JSON;
- definição das fronteiras arquiteturais;
- configuração básica do repositório;
- criação da estrutura destinada às próximas specifications.

**Nenhuma regra de negócio deve ser implementada nesta etapa.**

Não implementar:

- `Product`;
- `Customer`;
- `Order`;
- `OrderItem`;
- pagamentos;
- descontos;
- repositories;
- services;
- relatórios;
- CLI;
- persistência.

O resultado esperado é somente o **bootstrap arquitetural do projeto**.

---

# 2. Contexto

O projeto tem como objetivo estudar Java antes da introdução de Spring.

A pergunta central do projeto é:

> **"Eu sei Java ou simplesmente sei Spring?"**

Consequentemente, a arquitetura deve favorecer contato direto com os mecanismos da linguagem e da JDK.

O projeto deverá futuramente exercitar:

- classes e objetos;
- encapsulamento;
- interfaces;
- polimorfismo;
- records;
- enums;
- exceptions;
- generics;
- collections;
- Optional;
- lambdas;
- Stream API;
- Date/Time API;
- I/O;
- Maven;
- JUnit;
- Mockito.

---

# 3. Fora do escopo

Não adicionar:

- Spring;
- Spring Boot;
- Spring Data;
- Spring IoC;
- Hibernate;
- JPA;
- Jakarta Persistence;
- banco de dados;
- Flyway;
- Liquibase;
- Docker;
- Lombok;
- MapStruct;
- frameworks de Dependency Injection;
- frameworks web;
- servidor HTTP;
- REST;
- GraphQL;
- mensageria.

Também não criar abstrações antecipadas sem necessidade concreta.

---

# 4. Stack

Utilizar:

- Java 21 LTS;
- Maven;
- Maven Wrapper;
- Jackson;
- JUnit Jupiter;
- Mockito;
- Git;
- UTF-8.

OBS: Se o Maven não estiver instalador, instale-o.

Priorizar sempre a Java Standard Library antes da introdução de dependências externas.

---

# 5. Configuração Maven

Configurar:

```text
groupId:
dev.manoelreis

artifactId:
order-management-cli

name:
Order Management CLI

version:
0.1.0-SNAPSHOT

base package:
dev.manoelreis.ordermanagement
```

O projeto deverá utilizar Maven Wrapper.

Devem existir:

```text
.mvn/
mvnw
mvnw.cmd
pom.xml
```

O build deverá ser executável através de:

```bash
./mvnw clean package
```

E os testes através de:

```bash
./mvnw test
```

---

# 6. Estrutura raiz

Criar:

```text
order-management-cli/
│
├── .mvn/
│
├── data/
│
├── docs/
│   └── architecture/
│
├── specs/
│   ├── README.md
│   └── SPEC-001-project-bootstrap.md
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   │
│   └── test/
│       ├── java/
│       └── resources/
│
├── .editorconfig
├── .gitignore
├── CLAUDE.md
├── README.md
├── mvnw
├── mvnw.cmd
└── pom.xml
```

---

# 7. Diretório `specs`

Criar na raiz:

```text
specs/
├── README.md
└── SPEC-001-project-bootstrap.md
```

Esse diretório será a **fonte de verdade das especificações de implementação do projeto**.

Cada evolução relevante deverá possuir sua própria specification.

Convenção:

```text
SPEC-XXX-description.md
```

Exemplos futuros:

```text
SPEC-001-project-bootstrap.md
SPEC-002-domain-model.md
SPEC-003-repositories.md
SPEC-004-order-workflow.md
SPEC-005-payment-system.md
SPEC-006-reporting.md
SPEC-007-json-persistence.md
SPEC-008-cli.md
```

Os nomes são indicativos. Não criar as specs futuras nesta etapa.

---

# 8. `specs/README.md`

Criar um README curto explicando que:

- `specs/` contém as especificações do projeto;
- cada SPEC representa uma unidade planejada de evolução;
- specs devem ser implementadas em ordem quando houver dependências;
- uma spec não autoriza implementar funcionalidades pertencentes às próximas;
- `CLAUDE.md` contém as regras e contexto global;
- a spec ativa define o escopo específico da implementação.

Estabelecer a seguinte prioridade:

```text
CLAUDE.md
    ↓
SPEC ativa
    ↓
Código existente
```

O `CLAUDE.md` estabelece princípios globais.

A SPEC estabelece o objetivo específico da tarefa atual.

---

# 9. Diretório `docs`

Criar:

```text
docs/
└── architecture/
```

`docs` será utilizado para documentação complementar.

Poderá futuramente conter:

- diagramas;
- ADRs;
- explicações arquiteturais;
- decisões técnicas;
- fluxos;
- documentação de conceitos.

Não utilizar `docs` para armazenar specifications.

Specifications pertencem exclusivamente a:

```text
/specs
```

---

# 10. Diretório `data`

Criar:

```text
data/
```

Esse diretório será utilizado futuramente pela persistência baseada em arquivos.

Possíveis arquivos futuros:

```text
products.json
customers.json
orders.json
```

Não criar dados de exemplo.

Arquivos gerados em runtime não deverão ser versionados.

Manter apenas um placeholder quando necessário para preservar o diretório no Git.

---

# 11. Package base

Todo código deverá ficar abaixo de:

```text
dev.manoelreis.ordermanagement
```

Estrutura principal:

```text
dev.manoelreis.ordermanagement
│
├── domain
├── application
├── infrastructure
├── cli
└── shared
```

---

# 12. Domain

Criar:

```text
domain/
├── customer/
├── product/
├── order/
├── payment/
├── discount/
└── exception/
```

`domain` deverá representar conceitos e regras fundamentais do negócio.

Futuramente poderá conter:

- entities;
- value objects;
- records;
- enums;
- interfaces de domínio;
- policies;
- domain exceptions.

O domínio não deverá conhecer:

- CLI;
- Jackson;
- JSON;
- arquivos;
- infraestrutura.

---

# 13. `domain.customer`

Reservado aos conceitos relacionados a clientes.

Possíveis elementos futuros:

```text
Customer
CustomerId
```

Não implementar agora.

---

# 14. `domain.product`

Reservado aos conceitos relacionados ao catálogo.

Possíveis elementos futuros:

```text
Product
ProductId
```

Não implementar agora.

---

# 15. `domain.order`

Reservado ao domínio de pedidos.

Possíveis elementos futuros:

```text
Order
OrderItem
OrderStatus
OrderId
```

Não implementar agora.

---

# 16. `domain.payment`

Reservado aos conceitos relacionados a pagamento.

Possíveis elementos futuros:

```text
PaymentMethod
PaymentResult
PixPayment
CreditCardPayment
BankSlipPayment
```

Não implementar agora.

---

# 17. `domain.discount`

Reservado às políticas de desconto.

Possíveis conceitos:

```text
DiscountPolicy
NoDiscount
PercentageDiscount
MinimumValueDiscount
```

Não implementar agora.

---

# 18. `domain.exception`

Reservado às exceptions relacionadas às regras de domínio.

Possíveis elementos futuros:

```text
DomainException
InvalidOrderStateException
InvalidQuantityException
```

Não implementar agora.

---

# 19. Application

Criar:

```text
application/
├── customer/
├── product/
├── order/
├── payment/
└── report/
```

Essa camada deverá futuramente coordenar os casos de uso.

Ela poderá:

- consultar repositories;
- carregar objetos;
- executar operações de domínio;
- coordenar pagamentos;
- persistir alterações;
- gerar relatórios.

Não deverá conhecer detalhes da interface de terminal.

---

# 20. Infrastructure

Criar:

```text
infrastructure/
└── persistence/
    ├── memory/
    └── json/
```

`infrastructure` conterá implementações de mecanismos externos ao domínio.

As primeiras estratégias de persistência serão:

```text
Persistence
├── Memory
└── JSON File
```

Nenhum repository deverá ser implementado nesta spec.

---

# 21. `persistence.memory`

Reservado às implementações em memória.

Futuramente deverá exercitar estruturas como:

```text
HashMap
List
Set
```

---

# 22. `persistence.json`

Reservado à persistência em arquivos JSON.

Futuramente utilizará Jackson para:

- serialização;
- desserialização;
- leitura;
- escrita;
- adaptação dos dados persistidos.

Jackson não deverá contaminar `domain`.

---

# 23. CLI

Criar:

```text
cli/
├── menu/
├── input/
└── output/
```

Responsabilidades futuras:

### `menu`

Navegação entre menus.

### `input`

Leitura e conversão das entradas do usuário.

### `output`

Apresentação de informações.

Nenhuma regra de negócio deverá existir na CLI.

Fluxo conceitual:

```text
CLI
 ↓
Application
 ↓
Domain
```

---

# 24. Shared

Criar:

```text
shared/
```

Esse package deverá permanecer mínimo.

Não criar antecipadamente:

```text
utils/
helpers/
commons/
misc/
```

`shared` não deverá se transformar em depósito de código sem responsabilidade definida.

---

# 25. Estrutura Java resultante

```text
src/main/java/dev/manoelreis/ordermanagement/
│
├── domain/
│   ├── customer/
│   ├── product/
│   ├── order/
│   ├── payment/
│   ├── discount/
│   └── exception/
│
├── application/
│   ├── customer/
│   ├── product/
│   ├── order/
│   ├── payment/
│   └── report/
│
├── infrastructure/
│   └── persistence/
│       ├── memory/
│       └── json/
│
├── cli/
│   ├── menu/
│   ├── input/
│   └── output/
│
└── shared/
```

---

# 26. Estrutura de testes

Criar:

```text
src/test/java/dev/manoelreis/ordermanagement/
│
├── domain/
├── application/
├── infrastructure/
└── cli/
```

Os testes deverão acompanhar a organização do código principal.

Não criar testes vazios apenas para preencher a estrutura.

---

# 27. Test Resources

Criar:

```text
src/test/resources/
```

Futuramente armazenará:

- fixtures;
- JSON válido;
- JSON inválido;
- arquivos de teste;
- cenários de serialização.

Não adicionar fixtures agora.

---

# 28. Main Resources

Criar:

```text
src/main/resources/
```

Não armazenar dados produzidos em runtime nesse diretório.

Dados persistidos pertencem a:

```text
/data
```

---

# 29. Dependências de produção

Adicionar somente o necessário.

## Jackson

Adicionar suporte a JSON para a futura persistência.

Preparar os módulos necessários para trabalhar adequadamente com:

- objetos Java;
- JSON;
- Java Date/Time API.

Manter as versões dos módulos Jackson compatíveis entre si.

Jackson deverá ser tratado como dependência de infraestrutura.

Não utilizar annotations ou tipos Jackson dentro do domínio quando isso puder ser evitado.

---

# 30. Dependências de teste

Adicionar:

## JUnit Jupiter

Framework principal para testes.

## Mockito

Biblioteca para mocks e test doubles.

Mockito deverá ser utilizado somente quando houver uma dependência que realmente precise ser isolada.

---

# 31. Dependências proibidas

Não adicionar:

```text
Lombok
Spring
Spring Boot
Spring Data
Hibernate
JPA
H2
MapStruct
Guava
Apache Commons
Gson
Testcontainers
Dependency Injection frameworks
```

Uma nova dependência só deverá ser introduzida quando houver necessidade concreta.

---

# 32. Java Standard Library

Priorizar:

```text
java.util
├── List
├── Set
├── Map
├── Optional
└── UUID

java.util.stream
└── Stream API

java.time
├── LocalDate
└── LocalDateTime

java.nio.file
└── File I/O

java.math
└── BigDecimal
```

Não instalar bibliotecas para funcionalidades já adequadamente oferecidas pela JDK.

---

# 33. Maven Plugins

Configurar o ciclo básico de desenvolvimento.

## Maven Compiler Plugin

Configurar Java 21.

## Maven Surefire Plugin

Configurar execução dos testes JUnit.

O projeto deverá posteriormente ser preparado para gerar um JAR executável.

Não é necessário implementar a aplicação executável nesta spec.

---

# 34. Git

Criar `.gitignore` apropriado para Java/Maven.

Ignorar:

```text
target/
IDE metadata
runtime data
logs
temporary files
OS generated files
```

Não ignorar:

```text
.mvn/
mvnw
mvnw.cmd
pom.xml
src/
specs/
docs/
CLAUDE.md
```

---

# 35. EditorConfig

Criar `.editorconfig`.

Definir:

- UTF-8;
- newline consistente;
- newline ao final;
- remoção de trailing whitespace;
- quatro espaços para Java.

Não adicionar formatador externo nesta etapa.

---

# 36. README

Criar README inicial contendo:

- nome;
- propósito;
- objetivo educacional;
- stack;
- requisitos;
- build;
- testes;
- estrutura arquitetural resumida;
- informação explícita de que Spring não é utilizado.

Não documentar funcionalidades ainda inexistentes como concluídas.

---

# 37. CLAUDE.md

Criar:

```text
/CLAUDE.md
```

O arquivo deverá ficar na raiz do projeto.

Ele funcionará como contexto global para agentes de código trabalhando no repositório.

Seu conteúdo está definido separadamente nesta especificação.

---

# 38. Direção das dependências

Respeitar conceitualmente:

```text
┌───────────────┐
│      CLI      │
└───────┬───────┘
        │
        ▼
┌───────────────┐
│  Application  │
└───────┬───────┘
        │
        ▼
┌───────────────┐
│    Domain     │
└───────────────┘
        ▲
        │
┌───────┴───────┐
│Infrastructure │
└───────────────┘
```

Regra fundamental:

> `domain` não deve depender de `infrastructure`.

---

# 39. Evitar overengineering

Não implementar deliberadamente uma arquitetura completa baseada em:

- Clean Architecture;
- Hexagonal Architecture;
- Onion Architecture;
- CQRS;
- Event Sourcing;
- DDD estratégico.

Alguns princípios dessas arquiteturas podem orientar decisões, mas não devem aumentar artificialmente a complexidade.

---

# 40. Abstrações antecipadas

Não criar:

```text
BaseEntity
AbstractRepository
AbstractService
GenericService
BaseController
GenericMapper
GenericResult
CommandBus
QueryBus
Mediator
EventBus
```

Abstrações deverão surgir a partir de problemas concretos.

---

# 41. Validação do bootstrap

Após finalizar:

```bash
./mvnw clean
./mvnw compile
./mvnw test
./mvnw package
```

Todos deverão finalizar com sucesso.

---

# 42. Dependências esperadas

O projeto deverá possuir aproximadamente:

```text
Production
└── Jackson
    └── JSON

Test
├── JUnit Jupiter
└── Mockito
```

O restante deverá vir prioritariamente da JDK.

---

# 43. Estrutura final

```text
order-management-cli/
│
├── .mvn/
├── data/
│
├── docs/
│   └── architecture/
│
├── specs/
│   ├── README.md
│   └── SPEC-001-project-bootstrap.md
│
├── src/
│   ├── main/
│   │   ├── java/dev/manoelreis/ordermanagement/
│   │   │   ├── domain/
│   │   │   │   ├── customer/
│   │   │   │   ├── product/
│   │   │   │   ├── order/
│   │   │   │   ├── payment/
│   │   │   │   ├── discount/
│   │   │   │   └── exception/
│   │   │   │
│   │   │   ├── application/
│   │   │   │   ├── customer/
│   │   │   │   ├── product/
│   │   │   │   ├── order/
│   │   │   │   ├── payment/
│   │   │   │   └── report/
│   │   │   │
│   │   │   ├── infrastructure/
│   │   │   │   └── persistence/
│   │   │   │       ├── memory/
│   │   │   │       └── json/
│   │   │   │
│   │   │   ├── cli/
│   │   │   │   ├── menu/
│   │   │   │   ├── input/
│   │   │   │   └── output/
│   │   │   │
│   │   │   └── shared/
│   │   │
│   │   └── resources/
│   │
│   └── test/
│       ├── java/dev/manoelreis/ordermanagement/
│       │   ├── domain/
│       │   ├── application/
│       │   ├── infrastructure/
│       │   └── cli/
│       │
│       └── resources/
│
├── .editorconfig
├── .gitignore
├── CLAUDE.md
├── README.md
├── mvnw
├── mvnw.cmd
└── pom.xml
```

---

# 44. Definition of Done

A SPEC-001 estará concluída quando:

- [ ] projeto Maven criado;
- [ ] Java 21 configurado;
- [ ] Maven Wrapper configurado;
- [ ] `pom.xml` configurado;
- [ ] package base criado;
- [ ] `domain/` preparado;
- [ ] `application/` preparado;
- [ ] `infrastructure/` preparado;
- [ ] `cli/` preparado;
- [ ] `shared/` preparado;
- [ ] estrutura de testes preparada;
- [ ] `resources/` preparados;
- [ ] `data/` criado;
- [ ] `docs/architecture/` criado;
- [ ] `specs/` criado;
- [ ] `specs/README.md` criado;
- [ ] `specs/SPEC-001-project-bootstrap.md` criado;
- [ ] `CLAUDE.md` criado;
- [ ] Jackson configurado;
- [ ] JUnit Jupiter configurado;
- [ ] Mockito configurado;
- [ ] Maven Compiler Plugin configurado;
- [ ] Maven Surefire Plugin configurado;
- [ ] `.gitignore` configurado;
- [ ] `.editorconfig` configurado;
- [ ] README inicial criado;
- [ ] `./mvnw clean compile` funcionando;
- [ ] `./mvnw test` funcionando;
- [ ] `./mvnw package` funcionando.

---

# 45. Critério de parada

**Parar imediatamente após o bootstrap estar funcional.**

Não implementar:

- entidades;
- value objects;
- records;
- enums;
- repositories;
- services;
- payment methods;
- policies;
- exceptions de domínio;
- regras de pedidos;
- persistência JSON;
- menus;
- relatórios;
- testes de negócio.

A existência de packages destinados a esses conceitos **não autoriza sua implementação**.

Eles serão introduzidos progressivamente pelas próximas specifications.

O resultado desta etapa deve ser um projeto essencialmente vazio, porém com **arquitetura, build, dependências, documentação e ambiente preparados para evolução incremental**.