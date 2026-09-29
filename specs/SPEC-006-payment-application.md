# SPEC-006 — Processamento Simulado de Pagamentos

## 1. Metadados

- **Estado:** Concluída
- **Dependências:** SPEC-002 e SPEC-004 concluídas
- **Entrega:** Caso de uso de pagamento de pedidos por uma estratégia simulada, com resultado de aprovação ou recusa e transição de pedido aprovado para `PAID`

## 2. Objetivo

Permitir que a camada `application.payment` coordene uma tentativa de pagamento de um pedido existente sem introduzir integração financeira real. O domínio deve proteger as condições para pagar um pedido, impedir alterações de itens após a aprovação e manter o estado do pedido consistente quando o pagamento for recusado.

O resultado esperado é uma API Java testável com métodos de pagamento simulados fornecidos pelo chamador, sem interface de terminal, gateway externo ou implementação concreta de repository.

## 3. Contexto

A SPEC-002 introduziu `Order`, itens, subtotal e o único status atual, `DRAFT`. A SPEC-004 introduziu `OrderRepository` e `OrderNotFoundException`. Diferentemente dos contextos de clientes e produtos, o domínio ainda não possui conceitos de pagamento; por isso esta spec autoriza **somente** os tipos e comportamentos de domínio indispensáveis ao primeiro caso de uso de pagamento.

```text
Caller
   │
   ▼
PaymentApplicationService
   ├──► OrderRepository
   └──► Order.pay(PaymentMethod)
           └──► PaymentMethod.process(subtotal) -> PaymentResult
```

`PaymentMethod` é a estratégia recebida pela chamada: doubles simples nos testes podem simular aprovação ou recusa. Sua existência não exige implementações de PIX, cartão ou boleto nesta etapa. O valor cobrado é o subtotal já calculado por `Order`; descontos e total final não foram definidos.

## 4. Pré-condições

- SPEC-002 e SPEC-004 concluídas.
- `Order`, `OrderId`, `OrderStatus`, `OrderRepository` e `OrderNotFoundException` implementados e testados.
- Projeto compilando e com testes executáveis via Maven Wrapper e Java 21.
- Nenhuma dependência adicional necessária.

## 5. Escopo

Esta spec autoriza:

- introduzir `PaymentMethod` e `PaymentResult` em `domain.payment`;
- adicionar `PAID` ao `OrderStatus` e um comportamento de pagamento em `Order`;
- rejeitar pagamento de pedido vazio ou já pago antes de acionar o método de pagamento;
- impedir alterações dos itens de um pedido pago;
- criar `PaymentApplicationService` em `application.payment` para localizar, pagar e salvar um pedido aprovado;
- retornar a recusa sem alterar nem salvar o pedido;
- criar testes unitários do novo comportamento no domínio e na aplicação;
- atualizar a documentação afetada ao concluir a implementação.

## 6. Fora do escopo

Não implementar nesta etapa:

- gateways, operações bancárias, cobrança real, credenciais ou dados de cartão;
- implementações concretas de PIX, cartão, boleto ou seleção automática de método;
- cadastro ou repository próprio de pagamentos; histórico ou persistência de tentativas recusadas;
- descontos, taxas, frete, parcelamento, reembolsos ou total diferente do subtotal;
- cancelamento, estorno, reabertura ou outras transições de status;
- timestamp de pagamento, `Clock` ou identificador de transação;
- validação de meios de pagamento por cliente, antifraude ou idempotência distribuída;
- implementação concreta de repository, arquivos, JSON, Jackson, CLI ou serviços externos;
- alterações em `OrderApplicationService`, `OrderRepository` ou nos contextos de clientes e produtos;
- mecanismos transacionais, concorrência ou retentativas automáticas;
- DTOs, comandos, services genéricos, factories ou infraestrutura para extensões hipotéticas;
- Spring ou qualquer tecnologia proibida pelo `AGENTS.md`.

## 7. Requisitos funcionais

### RF-01 — Pagamento de pedido elegível

Ao receber `OrderId` e `PaymentMethod`, a aplicação deve localizar o pedido por `OrderRepository` e solicitar a `Order` que execute o pagamento. Um pedido elegível está em `DRAFT` e contém ao menos um item. O domínio deve passar o subtotal do pedido ao método exatamente uma vez.

Se o método aprovar, `Order` deve passar a `PAID`, a aplicação deve solicitar o armazenamento do pedido alterado uma vez e retornar `PaymentResult.APPROVED`.

### RF-02 — Recusa de pagamento

Se o método recusar, a aplicação deve retornar `PaymentResult.DECLINED`. O pedido deve permanecer em `DRAFT`, com os mesmos itens e subtotal, e a aplicação não deve solicitar seu armazenamento. A recusa é um resultado esperado, não uma exception.

### RF-03 — Pedido ausente

Se o repository não encontrar o pedido, a aplicação deve propagar `OrderNotFoundException` com o identificador procurado. Nenhum método de pagamento deve ser acionado e nenhum `save` deve ocorrer.

### RF-04 — Invariantes de pagamento

Pedidos vazios ou já pagos não podem iniciar outra tentativa de pagamento. O domínio deve rejeitá-los antes de invocar `PaymentMethod`. Falhas não devem avançar o status nem gerar chamada a `save`.

### RF-05 — Imutabilidade dos itens após aprovação

Um pedido `PAID` não pode receber produtos, ter quantidades alteradas ou ter itens removidos. O domínio deve rejeitar essas operações antes de mutar seus itens. Os dados do pedido e o subtotal devem permanecer iguais após cada rejeição.

## 8. Requisitos técnicos

### RT-01 — Linguagem e packages

Usar Java 21. As adições de domínio pertencem a `dev.manoelreis.ordermanagement.domain.payment` e `dev.manoelreis.ordermanagement.domain.order`; a coordenação pertence a `dev.manoelreis.ordermanagement.application.payment`. Os testes acompanham esses packages.

### RT-02 — Direção de dependências

`domain.order` pode depender de `domain.payment`; nenhum package de domínio pode depender de `application`, `infrastructure`, CLI ou Jackson. `application.payment` pode depender de `domain.order`, `domain.payment`, `application.order` e da JDK.

### RT-03 — Contrato da estratégia

`PaymentMethod` deve ser uma interface de domínio com uma operação:

```text
process(BigDecimal amount) -> PaymentResult
```

O método não recebe o objeto `Order` nem altera seu status diretamente. A implementação fornecida pelo chamador determina apenas aprovação ou recusa, sem exigência de infraestrutura concreta nesta spec.

### RT-04 — Resultado

`PaymentResult` deve representar exclusivamente `APPROVED` ou `DECLINED`. Não utilizar `null`, texto livre ou exception para representar recusa. Um `PaymentMethod` que retorne `null` viola o contrato e deve causar falha, sem marcar o pedido como pago.

### RT-05 — Estado e comportamento de Order

`OrderStatus` deve conter `DRAFT` e `PAID`. `Order.pay(PaymentMethod method) -> PaymentResult` deve preservar as invariantes do pedido, chamar `method.process(getSubtotal())` uma vez para pedido elegível e marcar `PAID` somente após `APPROVED`. Os métodos públicos de alteração de itens devem rejeitar pedidos pagos antes de alterar o estado. `Order` não deve armazenar a implementação de `PaymentMethod` nem dados de cobrança.

### RT-06 — Application service

`PaymentApplicationService` deve ser uma classe concreta e final, receber somente `OrderRepository` no construtor e oferecer:

```text
processPayment(OrderId orderId, PaymentMethod method) -> PaymentResult
```

O service deve delegar regras de elegibilidade e transição ao domínio e chamar `save` apenas após aprovação. Não deve reimplementar cálculo de subtotal ou estado do pedido.

### RT-07 — Dependências e testes

Nenhuma dependência de produção deve ser adicionada. Usar JUnit Jupiter e objetos reais de domínio; doubles manuais ou Mockito podem isolar exclusivamente `OrderRepository` e `PaymentMethod`.

## 9. Modelo e contratos

### 9.1 `PaymentMethod`

Representa uma tentativa de cobrança de um valor `BigDecimal` já calculado por `Order`. Seu retorno deve ser um `PaymentResult` não nulo. Não define marca comercial, credenciais, rede ou persistência.

### 9.2 `PaymentResult`

Enum imutável com `APPROVED` e `DECLINED`. É o resultado da tentativa atual, não uma entidade de pagamento nem um comprovante. O resultado recusado não implica transição de status.

### 9.3 `Order` e `OrderStatus`

`Order` permanece responsável pelo subtotal e pelo estado do pedido. `pay` verifica método não nulo, `DRAFT` e itens não vazios antes da cobrança. Uma aprovação torna o pedido `PAID`; uma recusa preserva `DRAFT`. Depois da aprovação, alterações em itens são proibidas. Os itens existentes, seus snapshots e o subtotal permanecem inalterados pela cobrança.

### 9.4 `PaymentApplicationService`

Coordena consulta em `OrderRepository`, chamada a `Order.pay` e armazenamento somente após aprovação. Reutiliza `OrderNotFoundException`, sem criar `PaymentRepository` ou modificar `OrderApplicationService`.

## 10. Tratamento de erros

- `OrderRepository` nulo no construtor deve produzir `IllegalArgumentException`;
- `OrderId` nulo deve produzir `IllegalArgumentException` antes de consultar o repository;
- pedido ausente deve produzir `OrderNotFoundException` antes de cobrar ou salvar;
- método nulo, pedido vazio ou pedido já pago devem ser rejeitados pelo domínio com `DomainException`, antes de acionar `PaymentMethod` ou alterar o pedido;
- resultado nulo de `PaymentMethod` deve produzir `DomainException`, sem mudar o status nem solicitar `save`;
- tentativa de alterar itens de pedido pago deve produzir `DomainException`, preservando o estado anterior;
- recusa deve retornar `DECLINED`, sem exception nem `save`;
- falhas técnicas lançadas pelo método ou repository não devem ser ocultadas ou transformadas; uma falha antes da aprovação não deve marcar o pedido como pago;
- não prometer reversão em memória se `save` falhar após a aprovação: transações e recuperação estão fora desta spec.

## 11. Arquivos afetados

A implementação desta spec poderá criar ou alterar somente:

```text
src/main/java/dev/manoelreis/ordermanagement/domain/payment/
src/main/java/dev/manoelreis/ordermanagement/domain/order/Order.java
src/main/java/dev/manoelreis/ordermanagement/domain/order/OrderStatus.java
src/main/java/dev/manoelreis/ordermanagement/application/payment/
src/test/java/dev/manoelreis/ordermanagement/domain/payment/
src/test/java/dev/manoelreis/ordermanagement/domain/order/OrderTest.java
src/test/java/dev/manoelreis/ordermanagement/application/payment/
README.md
specs/SPEC-006-payment-application.md
```

A criação desta specification também autoriza atualizar `specs/README.md`. Não alterar os repositories existentes, as camadas de infraestrutura ou CLI, os serviços de cliente, produto ou pedido nem o `pom.xml`.

## 12. Estratégia de testes

Usar `Order` e `OrderItem` reais. Fornecer estratégias de pagamento controladas nos testes para aprovar, recusar, retornar `null` e lançar falha. Um double de `OrderRepository` permite observar consulta e armazenamento sem criar infraestrutura concreta.

Cobrir, no mínimo:

- construtor do service e `OrderId` nulos, sem consulta ou cobrança;
- pedido ausente, sem cobrança ou `save`, preservando `OrderNotFoundException`;
- aprovação de pedido em rascunho com itens: valor igual ao subtotal em `BigDecimal`, uma chamada ao método, status `PAID`, um `save` e retorno `APPROVED`;
- recusa: retorno `DECLINED`, status `DRAFT`, itens e subtotal preservados, sem `save`;
- pedido vazio ou já pago e método nulo: erro de domínio, sem cobrança ou `save`;
- resultado nulo ou exception técnica do método: nenhuma transição ou `save`;
- tentativas de adicionar, alterar quantidade ou remover itens após aprovação: erro de domínio e invariantes preservadas;
- subtotal baseado nos snapshots dos itens, sem consulta a produtos ou recálculo pela aplicação.

Evitar repetir exaustivamente os testes já existentes de validação de itens e preços. Exercitar resultados observáveis e interações relevantes, sem testar detalhes internos.

## 13. Critérios de aceite

- **CA-01:** a application localiza um pedido existente e usa somente seu subtotal como valor da tentativa de pagamento.
- **CA-02:** aprovação retorna `APPROVED`, atualiza o status para `PAID` e salva o pedido uma vez.
- **CA-03:** recusa retorna `DECLINED`, mantém o pedido em `DRAFT` e não chama `save`.
- **CA-04:** pedido ausente, vazio ou já pago, método nulo e resultado nulo são tratados conforme os contratos de erro, sem cobrança ou armazenamento indevido.
- **CA-05:** nenhum item de pedido pago pode ser modificado.
- **CA-06:** invariantes e transição permanecem em `Order`, não duplicadas em `PaymentApplicationService`.
- **CA-07:** domínio e aplicação não dependem de infraestrutura, CLI ou Jackson; o service recebe o repository pelo construtor.
- **CA-08:** nenhum gateway, método concreto de pagamento ou repository adicional é criado.
- **CA-09:** testes relevantes e `./mvnw clean package` passam.

## 14. Definition of Done

- [x] Requisitos funcionais atendidos
- [x] Requisitos técnicos atendidos
- [x] `PaymentMethod`, `PaymentResult` e transição de `Order` implementados e testados
- [x] `PaymentApplicationService` implementado e testado
- [x] Testes relevantes implementados e aprovados
- [x] Build aprovado com `./mvnw clean package`
- [x] Nenhum item fora do escopo implementado
- [x] Documentação afetada atualizada
- [x] Estado da spec alterado para `Concluída` somente após a validação

## 15. Critério de parada

Parar quando uma tentativa simulada de pagamento de pedido existente puder ser aprovada ou recusada, com invariantes e status protegidos pelo domínio, persistência solicitada apenas para aprovação, testes aprovados e build limpo. Não iniciar gateways reais, implementações de métodos, histórico de pagamentos, descontos, infraestrutura de repository, CLI ou outras transições de pedido; essas evoluções exigem specifications próprias.
