# Order Management CLI

Projeto educacional em Java puro para construir, gradualmente, um sistema de gestão de pedidos pela linha de comando. O objetivo é praticar a linguagem, a JDK, Maven e testes antes de introduzir frameworks. **Spring não é utilizado.**

## Estado atual

A SPEC-001 prepara somente a estrutura, as dependências e o build. Ainda não há aplicação executável, regras de negócio ou testes de negócio.

## Stack e requisitos

- Java 21 LTS
- Maven 3.9.16, fornecido pelo Maven Wrapper
- Jackson para futura persistência JSON
- JUnit Jupiter e Mockito para testes futuros

Não é necessário instalar Maven globalmente. É necessário ter um JDK 21 disponível em `JAVA_HOME` ou no `PATH`.

## Build e testes

```bash
./mvnw clean package
./mvnw test
```

O primeiro uso do Wrapper baixa o Maven. Nesta etapa, `package` produz apenas um JAR sem ponto de entrada.

## Organização

O código ficará sob `dev.manoelreis.ordermanagement`. `domain` conterá conceitos de negócio; `application`, os casos de uso; `infrastructure`, os detalhes técnicos; `cli`, a interface de terminal; e `shared` será mantido mínimo. A direção prevista das dependências é `cli → application → domain`, enquanto infraestrutura depende de abstrações centrais. O domínio não depende de Jackson, arquivos ou CLI.

As especificações ficam em [`specs/`](specs/README.md), a documentação complementar em `docs/architecture/` e os futuros arquivos de runtime em `data/`.
