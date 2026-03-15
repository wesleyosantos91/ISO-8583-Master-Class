# Semana 21 — Desafio Integrador

## O Cenário

Você é engenheiro sênior responsável pela observabilidade do payment switch. Na quinta-feira às 14:15, o sistema de alertas dispara simultaneamente:

> **ALERTA CRÍTICO:** `iso8583.approval_rate{route="VISA"} = 62% (< 75% threshold)`
>
> **ALERTA CRÍTICO:** `iso8583.timeout_rate{destination="VISA_PRIMARY"} = 8.3% (> 0.5% threshold)`
>
> **ALERTA WARNING:** `iso8583.auth.latency.p95{route="VISA"} = 1850ms (> 1000ms threshold)`

O time de negócio está percebendo: as vendas na maquininha caíram 30% em 10 minutos. É quinta de manhã, início do pico de almoço. Cada minuto conta.

Você tem acesso apenas a:
- Métricas Prometheus/Grafana
- Logs estruturados (Elasticsearch)
- Status dos canais TCP (UP/DOWN/DEGRADED)
- Não há acesso SSH ao servidor em produção (protocolo de segurança)

## Sua Missão

### Parte 1 — Diagnóstico pelos Dados (25 min)

Escreva `diagnostico-s21.md` respondendo:

1. **Com base nos 3 alertas, qual é a hipótese mais provável?** Justifique usando apenas as métricas disponíveis.

2. **Descarte as hipóteses alternativas.** Para cada uma, explique qual métrica ou log você consultaria e o que encontraria (ou não) para eliminá-la:
   - Emissor específico fora do ar (todos os emissores Visa, ou só um?)
   - Bug no código do switch após deploy recente
   - Problema de rede/firewall entre switch e data center Visa
   - Sobrecarga no switch (CPU/memória)
   - Problema de chave criptográfica (ZPK vencida)

3. **Defina as 3 queries de log** que você executaria agora (em Elasticsearch ou SQL), em ordem de prioridade. Para cada query:
   - O que você está buscando?
   - O que a resposta mostraria se sua hipótese estiver correta?
   - O que mostraria se a hipótese estiver errada?

4. **Linha do tempo do incidente:** Com base nos alertas, quando exatamente o problema começou? (Lembre: os alertas disparam quando a métrica fica acima do threshold por X segundos — logo o problema começou antes dos alertas.)

### Parte 2 — Resolução e Mitigação (25 min)

Escreva `resolucao-s21.md` com:

1. **Ação imediata (próximos 5 minutos):**
   Se confirmado que o canal VISA_PRIMARY está degradado:
   - Como fazer failover para VISA_SECONDARY (se existir)?
   - Se não há secundário: como comunicar ao time de negócio o que está acontecendo?
   - Qual é a ação para as transações que já estão em timeout?

2. **Implemente** (ou descreva em detalhe) `IncidentDashboard.java`:
   Um componente que, durante um incidente, enriquece cada transação com contexto do incidente:
   ```java
   public class IncidentDashboard {
       public void startIncident(String description, String affectedRoute) { /* ... */ }
       public void recordTransactionDuringIncident(ISOMsg msg, String result) { /* ... */ }
       public IncidentSummary endIncident() { /* ... */ }
   }
   ```
   O `IncidentSummary` deve conter:
   - Duração do incidente
   - Transações afetadas (total, aprovadas, negadas, timeouts)
   - Valor total impactado
   - Taxa de aprovação durante vs antes do incidente

3. **RCA preliminar (Root Cause Analysis):** Com base nos dados disponíveis, escreva um RCA de 1 página que o time pode usar como comunicação imediata. (O RCA completo vem depois — este é o "draft inicial".)

### Parte 3 — Prevenção e Maturidade de Observabilidade (10 min)

Avalie o estado atual da observabilidade e proponha melhorias:

1. **O que os alertas atuais cobrem bem?** (O que funcionou neste incidente?)
2. **O que faltou?** O alerta chegou depois que o impacto já estava acontecendo. Que alertas preditivos você adicionaria?
3. **Proposta de 3 novos alertas** que detectariam esse problema 5 minutos antes:
   - Nome do alerta
   - Métrica usada
   - Threshold
   - Ação esperada

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| Hipótese diagnóstica correta e bem fundamentada em métricas | /25 |
| Eliminação das hipóteses alternativas com lógica sólida | /20 |
| Queries de investigação precisas e priorizadas | /15 |
| IncidentDashboard implementado ou descrito corretamente | /20 |
| Alertas preditivos são tecnicamente viáveis e úteis | /20 |

**Meta:** 80+ pontos = Semana 21 dominada.

---

## Dicas

- Um alerta de timeout alto + canal DEGRADED quase sempre aponta para problema de rede ou no host destino. É a hipótese 1.
- "Canal DEGRADED" significa: echo retorna, mas com latência alta. O host Visa está respondendo, mas lentamente.
- 62% de aprovação com 8% de timeout: significa que ~26% das transações Visa estão sendo negadas por outros motivos além do timeout. Isso é incomum e pode indicar problema de sessão/chave.
- Alertas reativos vs preditivos: um alerta quando a taxa já caiu para 62% é tarde. Um alerta quando a latência P50 passa de 200ms (antes de chegar em 1850ms P95) seria preditivo.
- Em produção, o incidente de "taxa de aprovação caiu" é o mais comum e o mais crítico. Aprenda a diagnosticá-lo de olhos fechados.
