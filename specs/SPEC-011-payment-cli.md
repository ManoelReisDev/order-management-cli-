# SPEC-011 — Pagamentos na CLI

## 1. Metadados

- **Estado:** Concluída
- **Dependências:** SPEC-006, SPEC-008 e SPEC-010 concluídas
- **Entrega:** Fluxo de terminal para processar um pagamento simulado como aprovado ou recusado por meio de `PaymentApplicationService`

## 2. Objetivo

Permitir que a interface de terminal processe o pagamento de um pedido existente utilizando o caso de uso definido na SPEC-006. O usuário deve informar o pedido e escolher de forma explícita o resultado da simulação, podendo observar aprovação ou recusa sem integração financeira real.

O resultado esperado é uma nova operação na sessão existente, composta manualmente com o mesmo repository de pedidos dos demais fluxos. A CLI deve apenas coletar e apresentar dados, enquanto `PaymentApplicationService` e `Order` continuam responsáveis pela coordenação e pelas regras de pagamento.

## 3. Contexto

A SPEC-006 introduziu `PaymentApplicationService`, `PaymentMethod` e `PaymentResult`, mas deixou a escolha da estratégia de pagamento a cargo do chamador. A SPEC-008 forneceu `InMemoryOrderRepository`, e a SPEC-010 tornou pedidos e seus itens acessíveis pelo terminal.

Nesta etapa, a CLI será o chamador do caso de uso e fornecerá uma estratégia determinística conforme a escolha do usuário:

```text
Terminal
   │
   ├── ID do pedido
   └── resultado simulado: aprovar ou recusar
             │
             ▼
PaymentApplicationService.processPayment(orderId, paymentMethod)
             │
             ▼
          Order.pay(paymentMethod)
```

A escolha não representa um meio de pagamento real. Ela existe somente para permitir que os dois resultados previstos pelo domínio sejam exercitados de forma observável e reproduzível na CLI.

## 4. Pré-condições

- SPEC-006, SPEC-008 e SPEC-010 concluídas.
- `PaymentApplicationService`, `PaymentMethod` e `PaymentResult` implementados e testados.
- `InMemoryOrderRepository` utilizado pela composição manual da aplicação.
- CLI capaz de criar, consultar e editar pedidos durante uma mesma sessão.
- Projeto compilando e com testes executáveis via Maven Wrapper e Java 21.

## 5. Escopo

Esta spec autoriza:

- adicionar ao menu a opção `13` para processar pagamento;
- solicitar o UUID completo do pedido;
- solicitar uma escolha entre aprovação e recusa simuladas;
- criar, na fronteira da CLI, um `PaymentMethod` determinístico correspondente à escolha;
- chamar `PaymentApplicationService.processPayment` e apresentar o resultado;
- compor `PaymentApplicationService` em `Main` com a mesma instância de `InMemoryOrderRepository` utilizada por `OrderApplicationService`;
- tratar erros esperados sem encerrar a sessão;
- preservar encerramento seguro por EOF em qualquer etapa do novo fluxo;
- atualizar os testes e a documentação afetada ao concluir a implementação.

## 6. Fora do escopo

Não implementar nesta etapa:

- gateways, APIs financeiras, rede, credenciais ou cobrança real;
- métodos concretos de PIX, cartão, boleto ou dinheiro;
- captura de dados bancários, cartão, parcelas, bandeira ou chave PIX;
- escolha aleatória de aprovação ou recusa;
- taxas, descontos, frete, reembolso, estorno ou cancelamento;
- histórico, comprovante, identificador ou timestamp de pagamento;
- cadastro ou repository de pagamentos;
- novas transições de `OrderStatus` ou alterações nas regras de `Order.pay`;
- mudanças nos contratos de `PaymentApplicationService`, `PaymentMethod` ou `PaymentResult`;
- relatórios na CLI;
- persistência JSON ou em banco de dados;
- retentativas automáticas, concorrência ou mecanismos transacionais;
- interfaces, factories ou abstrações genéricas criadas somente para a CLI;
- dependências de produção adicionais, Spring ou qualquer tecnologia proibida pelo `AGENTS.md`.

## 7. Requisitos funcionais

### RF-01 — Operação de pagamento

O menu deve oferecer a opção `13` com uma descrição clara de processamento de pagamento. Ao selecioná-la, a sessão deve solicitar primeiro o UUID completo do pedido e depois o resultado da simulação.

As escolhas aceitas para a simulação devem ser:

```text
1 -> aprovar
2 -> recusar
```

Espaços externos devem ser ignorados. Qualquer outro conteúdo é inválido.

### RF-02 — Aprovação simulada

Quando o usuário escolher aprovação, a CLI deve fornecer ao service um `PaymentMethod` que retorne `PaymentResult.APPROVED`. Para um pedido elegível, deve apresentar que o pagamento foi aprovado. O pedido deve permanecer disponível na sessão com status `PAID`, conforme as regras existentes.

### RF-03 — Recusa simulada

Quando o usuário escolher recusa, a CLI deve fornecer ao service um `PaymentMethod` que retorne `PaymentResult.DECLINED`. Deve apresentar que o pagamento foi recusado. O pedido deve permanecer em `DRAFT`, sem alteração de itens ou subtotal, e continuar disponível na sessão.

### RF-04 — Pedido inexistente ou inelegível

Se o pedido não existir, a CLI deve informar o identificador não encontrado e retornar ao menu.

Se o domínio rejeitar a tentativa porque o pedido está vazio ou já está pago, a CLI deve apresentar uma mensagem compreensível de operação rejeitada e retornar ao menu. A CLI não deve antecipar essas validações consultando status ou itens antes de chamar o service.

### RF-05 — Entradas inválidas

UUID malformado deve encerrar somente a operação atual, apresentar a mensagem de formato já utilizada pela CLI e retornar ao menu sem solicitar o resultado simulado.

Escolha de resultado diferente de `1` ou `2`, inclusive linha vazia, deve apresentar mensagem de escolha inválida, retornar ao menu e não chamar o service.

Não implementar novas tentativas por campo dentro da mesma operação.

### RF-06 — EOF e continuidade da sessão

EOF ao aguardar o ID ou o resultado simulado deve encerrar normalmente a sessão, sem tentativa de pagamento e sem mensagem de entrada inválida. Aprovações, recusas e erros esperados devem devolver o controle ao menu.

O pagamento deve afetar o mesmo pedido acessível pelas operações de consulta, listagem e edição durante a sessão. Uma aprovação deve impedir edições posteriores pelos comportamentos já existentes do domínio; uma recusa não deve impedir uma nova tentativa ou edição válida.

## 8. Requisitos técnicos

### RT-01 — Composição manual e identidade do repository

`Main` deve construir `PaymentApplicationService` com a mesma instância de `InMemoryOrderRepository` já injetada em `OrderApplicationService`.

`TerminalSession` deve receber `PaymentApplicationService` por construtor, rejeitando dependência nula com `IllegalArgumentException`. Atualizar todos os chamadores e testes; não é necessário manter construtor alternativo sem o serviço de pagamentos.

### RT-02 — Delegação às camadas existentes

A CLI deve chamar:

```text
PaymentApplicationService.processPayment(OrderId orderId, PaymentMethod method)
```

Ela não deve acessar repositories diretamente, chamar `Order.pay`, alterar status, calcular subtotal nem salvar pedidos. Elegibilidade, transição e decisão de armazenamento permanecem no domínio e no application service existentes.

### RT-03 — Estratégia simulada

A escolha `1` deve produzir uma implementação de `PaymentMethod` que sempre retorne `APPROVED`; a escolha `2`, uma implementação que sempre retorne `DECLINED`. Lambdas podem ser utilizadas porque `PaymentMethod` é uma interface funcional.

Essas estratégias pertencem à fronteira da CLI e não justificam novas classes no domínio, na aplicação ou na infraestrutura. O valor recebido por `PaymentMethod.process` não deve ser recalculado, substituído ou apresentado como entrada pelo terminal.

### RT-04 — Entrada e saída

Reutilizar `TerminalInput` e `TerminalOutput`, mantendo leitura por linha, uma única fonte de entrada, descarga dos prompts e propriedade dos recursos pelo chamador. Não fechar `System.in` ou `System.out`.

Mensagens devem distinguir aprovação, recusa, pedido inexistente, escolha inválida e rejeição pelo estado do pedido. Erros esperados não devem apresentar stack trace.

### RT-05 — Simplicidade, linguagem e dependências

Usar Java 21 e as dependências existentes. Código e testes devem permanecer em inglês; mensagens do terminal, em português. Não adicionar dependências, interfaces para mocks ou infraestrutura de pagamento.

### RT-06 — Execução

Preservar o comando existente após `./mvnw clean package`:

```bash
java -cp target/order-management-cli-0.1.0-SNAPSHOT.jar dev.manoelreis.ordermanagement.cli.Main
```

## 9. Modelo e contratos

Contratos públicos utilizados, sem alterações:

```text
PaymentApplicationService
└── processPayment(orderId, paymentMethod) -> PaymentResult

PaymentMethod
└── process(amount) -> PaymentResult

PaymentResult
├── APPROVED
└── DECLINED
```

`Main` mantém a raiz de composição. `TerminalSession` coleta o pedido e a decisão da simulação, cria a estratégia correspondente e interpreta o resultado retornado. `TerminalInput` continua responsável por converter o UUID, e `TerminalOutput` apresenta a nova opção, os prompts e os resultados.

`PaymentApplicationService` continua responsável por localizar o pedido e salvar somente uma aprovação. `Order` continua responsável por validar elegibilidade, fornecer o subtotal ao método e alterar o status. Nenhum estado paralelo de pagamentos deve ser mantido pela CLI.

## 10. Tratamento de erros

- `PaymentApplicationService` nulo no construtor de `TerminalSession` deve produzir `IllegalArgumentException` antes do início da sessão;
- UUID malformado deve apresentar a mensagem de formato existente, sem chamar o service;
- escolha de simulação inválida deve apresentar uma mensagem específica, sem chamar o service;
- `OrderNotFoundException` deve informar que o pedido não foi encontrado, incluindo o UUID procurado;
- `DomainException` para pedido vazio ou já pago deve resultar em mensagem de operação rejeitada, sem encerrar a sessão;
- recusa é resultado esperado e deve ser apresentada sem exception;
- EOF não é erro e deve encerrar normalmente sem tentativa parcial;
- falhas reais de entrada ou saída continuam propagando `IOException` até o ponto de entrada;
- exceptions técnicas inesperadas do processamento não devem ser capturadas genericamente nem apresentadas como recusa;
- não criar novas exceptions para atender a esta spec.

## 11. Arquivos afetados

A implementação futura poderá alterar:

```text
src/main/java/dev/manoelreis/ordermanagement/cli/Main.java
src/main/java/dev/manoelreis/ordermanagement/cli/menu/TerminalSession.java
src/main/java/dev/manoelreis/ordermanagement/cli/input/TerminalInput.java
src/main/java/dev/manoelreis/ordermanagement/cli/output/TerminalOutput.java
src/test/java/dev/manoelreis/ordermanagement/cli/
README.md
specs/SPEC-011-payment-cli.md
specs/README.md
```

Alterar `TerminalInput` somente se a implementação optar por centralizar a conversão da escolha simulada; comparação simples na sessão também é permitida.

A criação desta specification autoriza somente este documento e a atualização do índice. Não autoriza implementar o fluxo nesta tarefa de documentação.

Não alterar `domain`, `application`, `infrastructure`, `shared`, `pom.xml` ou Maven Wrapper.

## 12. Estratégia de testes

Usar JUnit Jupiter, services e repositories reais, `Clock.fixed`, entrada em memória e saída capturada. A composição dos testes deve compartilhar uma única instância de `InMemoryOrderRepository` entre `OrderApplicationService` e `PaymentApplicationService`.

Cobrir, no mínimo:

- preservação das opções existentes e exibição da opção `13`;
- rejeição de `PaymentApplicationService` nulo no construtor;
- aprovação de pedido com item: mensagem de aprovação, status `PAID` e pedido pago visível por consulta ou listagem na mesma sessão;
- recusa de pedido com item: mensagem de recusa, status `DRAFT`, itens e subtotal preservados e possibilidade de continuar usando a sessão;
- tentativa de pagar pedido vazio e de pagar novamente pedido já pago: mensagem de operação rejeitada, sem encerramento;
- pedido inexistente: mensagem com UUID e continuidade da sessão;
- escolha `1` e `2` com espaços externos;
- escolhas inválidas, incluindo linha vazia, texto, números diferentes e conteúdo com espaços internos, sem pagamento;
- UUIDs válidos com letras maiúsculas e espaços externos, além dos formatos inválidos já definidos;
- EOF antes do ID e antes do resultado simulado, sem tentativa de pagamento ou mensagem de entrada inválida;
- aprovação seguida de tentativa de editar itens, confirmando o comportamento já exposto pela CLI;
- recusa seguida de nova tentativa aprovada, confirmando que o mesmo pedido e repository são usados;
- isolamento entre sessões compostas com repositories distintos e preservação dos testes existentes de recursos e I/O.

Não repetir em detalhe os testes unitários da SPEC-006 sobre valor cobrado, quantidade de chamadas, resultado nulo ou falha técnica da estratégia. Na CLI, testar a integração observável com os contratos existentes.

Na implementação, executar `./mvnw test` e `./mvnw clean package`. Realizar smoke test do comando documentado com entrada controlada, cobrindo criação de cliente, produto e pedido, inclusão do item, aprovação do pagamento, consulta do status `PAID` e saída.

## 13. Critérios de aceite

- **CA-01:** o menu preserva as operações existentes e oferece a opção `13` para processar pagamento.
- **CA-02:** a composição usa a mesma instância de repository de pedidos nos services de pedido e pagamento.
- **CA-03:** o usuário pode selecionar aprovação ou recusa de forma explícita e determinística.
- **CA-04:** aprovação é apresentada e torna o pedido `PAID` na mesma sessão.
- **CA-05:** recusa é apresentada, preserva o pedido em `DRAFT` e não impede a continuidade da sessão.
- **CA-06:** UUID ou escolha inválidos, pedido inexistente, pedido vazio e pedido já pago são tratados sem encerramento indevido ou mutação incorreta.
- **CA-07:** EOF em qualquer campo encerra sem tentativa parcial de pagamento.
- **CA-08:** a CLI delega ao service e ao domínio, sem acessar repository, chamar `Order.pay`, calcular valores ou alterar status diretamente.
- **CA-09:** nenhum meio de pagamento real, classe de infraestrutura ou dependência adicional é introduzido.
- **CA-10:** testes, build e smoke test passam; README documenta o novo fluxo e seu caráter simulado.

## 14. Definition of Done

- [ ] Requisitos funcionais atendidos
- [ ] Requisitos técnicos atendidos
- [ ] Composição manual do service de pagamentos implementada e verificada
- [ ] Fluxos de aprovação e recusa implementados e testados
- [ ] Erros esperados e EOF verificados no novo fluxo
- [ ] Testes relevantes implementados e aprovados
- [ ] Build aprovado com `./mvnw clean package`
- [ ] Smoke test do comando de execução aprovado
- [ ] Nenhum item fora do escopo implementado
- [ ] Documentação afetada atualizada
- [ ] Estado alterado para `Concluída` somente após a validação da implementação

## 15. Critério de parada

Parar quando a CLI permitir processar, de forma determinística, aprovação ou recusa simulada de um pedido existente por meio de `PaymentApplicationService`, mantendo estado compartilhado durante a sessão, erros recuperáveis, EOF seguro, testes e build aprovados.

Não iniciar meios de pagamento reais, dados de cobrança, histórico, estorno, cancelamento, descontos, relatórios no terminal ou persistência em arquivos. Essas evoluções exigem specifications próprias.
