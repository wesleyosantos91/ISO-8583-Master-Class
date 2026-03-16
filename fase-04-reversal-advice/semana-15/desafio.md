# Semana 15 — Desafio Integrador

## O Cenário

Você lidera o time de infraestrutura de um switch de pagamentos que processa transações para 4 bandeiras: Visa, Mastercard, Elo e Hipercard. Cada bandeira tem seu próprio canal TCP (conexão dedicada ao host da bandeira).

Na quarta-feira às 08h45, durante o pico matinal (alta temporada de compras), o time de monitoramento detecta anomalias mas não tem clareza sobre o que está acontecendo. Você recebe 4 alertas simultâneos:

> **ALERTA 1:** `MASTERCARD: echo_latency_p95=820ms (threshold=500ms)`
>
> **ALERTA 2:** `ELO: sign_on_failed após reconexão TCP`
>
> **ALERTA 3:** `VISA-PRIMARY: 0 transações processadas nos últimos 5 minutos (normal: ~200/min)`
>
> **ALERTA 4:** `HIPERCARD: channel_status=UP, mas approval_rate=12% (normal: ~88%)`

São 4 problemas diferentes, simultâneos, durante o pico. O time de negócio já está percebendo a queda nas vendas.

## Sua Missão

### Parte 1 — Triagem e Diagnóstico (30 min)

Escreva `diagnostico-s15.md` com:

1. **Priorize os 4 alertas.** Qual você ataca primeiro e por quê? (Considere: impacto financeiro, urgência, possibilidade de resolução rápida)

2. **Para cada alerta, diagnostique:**
   - **ALERTA 1 (Mastercard degradado):** O que pode causar latência de 820ms no echo? O canal ainda pode processar transações? Qual é o risco?
   - **ALERTA 2 (Elo sign-on falhou):** O que acontece com as transações Elo agora? Estão sendo processadas ou bloqueadas? Por que o sign-on falhou após reconexão?
   - **ALERTA 3 (Visa zero transações):** O canal está UP mas sem tráfego. Quais são as hipóteses? (Dica: sign-on pode ter falhado silenciosamente)
   - **ALERTA 4 (Hipercard canal UP mas 12% aprovação):** Canal saudável mas quase tudo sendo negado. O que isso sugere? (Dica: pense em chave ZPK)

3. **Quais métricas/logs adicionais você precisaria** para confirmar cada diagnóstico? Seja específico (campo, valor esperado, sistema).

### Parte 2 — Resolução Operacional (25 min)

Escreva `resolucao-s15.md` com:

1. **Plano de ação imediata (próximos 15 minutos):**
   - Sequência de ações para cada um dos 4 canais
   - O que é feito manualmente vs automaticamente
   - Qual canal tem ação mais urgente?

2. **Implemente** (ou descreva com pseudocódigo detalhado) a função de recuperação automática:

```java
public class ChannelRecoveryManager {
    /**
     * Executado quando um canal volta de DOWN para UP.
     * Deve: fazer sign-on, verificar se key change é necessário,
     * processar SAF pendente, atualizar status de health check.
     */
    public RecoveryResult performRecovery(String channelName, QMUX mux) {
        // TODO
    }
}
```

3. **Dashboard em tempo real:** Construa (ou descreva em detalhe) um log estruturado que em uma única linha mostre o estado de cada canal a cada 30 segundos. Formato sugerido:
   ```
   [CHANNEL_REPORT] visa=UP/45ms elo=SIGN_ON_PENDING mastercard=DEGRADED/820ms hipercard=UP_ZPK_ISSUE
   ```

### Parte 3 — Arquitetura de Resiliência (15 min)

Escreva `arquitetura-resiliencia.md` propondo:

1. **Canal primário e secundário (failover):** Como o switch deve se comportar quando VISA-PRIMARY está DOWN? Existe um VISA-SECONDARY? Como o failover funciona sem impacto para o portador?

2. **Alertas proativos vs reativos:** Os 4 alertas chegaram simultâneos. Como você poderia ter detectado os problemas 5 minutos antes que impactassem transações? Proponha 3 alertas preditivos.

3. **Automação de recovery:** Para cada um dos 4 cenários deste desafio, indique se o recovery pode ser totalmente automático ou precisa de intervenção humana. Justifique.

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| Priorização correta dos 4 alertas com justificativa | /20 |
| Diagnóstico técnico preciso para cada canal | /25 |
| Plano de ação é concreto e sequenciado | /20 |
| `ChannelRecoveryManager` implementado ou descrito corretamente | /20 |
| Proposta de resiliência arquitetural é viável | /15 |

**Meta:** 80+ pontos = Semana 15 dominada.

---

## Dicas

- O ALERTA 4 (Hipercard canal UP, 12% aprovação) é o mais sutil e o mais importante. Pense em por que o canal pode estar "saudável" mas as transações sendo negadas.
- Sign-on não é apenas protocolo — ele sincroniza estado criptográfico. Se o ZPK mudou durante um downtime, o sign-on é o momento onde a nova chave é estabelecida.
- Em produção, 4 canais com problemas simultâneos durante o pico é comum após uma atualização de sistema ou após um feriado com muito SAF acumulado.
- Cronometre. A equipe de negócios está monitorando e cada minuto de degradação tem impacto medido em vendas perdidas.
