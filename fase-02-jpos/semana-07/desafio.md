# Semana 7 — Desafio Integrador
## "O Participant que Quebrou o Switch"

---

## O Cenário

São 23h47 de uma sexta-feira. Você recebe um alerta:

> **INCIDENTE P1 — Switch de produção rejeitando 100% das transações**
> Taxa de aprovação: 0% (era 87% às 23h30)
> Início: 23h42
> Volume afetado: ~8.000 transações/minuto
> Último deploy: 23h40 (novo participant `FraudCheck` em produção)

Os logs mostram:

```
23:42:01 ERROR txnmgr - Transaction 8821043 ABORTED by FraudCheck
23:42:01 ERROR txnmgr - Transaction 8821044 ABORTED by FraudCheck
23:42:01 ERROR txnmgr - Transaction 8821045 ABORTED by FraudCheck
...
[10.000 linhas iguais]
...
23:47:55 WARN  txnmgr - Thread pool at capacity (128/128 threads active)
23:48:02 ERROR Q2 - OutOfMemoryError: unable to create new native thread
```

O código do `FraudCheck` que foi para produção:

```java
public class FraudCheck implements TransactionParticipant {

    private final FraudService fraudService = new FraudService();

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg msg = ctx.get("REQUEST");

        try {
            String pan = msg.getString(2);
            boolean isFraud = fraudService.checkRealtime(pan); // Chamada HTTP externa
            if (isFraud) {
                ctx.put("RESPONSE_CODE", "59");
                return ABORTED;
            }
            return PREPARED;
        } catch (Exception e) {
            // Se o serviço de fraude falhar, vamos negar a transação por segurança
            ctx.put("RESPONSE_CODE", "05");
            return ABORTED;
        }
    }

    @Override
    public void commit(long id, Serializable context) {}

    @Override
    public void abort(long id, Serializable context) {}
}
```

Você descobre que o serviço `FraudService` — uma API HTTP externa — está com timeout de 60 segundos (o padrão Java). O serviço de fraude está **lento** (respondendo em 45-60s) mas não está fora.

---

## Sua Missão

### Parte 1 — Diagnóstico do Incidente (20 min)

Escreva `post-mortem-fraude.md` com:

1. **Root cause analysis:** O que exatamente causou o incidente? Identifique no mínimo 3 problemas no código do `FraudCheck`.

2. **Timeline de degradação:** Explique em detalhes o que aconteceu entre 23h40 e 23h48. Por que o sistema foi de "funcional" para "100% falhando" e depois "OutOfMemoryError" em apenas 8 minutos? Descreva o efeito cascata.

3. **Cálculo do impacto:**
   - Transações afetadas: 8.000/min × 8 min = ?
   - Valor estimado bloqueado (ticket médio R$ 95)?
   - Quantos clientes ficaram sem comprar?

4. **Por que ABORTED no catch foi a pior decisão possível aqui?** Qual era a intenção e qual foi o efeito real?

### Parte 2 — Correção Imediata (30 min)

Implemente `FraudCheck.java` corrigido que resolve **todos** os problemas identificados:

**Requisitos obrigatórios:**
1. Timeout máximo de **500ms** para a chamada ao serviço de fraude
2. Em caso de **timeout ou erro do serviço**: aprovar e logar um alerta (fail-open, não fail-closed)
3. **Circuit breaker simples**: após 10 falhas consecutivas, parar de chamar o serviço por 60 segundos
4. **Métricas no Context**: colocar `FRAUD_CHECK_DURATION_MS` e `FRAUD_CHECK_RESULT` para o AuditLog
5. **Thread safety**: o participant pode ser instanciado uma vez e usado por múltiplas threads

```java
public class FraudCheck implements TransactionParticipant {
    // Implemente aqui com todos os requisitos
}
```

Implemente testes unitários para:
- Serviço responde "fraude" em 100ms → ABORTED com DE39=59
- Serviço responde "ok" em 100ms → PREPARED
- Serviço demora 600ms → PREPARED (fail-open) + alerta logado
- Serviço lança exceção → PREPARED (fail-open) + alerta logado
- Após 10 falhas, circuit breaker abre → PREPARED sem chamar serviço

### Parte 3 — Processo de Prevenção (20 min)

Escreva `guia-deploy-participants.md` com:

1. **Checklist de code review** para novos participants (mínimo 8 itens)

2. **Regras de design para participants no switch:**
   - O que NUNCA fazer no `prepare()`
   - Timeout máximo permitido para I/O externo
   - Como tratar falhas de dependências externas (fail-open vs fail-closed — quando usar cada um?)
   - Como garantir thread safety

3. **Estratégia de rollback:** Como reverter um participant de produção em menos de 5 minutos?

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| Identifica os 3+ problemas no código original | /20 |
| Explica corretamente o efeito cascata (threads → OOM) | /15 |
| FraudCheck corrigido: timeout + fail-open funcionando | /25 |
| Circuit breaker implementado e testado | /20 |
| Guia de deploy com regras práticas e acionáveis | /20 |

**Meta:** 80+ pontos = Semana 7 dominada.

---

## Dicas

- O TransactionManager tem um pool de threads (`sessions`/`max-sessions`). Se cada thread fica presa por 60s esperando I/O, o pool esgota rapidamente.
- **Fail-open vs fail-closed:** Em segurança, fail-closed é mais seguro. Em pagamentos, fail-open geralmente é melhor — você prefere aprovar uma transação legítima do que negar por falha de infraestrutura.
- O circuit breaker não precisa ser sofisticado. Um contador atômico + timestamp de última falha já resolve o caso básico.
- Em produção real, você usaria uma biblioteca como Resilience4j para circuit breaker. Aqui implementar manualmente é o objetivo de aprendizado.
- Post-mortem não é sobre culpar pessoas. É sobre identificar o **processo** que permitiu o problema chegar à produção.
