# Desafio — Semana 33 — Regressão de Performance em Produção

## Contexto

Sua empresa lançou na última semana a versão 3.2.0 do switch, que incluiu três mudanças:
- Novo módulo de antifraude com scoring em tempo real (semana 31)
- Atualização do pool de conexões com emissores (de 100 para 300 conexões)
- Migração do Java 11 para Java 21 com ZGC habilitado

O monitoramento do Grafana mostra, comparando segunda-feira desta semana com a anterior:

```
Métrica              | v3.1.0 (semana anterior) | v3.2.0 (esta semana)
---------------------|--------------------------|---------------------
P50 latência         | 18ms                     | 22ms
P95 latência         | 95ms                     | 148ms
P99 latência         | 210ms                    | 890ms   ← REGRESSÃO
Throughput máximo    | 2.800 TPS                | 2.600 TPS
GC pause P99         | 180ms (G1GC)             | 4ms (ZGC)  ← melhora
Taxa de timeout      | 0.02%                    | 0.31%   ← REGRESSÃO
CPU (média)          | 45%                      | 71%     ← REGRESSÃO
```

A equipe está dividida sobre a causa. Três hipóteses estão na mesa:

1. **Hipótese A:** O módulo de antifraude adiciona latência no hot path
2. **Hipótese B:** O pool de 300 conexões está causando contenção de threads
3. **Hipótese C:** O ZGC aumenta o uso de CPU, causando degradação em pico

## O Problema

O P99 de 890ms está acima do SLA contratado de 500ms. O timeout aumentou de 0.02%
para 0.31% — num volume de 2 milhões de transações/dia, isso representa 6.200
transações falhando por dia que antes passavam.

A diretoria quer saber: é possível continuar com o v3.2.0 ou precisa fazer rollback?

## Missão

### Parte 1 — Diagnóstico

**1.1 Análise das hipóteses:**

Para cada hipótese, descreva:
- Que evidência nos dados acima a suporta (ou contradiz)?
- Que métrica adicional você coletaria para confirmá-la?
- Que teste específico eliminaria essa hipótese?

**1.2 Instrumentação adicional:**

O time não instrumentou o tempo do módulo de antifraude separadamente.
Implemente um wrapper de medição para o `FraudScreeningParticipant`:

```java
public class InstrumentedFraudScreeningParticipant implements TransactionParticipant {

    private final FraudScreeningParticipant delegate;
    private final MeterRegistry meterRegistry;

    @Override
    public int prepare(long id, Serializable context) {
        // TODO: medir o tempo de execução do delegate.prepare()
        // Registrar como Timer com tag "participant" = "fraud-screening"
        // Registrar também o resultado (PREPARED vs ABORTED) como Counter
        // Usar io.micrometer.core.instrument.Timer
        return 0;
    }
}
```

### Parte 2 — Tuning

Com base nos dados disponíveis, proponha ajustes de configuração para o v3.2.0
que possam recuperar o P99 sem fazer rollback:

**2.1 jPOS TransactionManager:**
O `sessions` atual é 200 e o `max-sessions` é 500.
- O CPU em 71% sugere que aumentar threads vai ajudar ou piorar? Justifique.
- Qual parâmetro você mudaria primeiro e para qual valor?

**2.2 VelocityEngine (antifraude):**
O Redis do velocity check está no mesmo cluster do Redis de deduplicação.
- Qual risco isso representa em pico de carga?
- Que mudança de arquitetura resolveria isso com menor risco de rollback?

**2.3 Pool de conexões com emissores:**
O pool foi aumentado de 100 para 300 conexões.
- Como verificar se os emissores estão aceitando as 300 conexões?
- O que acontece se um emissor aceita no máximo 200 conexões e você tenta 300?

### Parte 3 — Decisão e Comunicação

Você tem 30 minutos para decidir: rollback para v3.1.0 ou hotfix do v3.2.0?

1. Liste os fatores que pesam a favor do rollback e os que pesam contra.

2. Escreva a mensagem de Slack para o canal de engenharia (máximo 8 linhas) comunicando
   sua decisão e os próximos passos. Seja claro sobre o impacto atual e o plano.

3. Escreva o update de 3 frases para o CTO que explica: o que está acontecendo,
   o que você decidiu e quando estará resolvido.

## Critérios de Avaliação

- [ ] Analisa as três hipóteses com base nas métricas disponíveis (Parte 1.1)
- [ ] Implementa o Timer do Micrometer corretamente no wrapper (Parte 1.2)
- [ ] Raciocina corretamente sobre CPU vs threads (Parte 2.1)
- [ ] Identifica o risco de Redis compartilhado e propõe separação (Parte 2.2)
- [ ] Explica o impacto de conexões acima do limite do emissor (Parte 2.3)
- [ ] Decisão de rollback/hotfix com argumentação consistente com os dados (Parte 3.1)
- [ ] Comunicação para engenharia clara, sem alarme excessivo (Parte 3.2)
- [ ] Update para CTO conciso e orientado a impacto de negócio (Parte 3.3)

## Dicas

- O GC pause P99 melhorou (de 180ms para 4ms) — ZGC está funcionando bem
- CPU em 71% com mesma carga sugere processamento adicional por transação
- 0.31% de timeout com P99 de 890ms são consistentes: as transações lentas estão travando threads
- `Timer.record(Supplier<T>)` do Micrometer captura tempo e resultado em uma chamada
- Rollback é seguro tecnicamente mas tem custo: todas as melhorias do v3.2.0 voltam junto
