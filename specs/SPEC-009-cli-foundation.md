# SPEC-009 — CLI Inicial de Clientes e Produtos

## 1. Metadados

- **Estado:** Concluída
- **Dependências:** SPEC-003, SPEC-005 e SPEC-008 concluídas
- **Entrega:** Aplicação executável no terminal, com composição manual, menu principal e cadastro, consulta e listagem de clientes e produtos em memória

## 2. Objetivo

Introduzir a primeira interface de terminal do projeto, permitindo utilizar os casos de uso existentes em uma sessão interativa.

A CLI deve receber entradas, converter formatos, chamar os application services e apresentar resultados e erros. Regras de negócio continuam nos objetos de domínio e na camada de aplicação.

O resultado esperado é uma aplicação simples, testável e executável com Java 21, que mantenha clientes e produtos em memória durante a sessão e encerre corretamente por escolha do usuário ou fim da entrada.

## 3. Contexto

O projeto já possui `CustomerApplicationService`, `ProductApplicationService` e implementações em memória dos respectivos repositories. Ainda não existe ponto de entrada nem composição que permita utilizar esses elementos no terminal.

O fluxo desta etapa é:

```text
Main: composição manual
       │
       ▼
CLI: menu, entrada e saída
       │
       ▼
Application services
       │
       ▼
Repository interfaces
       ▲
       │
Repositories em memória
```

A composição cria os repositories e injeta cada um no service correspondente. A CLI recebe os services por construtor e reutiliza as mesmas instâncias durante toda a sessão.

Esta etapa cobre somente clientes e produtos. Pedidos, pagamentos e relatórios possuem casos de uso existentes, mas sua exposição no terminal exige incrementos próprios.

## 4. Pré-condições

- SPECS 003, 005 e 008 concluídas e validadas.
- Services de clientes e produtos e seus repositories disponíveis com os contratos atuais.
- Entidades e identificadores tipados implementados e testados.
- Projeto compilando e executando testes com Java 21 e Maven Wrapper.
- Nenhuma dependência adicional necessária.

## 5. Escopo

Esta spec autoriza:

- criar um ponto de entrada `main` no package `cli`;
- compor manualmente repositories em memória, services e interface de terminal;
- apresentar um menu principal repetidamente durante a sessão;
- cadastrar clientes e produtos;
- consultar clientes e produtos por identificador;
- listar clientes e produtos;
- converter entradas de menu, UUID e preço;
- apresentar mensagens de sucesso, ausência de registros e erros esperados;
- encerrar por opção explícita ou fim da entrada, inclusive durante uma operação;
- injetar entrada e saída para testar a CLI sem um terminal real;
- documentar o comando de execução e a perda dos dados ao encerrar;
- criar testes de entrada, navegação e fluxos integrados com services e repositories reais.

## 6. Fora do escopo

Não implementar nesta etapa:

- operações de alteração ou exclusão de clientes e produtos;
- criação, manutenção ou consulta de pedidos pelo terminal;
- pagamentos, descontos ou relatórios pelo terminal;
- persistência JSON, arquivos, banco de dados ou carga de dados de exemplo;
- alterações em domínio, application services, interfaces ou repositories existentes;
- unicidade de e-mail ou outras regras de negócio adicionais;
- argumentos de linha de comando, subcomandos ou modo batch dedicado;
- histórico de comandos, autocomplete, cores ANSI, tabelas complexas ou interface de tela inteira;
- autenticação, concorrência ou execução em background;
- frameworks de CLI ou de injeção de dependência;
- command bus, registro genérico de comandos, hierarquia de menus ou abstrações genéricas para CRUD;
- empacotamento com dependências embutidas ou alterações no `pom.xml`.

## 7. Requisitos funcionais

### RF-01 — Início da sessão e ciclo do menu

Ao iniciar, a aplicação deve informar que os dados são mantidos somente em memória e apresentar estas opções:

```text
╔══════════════════════════════════╗
║         SISTEMA DE GESTÃO        ║
╠══════════════════════════════════╣
║  CLIENTES                        ║
║  [1] Cadastrar cliente           ║
║  [3] Listar clientes             ║
║  [5] Buscar cliente por ID       ║
║                                  ║
║  PRODUTOS                        ║
║  [2] Cadastrar produto           ║
║  [4] Listar produtos             ║
║  [6] Buscar produto por ID       ║
║                                  ║
║  [0] Sair                        ║
╚══════════════════════════════════╝

Escolha uma opção:
```

Após concluir uma operação ou apresentar um erro recuperável, o menu deve ser exibido novamente. Não criar menus recursivos. Uma opção inválida deve produzir uma mensagem compreensível e manter a sessão ativa.

### RF-02 — Cadastro de cliente

A CLI deve solicitar nome e e-mail, nessa ordem, e chamar `registerCustomer` somente após receber todos os campos.

Em caso de sucesso, deve apresentar confirmação, identificador gerado, nome e e-mail do cliente retornado pelo service. Entradas inválidas segundo o domínio devem produzir mensagem de erro e retorno ao menu, sem cadastro parcial.

### RF-03 — Consulta de cliente

A CLI deve solicitar um UUID, convertê-lo em `CustomerId` e chamar `findCustomerById`.

Quando encontrado, deve apresentar identificador, nome e e-mail. Quando o formato for inválido ou o cliente não existir, deve apresentar uma mensagem correspondente e retornar ao menu.

### RF-04 — Listagem de clientes

A CLI deve chamar `listCustomers` e apresentar identificador, nome e e-mail de cada cliente na ordem devolvida pelo service. Uma lista vazia deve produzir uma mensagem explícita de ausência de clientes cadastrados.

### RF-05 — Cadastro de produto

A CLI deve solicitar nome, descrição e preço, nessa ordem. A descrição pode ser uma linha vazia, conforme as regras existentes do domínio.

O prompt de preço deve informar o uso de ponto decimal, por exemplo `100.00`. Após receber e converter todos os campos, a CLI deve chamar `registerProduct`.

Em caso de sucesso, deve apresentar confirmação, identificador gerado, nome, descrição e preço do produto retornado pelo service. Formato inválido de preço ou dados rejeitados pelo domínio devem produzir mensagem de erro e retorno ao menu, sem cadastro parcial.

### RF-06 — Consulta e listagem de produtos

A consulta deve solicitar UUID, convertê-lo em `ProductId` e chamar `findProductById`. A listagem deve chamar `listProducts`.

Os resultados devem apresentar identificador, nome, descrição e preço, respeitando a ordem devolvida pelo service. Produto ausente, UUID inválido e lista vazia devem possuir mensagens compreensíveis.

Preços devem ser apresentados com duas casas decimais e ponto decimal, sem conversão para `float` ou `double` e sem assumir uma moeda não definida pelo domínio.

### RF-07 — Conversão de formatos

Espaços antes e depois de opções, UUID e preço devem ser desconsiderados na conversão. Nome, e-mail e descrição devem ser encaminhados ao service, preservando a responsabilidade do domínio por sua normalização e validação.

O menu aceita os códigos numéricos exibidos. UUID deve usar a representação completa de 36 caracteres com grupos `8-4-4-4-12`, aceitando letras hexadecimais maiúsculas ou minúsculas.

Preço aceita texto decimal com sinal opcional, dígitos e ponto opcional seguido de dígitos. Vírgulas, separadores de milhares, símbolos monetários e notação científica não são aceitos. A conversão deve construir `BigDecimal` diretamente do texto.

Formato numérico e regra de negócio são responsabilidades distintas: valor positivo e quantidade permitida de casas decimais continuam sendo validados pelo domínio. A CLI não deve reproduzir essas regras.

Uma falha de conversão encerra a operação atual e retorna ao menu. Não é necessário implementar nova tentativa para cada campo individual.

### RF-08 — Encerramento e fim da entrada

A opção `0` deve encerrar normalmente, com uma mensagem de encerramento, sem apresentar outro menu ou ler novos campos.

Fim da entrada (EOF) deve encerrar normalmente tanto no menu quanto durante qualquer operação. Se ocorrer durante um cadastro, nenhum service de cadastro deve ser chamado com campos incompletos.

EOF deve ser distinguido de uma linha vazia. Não pode gerar repetição infinita, exception usada como fluxo normal, cadastro parcial ou mensagem de formato inválido. Uma sessão encerrada não deve continuar lendo entrada.

### RF-09 — Estado da sessão

Um cliente ou produto cadastrado deve estar disponível para consulta e listagem até o encerramento. Os repositories não podem ser recriados a cada opção do menu.

Uma nova execução começa sem dados. Não gravar nem recuperar dados de arquivos.

## 8. Requisitos técnicos

### RT-01 — Linguagem, packages e dependências

Usar Java 21 e Java Standard Library. Não adicionar dependências. Código, nomes de tipos, métodos, variáveis e testes devem usar inglês; mensagens ao usuário devem usar português.

O ponto de entrada deve ser `dev.manoelreis.ordermanagement.cli.Main`, com `public static void main(String[] args)`. Outras classes devem ficar em `cli`, `cli.menu`, `cli.input` ou `cli.output`, conforme sua responsabilidade, sem obrigatoriedade de preencher todos os subpackages.

### RT-02 — Composição e direção de dependências

`Main` é o ponto de composição autorizado a construir `InMemoryCustomerRepository`, `InMemoryProductRepository`, `CustomerApplicationService` e `ProductApplicationService`.

As classes que executam os fluxos de menu devem receber os services por construtor e utilizar seus métodos públicos. Não devem consultar repositories diretamente nem construir entidades para executar cadastros.

A CLI pode utilizar entidades e identificadores para converter entradas e apresentar resultados. `domain` e `application` não devem importar classes da CLI ou receber dependências de terminal.

### RT-03 — Entrada, saída e recursos

A lógica interativa deve receber entrada e saída por construtor, usando abstrações da JDK, como `Reader` e `Writer` ou `PrintWriter`. `System.in` e `System.out` devem ser conectados apenas no ponto de entrada.

Ler entradas por linha, evitando misturar leitura de tokens e linhas. Usar uma única fonte de leitura durante a sessão. Apresentar e descarregar prompts antes de esperar entrada quando a saída tiver buffering.

Uma linha ausente pode ser representada por `Optional.empty()` ou pelo resultado de EOF da API da JDK; uma linha vazia continua sendo uma entrada presente. Não criar uma exception para representar EOF.

Não fechar `System.in` ou `System.out` nas classes de menu e entrada/saída. Nos testes, o chamador mantém a propriedade dos recursos injetados.

### RT-04 — Simplicidade e encapsulamento

Usar um loop explícito e despacho simples de opções. Separar conversão e apresentação quando melhorar a clareza, sem criar interfaces apenas para permitir mocks.

Não usar estado global mutável, singleton, reflexão, descoberta automática de comandos ou abstrações compartilhadas com persistência.

### RT-05 — Execução pelo artefato existente

Após `./mvnw clean package`, a aplicação deve ser executável com:

```bash
java -cp target/order-management-cli-0.1.0-SNAPSHOT.jar dev.manoelreis.ordermanagement.cli.Main
```

Não é necessário tornar o JAR executável por `java -jar`, adicionar plugin de execução ou incluir dependências no artefato. O caminho documentado deve acompanhar a versão atual do projeto.

## 9. Modelo e contratos

### 9.1 Ponto de composição

`Main` cria as dependências uma vez, conecta entrada e saída do processo e inicia a sessão. Não deve concentrar os fluxos de cadastro e consulta.

### 9.2 Sessão interativa

Uma classe concreta deve coordenar o menu e oferecer uma operação de início, como `run()`, que termina ao receber `0` ou EOF. Seus services e recursos de entrada/saída são obrigatórios e recebidos por construtor; argumentos nulos devem produzir `IllegalArgumentException` antes de iniciar a sessão.

O nome da classe e a divisão em colaboradores específicos podem ser definidos na implementação, preservando simplicidade, responsabilidades e testabilidade.

### 9.3 Contratos utilizados

```text
CustomerApplicationService
├── registerCustomer(name, email)
├── findCustomerById(customerId)
└── listCustomers()

ProductApplicationService
├── registerProduct(name, description, price)
├── findProductById(productId)
└── listProducts()
```

Identificadores gerados pertencem aos services. A CLI não solicita ID no cadastro nem altera entidades retornadas para aplicar regras de negócio.

### 9.4 Organização implementada

A implementação utiliza os subpackages previamente existentes da CLI:

```text
cli/
├── Main.java
├── input/
│   └── TerminalInput.java
├── menu/
│   └── TerminalSession.java
└── output/
    └── TerminalOutput.java
```

- `Main` compõe repositories e services uma vez, conecta os recursos do processo e inicia a sessão.
- `input.TerminalInput` mantém uma única fonte de leitura por linha e converte UUID completo e preço decimal. Conversões inválidas retornam `Optional.empty()`; EOF é representado por `null` em `readLine()`, distinguindo-se de uma linha vazia. Falhas de leitura propagam `IOException`.
- `menu.TerminalSession` recebe services, `Reader` e `Writer` por construtor, coordena o loop e os fluxos, delega leitura e apresentação e trata erros esperados. A normalização e as regras de negócio continuam nos services e no domínio.
- `output.TerminalOutput` apresenta menu, mensagens e campos dos registros, descarrega a saída antes da leitura e propaga falhas de escrita. Os preços são apresentados diretamente a partir de `BigDecimal`, com duas casas decimais e ponto decimal.

Os colaboradores não fecham os recursos injetados. Os testes da sessão permanecem em `src/test/java/dev/manoelreis/ordermanagement/cli/TerminalSessionTest.java` e exercitam essa divisão com services e repositories reais.

## 10. Tratamento de erros

- Opção desconhecida, entrada vazia em opção, UUID malformado e preço malformado são erros de entrada recuperáveis.
- `InvalidCustomerException` e `InvalidProductException` devem ser apresentados como erros de dados, preservando a sessão.
- `CustomerNotFoundException` e `ProductNotFoundException` devem produzir mensagem de registro não encontrado, preservando a sessão.
- Não apresentar stack traces para esses erros esperados.
- Capturar erros de conversão perto da conversão; não envolver toda a sessão em um `catch (Exception)` ou converter falhas inesperadas em erros de digitação.
- EOF é encerramento normal e deve ser tratado explicitamente.
- Falha real de leitura ou escrita deve interromper a sessão e ser distinguida de EOF e de entrada inválida. Pode ser propagada até o ponto de entrada para apresentação de erro de I/O; não retentar indefinidamente.
- Não criar exceptions de domínio ou infraestrutura para atender a erros de terminal.
- As mensagens devem indicar o problema e os resultados devem identificar os registros; não é necessário fixar o texto literal completo das mensagens nesta spec.

## 11. Arquivos afetados

A implementação desta spec poderá criar ou alterar somente:

```text
src/main/java/dev/manoelreis/ordermanagement/cli/
src/test/java/dev/manoelreis/ordermanagement/cli/
README.md
specs/SPEC-009-cli-foundation.md
specs/README.md
```

A criação desta specification autoriza criar este documento e atualizar seu índice. Não autoriza implementar a CLI nesta mesma tarefa de documentação.

Não são necessárias mudanças em `domain`, `application`, `infrastructure`, `shared`, `pom.xml` ou Maven Wrapper.

## 12. Estratégia de testes

Usar JUnit Jupiter, entrada em memória como `StringReader` e saída capturada como `StringWriter`. Preferir services, entidades e repositories reais. Cada teste deve compor uma sessão nova com repositories novos.

Cobrir, no mínimo:

- exibição do menu, saída por `0` e ausência de novas leituras após encerramento;
- EOF no menu e em cada etapa dos cadastros, sem armazenamento parcial;
- distinção entre EOF e linha vazia, incluindo descrição vazia válida;
- opção inválida seguida de opção válida, demonstrando continuidade da sessão;
- cadastro de cliente e posterior consulta/listagem na mesma sessão;
- cadastro de produto e posterior consulta/listagem na mesma sessão;
- apresentação dos IDs gerados e dos campos retornados pelos services;
- listagens vazias e ordem de múltiplos registros;
- UUID completo válido, inclusive com maiúsculas e espaços externos, e formatos inválidos, incluindo UUID abreviado;
- consultas com ID válido ausente, seguidas de outra operação válida;
- preço decimal válido e apresentação com duas casas;
- rejeição de vírgula, símbolos monetários e notação científica;
- preço não positivo e precisão monetária inválida rejeitados pelo domínio;
- dados inválidos de cliente/produto, sem alteração do armazenamento e com retorno ao menu;
- isolamento de dados entre sessões compostas com repositories diferentes;
- rejeição de dependências obrigatórias nulas;
- falha de leitura usando um `Reader` controlado, distinta de EOF e sem loop infinito.

Para consultas de registros criados durante a execução, testar sessões com registros previamente cadastrados pelos services ou executar cadastro e consulta em sessões consecutivas que compartilhem deliberadamente os mesmos services. Não fixar UUIDs gerados aleatoriamente nem criar geradores de ID no código de produção para facilitar testes.

Asserções devem verificar comportamento, dados e continuidade, evitando depender do alinhamento, espaçamento ou texto completo de toda a saída. Não substituir globalmente `System.in` ou `System.out` para testar a lógica interativa.

Executar `./mvnw test` e `./mvnw clean package`. Realizar um smoke test do comando de execução com entrada controlada, cobrindo cadastro, listagem e saída, e registrar o resultado.

## 13. Critérios de aceite

- **CA-01:** o comando documentado inicia a aplicação após o build.
- **CA-02:** o menu oferece exatamente as operações desta etapa e saída explícita.
- **CA-03:** clientes podem ser cadastrados, consultados e listados durante a sessão.
- **CA-04:** produtos podem ser cadastrados, consultados e listados durante a sessão, com descrição vazia válida e preços em `BigDecimal`.
- **CA-05:** formatos inválidos, dados rejeitados pelo domínio e registros ausentes são apresentados sem encerrar a sessão.
- **CA-06:** `0` e EOF encerram normalmente, inclusive com EOF durante operações, sem cadastros parciais ou loops infinitos.
- **CA-07:** a composição é manual e os mesmos repositories são reutilizados durante a sessão.
- **CA-08:** os fluxos utilizam services existentes, sem regras de negócio duplicadas ou alterações nas camadas centrais.
- **CA-09:** entrada e saída são injetáveis e os testes usam recursos em memória.
- **CA-10:** nenhuma dependência, persistência durável ou operação fora do escopo é introduzida.
- **CA-11:** testes, smoke test e `./mvnw clean package` passam.
- **CA-12:** README documenta execução, operações disponíveis e armazenamento transitório.

## 14. Definition of Done

- [x] Requisitos funcionais atendidos
- [x] Requisitos técnicos atendidos
- [x] Ponto de entrada e composição manual implementados
- [x] Fluxos de clientes e produtos implementados e testados
- [x] Entradas inválidas e erros esperados tratados
- [x] Encerramento por opção e EOF verificado
- [x] Testes relevantes implementados e aprovados
- [x] Build aprovado com `./mvnw clean package`
- [x] Smoke test do comando de execução aprovado
- [x] Nenhum item fora do escopo implementado
- [x] Documentação afetada atualizada
- [x] Estado da spec alterado para `Concluída` somente após a validação

### Validação realizada

A validação abaixo foi repetida após a reorganização em `input`, `menu` e `output`, com os mesmos resultados aprovados.

- `./mvnw test`: aprovado, 178 testes, sem falhas ou erros (61 casos da CLI).
- `./mvnw clean package`: aprovado com Java 21; JAR gerado no caminho documentado.
- Smoke test com `java -cp target/order-management-cli-0.1.0-SNAPSHOT.jar dev.manoelreis.ordermanagement.cli.Main`: aprovado com entrada controlada para cadastro de cliente, cadastro de produto com descrição vazia e preço `100.50`, listagem de ambos e saída por `0`. Os registros foram apresentados no cadastro e na listagem.
- Smoke tests adicionais: EOF no menu, EOF durante cadastro sem registro parcial e nova execução com listagens vazias; todos encerraram normalmente, sem erros em stderr.
- Implementação limitada à CLI, seus testes e documentação autorizada, sem alterações nas camadas centrais ou dependências.

## 15. Critério de parada

Parar quando a aplicação puder ser iniciada pelo comando documentado e permitir cadastrar, consultar e listar clientes e produtos em memória, mantendo a sessão após erros esperados e encerrando corretamente por `0` ou EOF, com testes e build aprovados.

Não iniciar edição de registros, fluxos de pedidos, pagamentos, relatórios, persistência JSON ou recursos avançados de terminal. Essas evoluções exigem specifications próprias.
