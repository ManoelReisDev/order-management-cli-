# Order Management CLI

Projeto educacional em Java puro para construir, gradualmente, um sistema de gestão de pedidos pela linha de comando. O objetivo é praticar a linguagem, a JDK, Maven e testes antes de introduzir frameworks. **Spring não é utilizado.**

## Estado atual

As SPEC-001 a SPEC-009 estão concluídas. O projeto possui o bootstrap Maven, o modelo inicial de clientes, produtos e pedidos, os casos de uso de clientes e produtos, a manutenção de pedidos em rascunho e o processamento simulado de pagamentos aprovados ou recusados. Pedidos aprovados passam ao estado pago e deixam de aceitar alterações em seus itens. A camada de aplicação também oferece um relatório de vendas por período de criação, com contagens, receita de pedidos pagos e vendas agrupadas por cliente. A interface de terminal permite cadastrar, buscar por ID e listar clientes e produtos.

Os repositories de clientes, produtos e pedidos possuem implementações em `infrastructure.persistence.memory`, compatíveis com injeção manual nos application services. Cada instância armazena referências por identificador e preserva a ordem de primeira inserção, inclusive ao substituir uma entidade. As listagens são snapshots estruturalmente não modificáveis, e os dados existem apenas em memória durante a execução, sem persistência em arquivos.

## Stack e requisitos

- Java 21 LTS
- Maven 3.9.16, fornecido pelo Maven Wrapper
- Jackson para futura persistência JSON
- JUnit Jupiter e Mockito para testes

Não é necessário instalar Maven globalmente. É necessário ter um JDK 21 disponível em `JAVA_HOME` ou no `PATH`.

## Build e testes

```bash
./mvnw clean package
./mvnw test
```

O primeiro uso do Wrapper baixa o Maven. `package` produz um JAR com o ponto de entrada `dev.manoelreis.ordermanagement.cli.Main`, sem dependências embutidas.

## Execução no terminal

Após `./mvnw clean package`, execute com Java 21:

```bash
java -cp target/order-management-cli-0.1.0-SNAPSHOT.jar dev.manoelreis.ordermanagement.cli.Main
```

O menu oferece cadastro de clientes (`1`) e produtos (`2`), listagem de clientes (`3`) e produtos (`4`), busca de cliente (`5`) e produto (`6`) por UUID completo e saída (`0`). No cadastro de produto, a descrição pode ser vazia e o preço deve usar ponto decimal, como `100.00`, sem símbolo monetário ou notação científica. Erros de entrada ou dados inválidos são apresentados e devolvem a sessão ao menu.

Os dados ficam somente em memória durante a sessão e são perdidos ao encerrar. Cada execução começa sem registros. A opção `0` ou o fim da entrada (EOF) encerra a aplicação; um cadastro interrompido por EOF não é armazenado. Pedidos, pagamentos e relatórios ainda não estão disponíveis na CLI.

## Organização

O código ficará sob `dev.manoelreis.ordermanagement`. `domain` conterá conceitos de negócio; `application`, os casos de uso; `infrastructure`, os detalhes técnicos; `cli`, a interface de terminal; e `shared` será mantido mínimo. A direção prevista das dependências é `cli → application → domain`, enquanto infraestrutura depende de abstrações centrais. O domínio não depende de Jackson, arquivos ou CLI.

Na CLI, `Main` compõe as dependências, `input.TerminalInput` lê linhas e converte UUID e preço, `menu.TerminalSession` coordena os fluxos e `output.TerminalOutput` apresenta o menu e os registros.

As especificações ficam em [`specs/`](specs/README.md), a documentação complementar em `docs/architecture/` e os futuros arquivos de runtime em `data/`.
