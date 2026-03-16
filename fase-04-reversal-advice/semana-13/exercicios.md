# Semana 13 — Exercícios de Fixação

## Exercício 1 — Quando Reverter? (Nível: Iniciante)

Para cada cenário abaixo, decida: **deve reverter ou não?** Justifique em uma frase.

| # | Cenário | Reverter? | Justificativa |
|---|---------|-----------|---------------|
| 1 | Terminal enviou 0200, recebeu DE39=00, mas a impressora falhou e não imprimiu o comprovante | | |
| 2 | Terminal enviou 0200, recebeu DE39=05 (Do Not Honor) | | |
| 3 | Terminal enviou 0200, conexão TCP caiu antes de receber resposta | | |
| 4 | Terminal enviou 0200, recebeu DE39=51 (Insufficient Funds) | | |
| 5 | Terminal enviou 0200, recebeu DE39=96 (System Malfunction) | | |
| 6 | Terminal enviou 0200, timer de 30s expirou sem nenhuma resposta | | |
| 7 | Terminal enviou 0200, chip EMV não validou o ARPC recebido | | |
| 8 | Terminal enviou 0200, recebeu DE39=01 (Refer to Card Issuer) | | |

**Critério de sucesso:** Acertou todos os 8 cenários e a justificativa reflete a "regra de ouro": se há dúvida sobre o estado do emissor, reverter.

---

## Exercício 2 — Anatomia do DE 90 (Nível: Iniciante)

O DE 90 (Original Data Elements) tem formato fixo de 42 caracteres:

```
MTI(4) | STAN original(6) | DateTime original(10) | AcqID(11) | FwdID(11)
```

Dada a autorização original abaixo, monte manualmente o DE 90 do reversal:

- MTI original: `0200`
- STAN original: `123456`
- DateTime original: `0315143022` (15 de março, 14:30:22)
- Acquiring Institution ID (DE 32): `00012345678`
- Forward Institution ID: não presente (preencher com zeros)

Responda:
1. Qual é o valor completo do DE 90?
2. Qual MTI leva o reversal? (`0400` ou `0410`?)
3. O STAN do reversal deve ser o mesmo do original ou um novo?
4. O DE 7 (Transmission Date/Time) do reversal deve ser o do original ou o momento atual?

**Critério de sucesso:** DE 90 montado corretamente com 42 caracteres, campos na posição certa e padding correto.

---

## Exercício 3 — Implemente ReversalBuilder (Nível: Iniciante/Intermediário)

Implemente a classe `ReversalBuilder` em Java usando jPOS:

```java
public class ReversalBuilder {
    public ISOMsg buildReversal(ISOMsg originalAuth) throws ISOException {
        // TODO: implementar
    }
}
```

Requisitos:
- MTI = `0400`
- Copiar DEs: 2, 3, 4, 12, 13, 22, 25, 32, 41, 42, 49
- Gerar NOVO DE 7 (data/hora atual)
- Gerar NOVO DE 11 (novo STAN)
- Montar DE 90 corretamente (42 chars)

Escreva também um teste JUnit que:
1. Cria uma `ISOMsg` de autorização com os campos mínimos
2. Chama `buildReversal`
3. Verifica que o MTI é `0400`
4. Verifica que o DE 90 tem exatamente 42 caracteres
5. Verifica que o DE 90 começa com o MTI original

**Critério de sucesso:** Teste passa, DE 90 correto, STAN e DateTime são novos.

---

## Exercício 4 — Implemente AutoReversalEngine (Nível: Intermediário)

Implemente `AutoReversalEngine` como `TransactionParticipant` do jPOS com as seguintes regras:

- Chamar `scheduleReversal` no método `abort` quando `NEEDS_REVERSAL = true`
- Máximo de 3 tentativas
- Intervalo de 15 segundos entre tentativas (use `ScheduledExecutorService`)
- Considerar sucesso quando DE39 = `00` (reversed) ou `76` (not found — nunca processou)
- Após esgotar todas as tentativas: chamar `alertOperations`

Escreva testes para os seguintes cenários:
1. Reversal bem-sucedido na primeira tentativa (DE39=00)
2. Reversal bem-sucedido na segunda tentativa (primeira timeout, segunda DE39=00)
3. Emissor responde DE39=76 — deve ser tratado como sucesso
4. Todas as tentativas falham — deve chamar `alertOperations`

**Critério de sucesso:** Todos os 4 cenários de teste passam.

---

## Exercício 5 — Late Response: O Problema da Resposta Tardia (Nível: Intermediário)

**Cenário:** O switch enviou um 0200 para o emissor. O timer de 30s expirou. O switch enviou um 0400 (reversal). Nesse momento, a resposta original (0210, DE39=00) chegou.

Responda:
1. O switch deve processar essa resposta tardia ou ignorá-la?
2. Se ignorar: existe risco de o portador ser cobrado? Por quê?
3. Se processar: o que acontece com o reversal já enviado?
4. Qual é a decisão correta e por quê?

Implemente em código: um `LateResponseHandler` que:
- Detecta respostas com timestamp anterior ao envio do reversal
- Descarta a resposta tardia
- Loga o evento com nível WARN incluindo: STAN original, timestamp da resposta, timestamp do reversal

**Critério de sucesso:** Código implementado, late response descartada, log gerado com campos corretos.

---

## Exercício 6 — Persistência de Reversals Pendentes (Nível: Intermediário/Avançado)

**Problema:** O switch reinicia enquanto há um reversal pendente na memória. O reversal é perdido. O portador fica com a cobrança indevida.

Implemente `PersistentReversalQueue` que:
1. Ao enfileirar um reversal: grava imediatamente em banco de dados (use H2 em memória para o exercício)
2. Ao inicializar: carrega reversals pendentes do banco e reagenda
3. Ao confirmar sucesso: remove do banco
4. Ao falhar todas as tentativas: marca como `EXHAUSTED` no banco (não remove — evidência)

Esquema sugerido:
```sql
CREATE TABLE pending_reversals (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    stan        VARCHAR(6),
    original_mti VARCHAR(4),
    reversal_payload TEXT,  -- JSON ou hex do ISOMsg
    retries_left INT,
    enqueued_at TIMESTAMP,
    status      VARCHAR(20) -- PENDING, EXHAUSTED
);
```

Escreva um teste que:
1. Enfileira 3 reversals
2. Simula restart (recria o engine do zero, lendo do banco)
3. Verifica que todos os 3 reversals foram reprocessados

**Critério de sucesso:** Após restart simulado, reversals pendentes são retomados.

---

## Exercício 7 — Diagrama de Sequência: Cenários de Reversal (Nível: Avançado)

Crie um diagrama Mermaid (`reversal-scenarios.mermaid`) cobrindo os 4 fluxos abaixo em um único diagrama com `alt`:

1. **Fluxo normal:** autorização aprovada sem reversal
2. **Timeout simples:** auth timeout → reversal imediato → emissor confirma (DE39=00)
3. **Late response:** auth timeout → reversal enviado → resposta original chega tarde → reversal confirmado
4. **Double failure:** auth timeout → reversal também dá timeout → retry 1 → retry 2 → retry 3 → alerta crítico

Cada fluxo deve mostrar: POS, Switch, Emissor e os timestamps aproximados de cada evento.

**Critério de sucesso:** Diagrama correto, todos os 4 fluxos representados, timestamps coerentes.

---

## Exercício 8 — Análise de Impacto Financeiro (Nível: Avançado)

Um switch processa 500.000 transações/dia com ticket médio de R$ 120,00. A taxa de timeout atual é 0,3%.

Calcule:
1. Quantos reversals são gerados por dia?
2. Se 2% dos reversals falham (REVERSAL_EXHAUSTED): quantos portadores são cobrados indevidamente por dia?
3. Qual o valor total de cobranças indevidas por dia? Por mês?
4. Se o SLA contratual exige taxa de reversal bem-sucedido > 99,5%: a operação atual está dentro do SLA?
5. Para zerar as cobranças indevidas, qual deveria ser o processo de intervenção manual após REVERSAL_EXHAUSTED?

Documente em `analise-impacto-reversal.md` com os cálculos e proposta de processo.

**Critério de sucesso:** Cálculos corretos, SLA avaliado, processo de intervenção documentado.
