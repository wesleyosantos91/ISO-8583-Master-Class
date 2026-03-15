# Exercícios — Semana 28 — Certificação e Projeto Final

## Exercício 1 — Anatomia de um Caso de Teste do Test Deck (Fixação)

O test deck da Visa contém ~150-300 cenários. Cada cenário tem: título, pré-condição, entrada (campos ISO 8583 exatos), saída esperada e critério de aprovação.

Escreva manualmente os casos de teste abaixo no formato completo:

**TC-AUTH-001: Compra aprovada — chip inserido, crédito à vista**
```
Pré-condição: BIN 453201, saldo disponível > R$ 100
MTI de entrada: ?
DE 2:  4532010000001234
DE 3:  ?  (compra crédito)
DE 4:  ?  (R$ 100,00 em centavos, 12 dígitos)
DE 22: ?  (chip inserido = contact chip)
DE 25: ?  (condição normal)
Saída esperada: MTI=?, DE39=?
Critério: transação aprovada, DE38 presente
```

**TC-REVERSAL-001: Timeout no adquirente gera reversal automático**
```
Pré-condição: emissor simulado configurado para não responder em 30s
Ação: enviar 0100 e aguardar timeout
Resultado esperado: switch envia 0400 automaticamente
DE90 no 0400: conter dados da mensagem original
Critério: 0410 recebido com DE39=00
```

Escreva também TC-AUTH-004 (cartão expirado → DE39=54) e TC-NETWORK-001 (sign-on bem-sucedido).

**Objetivo:** Aprender a escrever casos de teste no formato das bandeiras, com campos precisos.

---

## Exercício 2 — Implementar o Test Deck Runner (Intermediário)

Implemente `TestDeckRunner` que executa os 20 cenários obrigatórios contra o switch:

```java
public class TestDeckRunner {

    public record TestCase(
        String id,           // "TC-AUTH-001"
        String description,
        ISOMsg inputMessage,
        Predicate<ISOMsg> assertion,  // o que verificar na resposta
        String failureMessage
    ) {}

    public record TestResult(
        String testCaseId,
        boolean passed,
        ISOMsg actualResponse,
        String failureReason,
        long latencyMs
    ) {}

    /**
     * Executa todos os cenários e retorna resultados.
     * Imprime progresso durante execução.
     * Meta: 100% de aprovação.
     */
    public List<TestResult> runAll(List<TestCase> testCases, ISOChannel channel) { /* ... */ }

    /**
     * Gera relatório de execução no formato esperado pelas bandeiras:
     * cenário, resultado, campos enviados vs esperados vs recebidos.
     */
    public String generateReport(List<TestResult> results) { /* ... */ }
}
```

Implemente pelo menos os cenários 1-10 da tabela da teoria (semana 28, seção 2.3).

**Objetivo:** Construir a infraestrutura de test deck que seria usada em uma certificação real.

---

## Exercício 3 — README de Nível Sênior (Intermediário)

Escreva o `README.md` do payment-switch-lab no padrão que um engenheiro sênior de pagamentos esperaria encontrar em um projeto profissional.

Deve conter:
1. **O que é** (2-3 parágrafos sem jargão excessivo)
2. **Arquitetura** (diagrama Mermaid do fluxo principal)
3. **Fluxos suportados** (tabela: MTI, descrição, campos obrigatórios)
4. **Como rodar** (máximo 5 comandos para subir localmente)
5. **Test deck** (como executar os 20 cenários)
6. **Decisões de arquitetura** (link para ADRs)
7. **Operação** (link para runbook)
8. **Roadmap** (o que falta para produção)

**Critério:** Um engenheiro que nunca viu o projeto deve conseguir rodá-lo e entender a arquitetura em menos de 30 minutos lendo apenas o README.

---

## Exercício 4 — Runbook Operacional (Avançado)

Escreva `runbook.md` com os procedimentos de resposta para os 5 alertas mais comuns:

Para cada alerta, use o formato:
```
### ALERT: [nome_do_alerta]
**Severidade:** CRÍTICO / ALTO / MÉDIO
**Causa provável:** ...
**Como detectar:** [métrica ou log que dispara]
**Investigação:**
  1. Verificar [o quê] em [onde]
  2. ...
**Ação:**
  1. [primeira ação]
  2. ...
**Escalação:** Se não resolvido em [X minutos], escalar para [quem]
**Prevenção:** [o que fazer para evitar recorrência]
```

Alertas obrigatórios:
1. `auth_latency_p95 > 500ms` por 5 minutos
2. `timeout_rate > 1%` nas transações off-us
3. `reversal_exhausted` (reversal não entregue após 3 tentativas)
4. `duplicate_rate > 0.5%` (possível bug de terminal)
5. `channel_health_check_failed` (emissor não responde ao echo 0800)

**Objetivo:** Produzir o runbook que o time de plantão usaria às 3h da manhã.

---

## Exercício 5 — Apresentação Multi-Audiência (Revisão Final)

Você deve apresentar o payment-switch-lab para 4 audiências diferentes. Para cada uma, escreva o roteiro da apresentação (máximo 10 linhas cada):

**Audiência 1 — CTO (5 minutos):**
Foco em: o que o sistema faz, capacidade (TPS, latência), cobertura de testes, estado atual.

**Audiência 2 — Engenheiro Sênior de Pagamentos (30 minutos):**
Foco em: arquitetura (C4), pipeline do TransactionManager, fluxos avançados implementados, decisões técnicas e trade-offs.

**Audiência 3 — Head de Produto (15 minutos):**
Foco em: casos de uso suportados (compra, parcelamento, pré-autorização), fluxo do dinheiro, limitações atuais, o que é necessário para ir a produção.

**Audiência 4 — Desenvolvedor Júnior (20 minutos):**
Foco em: o que é ISO 8583, como funciona um switch, como contribuir com o projeto, onde aprender mais.

Salve em `presentations.md`.

**Objetivo:** Demonstrar que você domina o material ao ponto de explicá-lo para qualquer audiência — a marca de um especialista de referência.
