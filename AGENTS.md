# Order Management CLI — Project Context

## Project Overview

**Order Management CLI** é um projeto educacional desenvolvido em Java puro para construir um sistema de gestão de pedidos executado através de linha de comando.

O principal objetivo não é desenvolver um produto comercial nem construir um backend web.

O objetivo é desenvolver domínio real sobre **Java e seu ecossistema fundamental antes da introdução de Spring**.

A pergunta que orienta o projeto é:

> **"Eu sei Java ou simplesmente sei Spring?"**

Por esse motivo, mecanismos fundamentais não devem ser substituídos prematuramente por frameworks.

---

# Learning Goals

O projeto deve proporcionar experiência prática com:

```text
Java
├── Classes e Objects
├── Encapsulation
├── Interfaces
├── Polymorphism
├── Inheritance when justified
├── Records
├── Enums
├── Exceptions
├── Generics
├── Collections
│   ├── List
│   ├── Set
│   └── Map
├── Optional
├── Lambdas
├── Stream API
├── Date/Time API
├── File I/O
├── Maven
├── JUnit
└── Mockito
```

O aprendizado desses recursos possui prioridade sobre conveniência.

---

# Technology Stack

Stack principal:

```text
Java 21 LTS
Maven
Jackson
JUnit Jupiter
Mockito
Git
```

Priorizar sempre:

> Java Standard Library antes de dependências externas.

---

# Explicitly Forbidden Technologies

Não adicionar sem uma futura decisão explícita:

```text
Spring
Spring Boot
Spring Data
Spring IoC
Hibernate
JPA
Jakarta Persistence
Lombok
MapStruct
Dependency Injection frameworks
Web frameworks
ORM
Database
Docker
Messaging systems
```

Spring é deliberadamente proibido neste projeto.

---

# Project Scope

O sistema deverá evoluir para permitir:

* cadastro de produtos;
* cadastro de clientes;
* criação de pedidos;
* adição e remoção de itens;
* cálculo de subtotal;
* aplicação de descontos;
* cálculo de total;
* gerenciamento de status;
* processamento simulado de pagamentos;
* busca de pedidos;
* geração de relatórios;
* persistência em arquivos;
* processamento de coleções através de Streams.

A aplicação será executada via CLI.

---

# Domain Overview

Modelo conceitual principal:

```text
Customer
   │
   └── Order
        ├── OrderItem
        │    └── Product
        │
        ├── Payment
        │    └── PaymentMethod
        │
        └── OrderStatus
```

Possíveis abstrações adicionais:

```text
DiscountPolicy
PaymentResult
Repositories
Domain Exceptions
Reports
```

Esses elementos devem ser introduzidos somente pelas specifications correspondentes.

---

# Architecture

A aplicação é organizada em quatro áreas principais:

```text
CLI
 │
 ▼
Application
 │
 ▼
Domain
 ▲
 │
Infrastructure
```

Packages principais:

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

# Domain

`domain` representa os conceitos e regras fundamentais do negócio.

Estrutura planejada:

```text
domain/
├── customer/
├── product/
├── order/
├── payment/
├── discount/
└── exception/
```

O domínio deve possuir o mínimo possível de dependências externas.

Especialmente:

> Domain must not depend on Infrastructure.

O domínio não deve conhecer:

* terminal;
* arquivos;
* JSON;
* Jackson;
* detalhes de persistência.

---

# Application

`application` coordena casos de uso.

Estrutura:

```text
application/
├── customer/
├── product/
├── order/
├── payment/
└── report/
```

Application poderá coordenar objetos de domínio e abstrações de persistência.

Application não deve conter detalhes de terminal ou serialização.

---

# Infrastructure

Responsável por detalhes técnicos externos ao domínio.

Estrutura inicial:

```text
infrastructure/
└── persistence/
    ├── memory/
    └── json/
```

Persistência deverá inicialmente evoluir de:

```text
In Memory
    ↓
JSON Files
```

Não introduzir banco de dados neste projeto sem uma mudança explícita de escopo.

---

# CLI

Responsável pela interface com o usuário.

```text
cli/
├── menu/
├── input/
└── output/
```

A CLI pode:

* receber input;
* validar formato de entrada;
* apresentar menus;
* apresentar resultados;
* apresentar erros.

A CLI **não pode conter regras de negócio**.

---

# Shared

`shared` deve permanecer pequeno.

Não utilizar como depósito genérico.

Evitar:

```text
utils
helpers
commons
misc
```

Sempre preferir colocar uma classe próxima ao contexto ao qual ela pertence.

---

# Dependency Direction

Dependências devem apontar para conceitos mais centrais.

Preferir:

```text
CLI → Application → Domain
Infrastructure → abstractions/core
```

Evitar:

```text
Domain → Infrastructure
Domain → CLI
Application → CLI
Domain → Jackson
```

---

# Design Principles

## Encapsulation

Objetos devem proteger seu próprio estado.

Evitar objetos compostos exclusivamente por getters e setters.

Preferir comportamentos semanticamente relevantes.

Exemplo conceitual:

```text
order.cancel()
```

em vez de exposição irrestrita do estado interno.

---

# Interfaces

Criar interfaces somente quando representarem uma abstração real.

Exemplos conceitualmente válidos:

```text
PaymentMethod
DiscountPolicy
Repository
```

Não criar automaticamente:

```text
ProductInterface
CustomerInterface
OrderInterface
```

Interfaces não existem apenas para aumentar abstração.

---

# Inheritance

Herança não é requisito obrigatório.

Utilizá-la somente quando existir uma relação comportamental justificável.

Preferir composição quando apropriado.

Não criar hierarquias artificiais apenas para demonstrar conhecimento de herança.

---

# Records

Records devem ser considerados para estruturas:

* imutáveis;
* orientadas a dados;
* sem identidade própria;
* utilizadas para transportar resultados ou valores.

Não transformar todas as classes em records.

---

# Collections

Escolher Collections semanticamente.

```text
List
```

Quando ordem e múltiplos elementos forem relevantes.

```text
Set
```

Quando duplicatas não fizerem sentido.

```text
Map
```

Quando associação chave → valor ou lookup eficiente forem necessários.

Não utilizar `List` para tudo.

---

# Optional

Utilizar `Optional` principalmente quando uma operação legitimamente pode não produzir resultado.

Exemplo conceitual:

```text
findById → Optional<T>
```

Não utilizar `Optional` indiscriminadamente como atributo de todas as classes.

---

# Money

Valores monetários devem utilizar:

```text
BigDecimal
```

Nunca utilizar:

```text
float
double
```

para valores monetários do domínio.

---

# Date and Time

Utilizar APIs modernas:

```text
java.time
```

Preferir:

```text
LocalDate
LocalDateTime
Duration
```

quando semanticamente apropriado.

Evitar APIs legadas como:

```text
Date
Calendar
```

---

# Streams

Stream API deve ser utilizada quando tornar operações sobre coleções mais declarativas.

Especialmente:

* filtering;
* mapping;
* aggregation;
* grouping;
* sorting;
* reporting.

Não substituir automaticamente todos os loops por Streams.

Utilizar a construção mais legível para cada situação.

---

# Exceptions

Exceptions devem representar situações excepcionais relevantes.

Não utilizar exceptions como fluxo normal de controle.

Domain exceptions devem permanecer próximas ao domínio.

---

# Dependency Injection

Dependency Injection deverá inicialmente ser feita manualmente.

Isso é deliberado.

O projeto deve permitir compreender:

* constructor injection;
* inversão de dependência;
* abstrações;
* composição de objetos.

Não adicionar container de DI.

---

# Persistence

A evolução esperada é:

```text
Repository abstraction
        │
        ├── InMemory implementation
        │
        └── JSON implementation
```

Detalhes de persistência não devem vazar para o domínio.

---

# JSON

Jackson será utilizado exclusivamente como mecanismo técnico de serialização/deserialização.

Evitar acoplamento do domínio ao Jackson.

JSON pertence à infraestrutura.

---

# Testing

Framework principal:

```text
JUnit Jupiter
```

Mocks:

```text
Mockito
```

Prioridade:

> Use objetos reais quando forem simples de construir.

Mockito deve ser utilizado principalmente para isolar dependências externas ou fronteiras relevantes.

Evitar testes compostos inteiramente por mocks.

---

# Maven

Maven é responsável por:

* dependency management;
* compilation;
* tests;
* packaging.

Comandos básicos devem permanecer funcionais:

```bash
./mvnw compile
./mvnw test
./mvnw package
./mvnw clean package
```

---

# Specifications

Todas as specifications ficam em:

```text
/specs
```

Convenção:

```text
SPEC-XXX-description.md
```

Exemplo:

```text
SPEC-001-project-bootstrap.md
SPEC-002-domain-model.md
SPEC-003-repositories.md
```

As specifications representam incrementos planejados do sistema.

---

# Working With Specs

Antes de implementar uma tarefa:

1. Leia este `AGENTS.md`.
2. Identifique a SPEC solicitada.
3. Leia a SPEC completa.
4. Inspecione o código existente relacionado.
5. Implemente somente o escopo autorizado.
6. Execute os testes.
7. Execute o build.
8. Pare quando os critérios da SPEC forem satisfeitos.

Uma SPEC não autoriza implementar antecipadamente a próxima.

---

# Scope Discipline

Não implementar funcionalidades "porque serão necessárias depois".

Exemplo:

Se uma SPEC solicita somente modelagem de `Product`, não implementar antecipadamente:

```text
ProductRepository
ProductService
ProductController
Product JSON persistence
```

Mesmo que essas funcionalidades provavelmente apareçam posteriormente.

Implementar somente o necessário para cumprir a SPEC atual.

---

# YAGNI

Aplicar YAGNI rigorosamente.

> You Aren't Gonna Need It.

Não criar infraestrutura para requisitos hipotéticos.

---

# KISS

Preferir a solução mais simples que preserve corretamente:

* domínio;
* legibilidade;
* testabilidade;
* encapsulamento.

Evitar complexidade arquitetural sem benefício concreto.

---

# SOLID

Utilizar SOLID como guia, não como checklist mecânico.

Não criar abstrações artificiais apenas para afirmar que um princípio foi aplicado.

O design deve permanecer compreensível para alguém aprendendo Java.

---

# No Overengineering

Não introduzir prematuramente:

```text
Clean Architecture completa
Hexagonal Architecture completa
Onion Architecture
CQRS
Event Sourcing
Command Bus
Query Bus
Mediator
Event Bus
Factories genéricas
Generic Services
Generic Mappers
BaseEntity
AbstractService
```

Essas técnicas só devem aparecer caso um problema real justifique sua existência.

---

# No Lombok

Não utilizar Lombok.

Parte do objetivo do projeto é compreender diretamente como Java funciona.

Não esconder:

* constructors;
* encapsulation;
* object initialization;
* equals/hashCode;
* records;
* immutability;

atrás de geração automática de código enquanto esses conceitos estão sendo estudados.

---

# Code Quality

Código deve priorizar:

1. clareza;
2. responsabilidade bem definida;
3. encapsulamento;
4. nomes explícitos;
5. baixo acoplamento;
6. testes;
7. simplicidade.

Evitar comentários explicando código ruim.

Preferir código cujo propósito seja compreensível pelos nomes e estrutura.

---

# Language

Código deve utilizar inglês.

Isso inclui:

* classes;
* methods;
* variables;
* packages;
* exceptions;
* tests;
* commit-related technical terminology.

Documentação educacional e specifications podem ser escritas em português.

---

# Git

Não versionar:

* build artifacts;
* arquivos temporários;
* runtime data;
* logs;
* configurações específicas de IDE.

Versionar:

```text
src/
specs/
docs/
pom.xml
.mvn/
mvnw
mvnw.cmd
CLAUDE.md
README.md
```

---

# Project Structure

Estrutura esperada:

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
│   │   │   ├── application/
│   │   │   ├── infrastructure/
│   │   │   ├── cli/
│   │   │   └── shared/
│   │   └── resources/
│   │
│   └── test/
│       ├── java/dev/manoelreis/ordermanagement/
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

# Core Rule

Quando existir dúvida entre:

> "criar uma abstração agora"

e

> "esperar existir uma necessidade concreta"

prefira **esperar**.

Quando existir dúvida entre:

> "usar uma biblioteca"

e

> "aprender o mecanismo correspondente da JDK"

prefira **a JDK**, desde que ela resolva adequadamente o problema.

Quando existir dúvida entre:

> "implementar algo da próxima SPEC"

e

> "parar"

**pare.**

O objetivo deste projeto não é chegar rapidamente ao Spring.

O objetivo é compreender profundamente o que Spring futuramente fará por nós.
