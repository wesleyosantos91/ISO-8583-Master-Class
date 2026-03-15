# Semana 7 — Exercícios de Fixação
## TransactionManager + Participants

---

## Exercício 1 — Vocabulário do TransactionManager (Nível: Iniciante)

Preencha a tabela sem consultar a teoria:

| Conceito | Significado | Quando ocorre |
|----------|-------------|---------------|
| `PREPARED` | | |
| `ABORTED` | | |
| `NO_JOIN` | | |
| `commit()` | | |
| `abort()` | | |
| `GroupSelector` | | |
| `Context` | | |
| `Space` | | |

**Critério de sucesso:** Todos os 8 conceitos corretos, sem consulta.

---

## Exercício 2 — Fluxo de Estados (Nível: Iniciante)

Dado o pipeline com 4 participants (A, B, C, D), trace o fluxo para cada cenário:

```
A → B → C → D
(todos implementam prepare/commit/abort)
```

**Cenário 1:** A=PREPARED, B=PREPARED, C=PREPARED, D=PREPARED
- Quais métodos são chamados em cada participant?
- Em que ordem?

**Cenário 2:** A=PREPARED, B=PREPARED, C=ABORTED, D=nunca chamado
- Quais métodos são chamados em cada participant?
- Quem recebe `abort()`?

**Cenário 3:** A=PREPARED, B=NO_JOIN, C=PREPARED, D=PREPARED
- Quais participants recebem `commit()`?
- Por que B não recebe?

Para cada cenário, desenhe o fluxo como diagrama ASCII.

**Critério de sucesso:** Os três fluxos corretos, sem confundir quem recebe commit vs abort.

---

## Exercício 3 — Implementando QueryHost (Nível: Intermediário)

Implemente `QueryHost.java` que:

1. Implementa `GroupSelector` (que estende `TransactionParticipant`)
2. No `prepare()`: extrai a `ISOMsg` do Space e coloca no Context com chave `"REQUEST"`
3. No `select()`: retorna o nome do grupo baseado no MTI:
   - `"0100"`, `"0200"` → `"authorization"`
   - `"0400"`, `"0420"` → `"reversal"`
   - `"0800"` → `"network-mgmt"`
   - MTI desconhecido → `null` (pula para BuildResponse)
4. No `abort()`: loga o motivo do aborto com o ID da transação

**Requisito adicional:** Se a mensagem não estiver no Space após 100ms de espera, retornar `ABORTED` com código de resposta `"96"` no Context.

Implemente teste unitário com Context mockado verificando cada case do MTI.

**Critério de sucesso:** Implementação funcional, todos os testes passando.

---

## Exercício 4 — Implementando ValidateMessage (Nível: Intermediário)

Implemente `ValidateMessage.java` baseado no exemplo da teoria, com as seguintes **adições**:

1. **Validação por MTI:** Campos obrigatórios diferentes para cada tipo:
   - `0200` (financial): DE2, DE3, DE4, DE7, DE11, DE22, DE25, DE41, DE42, DE49
   - `0400` (reversal): DE2, DE3, DE4, DE7, DE11, DE41, DE42
   - `0800` (network): apenas DE11 e DE70

2. **Validação de Luhn** para DE2

3. **Validação de Processing Code:** DE3 deve ser um dos valores conhecidos

4. **Validação de Amount:** DE4 deve ser numérico e maior que zero

5. **Código de response correto:** coloca no Context o `RESPONSE_CODE` apropriado para cada tipo de erro

Implemente testes para pelo menos 6 cenários de erro diferentes.

**Critério de sucesso:** Todos os cenários de erro geram o response code correto.

---

## Exercício 5 — Implementando BuildResponse (Nível: Intermediário)

Implemente `BuildResponse.java` que:

1. Lê `"RESPONSE"` do Context. Se não existe, usa `"REQUEST"` como base
2. Garante que o MTI seja o response correto (`0200` → `0210`, `0100` → `0110`, etc.)
3. Copia do request para a response os campos: DE2, DE3, DE4, DE7, DE11, DE37, DE41, DE42
4. Preenche DE39 com o `"RESPONSE_CODE"` do Context (default: `"96"` se não houver)
5. Preenche DE38 (auth code) se DE39 for `"00"` e houver `"AUTH_CODE"` no Context
6. Envia a response de volta via `ISOSource` do Context

Trate o caso onde `ISOSource` é null (pode acontecer em testes).

**Critério de sucesso:** Response corretamente construída para os 4 MTIs principais.

---

## Exercício 6 — Implementando AuditLog (Nível: Intermediário)

Implemente `AuditLog.java` que usa `NO_JOIN` (não precisa de commit/abort) e loga:

**No `prepare()`:**
```
[AUDIT] txn_id=12345 mti=0200 pan=4532****0366 amount=150.00 terminal=TERM0001
        stan=000042 route=ON_US response_code=00 duration_ms=45
```

**Requisitos:**
- PAN deve ser mascarado: primeiros 6 e últimos 4 dígitos visíveis
- Amount deve ser formatado como valor monetário (`150.00`, não `000000015000`)
- `duration_ms` vem do Context (colocado por um participant anterior)
- Se algum campo faltar, logar `"N/A"` em vez de null ou exceção

**Por que `NO_JOIN`?** Explique no Javadoc da classe.

**Critério de sucesso:** Log formatado corretamente, PAN mascarado, sem NPE em campos ausentes.

---

## Exercício 7 — Pipeline Completo (Nível: Avançado)

Configure o `05_txnmgr.xml` e implemente o pipeline completo:

```
QueryHost (GroupSelector)
    ├── [group authorization]
    │   ├── ValidateMessage
    │   ├── RouteByBIN (stub: sempre ON_US)
    │   └── ForwardToIssuer (stub: sempre aprova com "00")
    ├── [group reversal]
    │   └── ProcessReversal (stub: sempre OK)
    └── [group network-mgmt]
        └── ProcessEcho (responde com DE39="00")
BuildResponse
AuditLog
```

Implemente um teste de integração que:
1. Cria um `ISOMsg` de autorização (0200)
2. Coloca no Space
3. Aguarda a response no Space
4. Verifica que DE39 = `"00"`
5. Verifica que o log de auditoria foi gerado

**Critério de sucesso:** Teste de integração passando, pipeline completo funcionando.

---

## Exercício 8 — TimingParticipant (Nível: Avançado)

Implemente `TimingParticipant.java` que mede a latência de cada transação:

```java
public class TimingParticipant implements TransactionParticipant {

    // prepare(): registra System.nanoTime() no Context com chave "TIMING_START"
    // commit(): calcula latência e loga como métrica
    // abort(): também calcula e loga (com label "aborted")
}
```

**Formato da métrica:**
```
[METRIC] iso8583.txn.latency mti=0200 result=approved duration_ms=47.3
[METRIC] iso8583.txn.latency mti=0200 result=declined duration_ms=12.1
[METRIC] iso8583.txn.latency mti=0200 result=error duration_ms=31.8
```

**Requisito adicional:** Calcule e logue uma média móvel dos últimos 100 valores por MTI.

Onde no pipeline você colocaria esse participant? Justifique a posição.

**Critério de sucesso:** Métrica gerada em commit e abort, média móvel funcionando.
