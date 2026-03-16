# Semana 31 — Antifraude: Risk Scoring e Detecção em Tempo Real

## Por que antifraude é parte do currículo de ISO 8583?

Porque a decisão de autorizar ou negar uma transação **não é apenas do emissor**. O switch do adquirente, o gateway, o sistema antifraude e o emissor colaboram em milissegundos para tomar essa decisão. Entender onde cada componente atua e como o ISO 8583 carrega os dados de risco é fundamental para quem projeta switches de produção.

---

## 1. A Anatomia de uma Decisão de Risco

```
t=0ms   Terminal captura dados do cartão

t=1ms   Adquirente:
          - Velocity check básico (mesmo cartão, múltiplos terminais?)
          - BIN validation (BIN existe? País bate com localização?)
          - MCC check (tipo de merchant suspeito?)

t=5ms   Sistema Antifraude do adquirente (se existir):
          - Score de risco baseado em regras + ML
          - Device fingerprint (e-commerce)
          - Geolocalização (IP vs BIN emissor)
          - Comportamento histórico do cartão

t=10ms  Bandeira (switch):
          - Fraud screening próprio (Visa Advanced Authorization, Mastercard Safety Net)
          - Neural network score no DE 44/48 da response

t=15ms  Emissor:
          - Score próprio de fraude
          - Validação ARQC (chip)
          - Regras de comportamento (novo país, valor atípico)
          - Decisão final: approve / decline / code 10 (call center)
```

---

## 2. Campos ISO 8583 Relevantes para Antifraude

### 2.1 Campos que o antifraude consome

| Campo | Dado | Uso no risco |
|-------|------|-------------|
| DE 2 | PAN | Histórico do cartão |
| DE 4 | Amount | Valor atípico para o portador? |
| DE 12/13 | Data/Hora local | Fuso horário coerente com localização? |
| DE 22 | POS Entry Mode | Chip (menor risco) vs tarja (maior risco) vs CNP (maior risco) |
| DE 25 | POS Condition Code | Operação não assistida? Força de venda? |
| DE 35 | Track 2 | Dados completos de tarja (risco se não for presencial) |
| DE 41 | Terminal ID | Terminal conhecido? Na lista negra? |
| DE 42 | Merchant ID | Merchant com alto chargeback ratio? |
| DE 43 | Merchant Name/Location | País do merchant bate com portador? |
| DE 48 | Private Data | Device fingerprint, 3DS data, scores |
| DE 55 | EMV Data | ARQC, ATC, TVR — validação do chip |
| DE 61 | POS Data | Informações adicionais do terminal |

### 2.2 Visa Advanced Authorization (VAA)

A Visa transmite seu score de risco na response da autorização:

```
DE 44 (Additional Response Data) — na 0110:
  Score: 00-99 (99 = altíssimo risco)
  Reason codes: indica por que o score é alto
    01 = Unusual transaction
    02 = Possible stolen card
    03 = Unacceptable account
    04 = Suspicious transaction
    ...

O emissor pode usar esse score para:
  - Score baixo (<40): aprovar automaticamente
  - Score médio (40-70): aplicar regras adicionais
  - Score alto (>70): declinar ou pedir autenticação extra
```

### 2.3 Mastercard Safety Net

```
Mastercard Safety Net — sistema de stand-in com antifraude:
  Quando emissor está fora do ar:
    - Mastercard processa a transação por conta própria
    - Aplica seu próprio modelo de risco
    - Aprova ou nega baseado em histórico de transações do emissor
    - DE 44 carrega o resultado
```

---

## 3. Velocity Rules — Implementação Completa

### 3.1 O que são velocity rules

Regras que contam eventos em janelas de tempo para identificar padrões suspeitos:

```
"Mais de 3 transações do mesmo cartão em 5 minutos"
"Mais de 5 cartões diferentes do mesmo IP em 1 hora"
"Valor total > R$ 10.000 do mesmo cartão em 24 horas"
```

### 3.2 Implementação com Redis

```java
@Component
public class VelocityEngine {

    private final RedisTemplate<String, String> redis;

    // Conta eventos em janela deslizante usando sorted set
    private long countInWindow(String key, long windowSeconds) {
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000);

        // Remove eventos fora da janela
        redis.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // Adiciona evento atual
        redis.opsForZSet().add(key, String.valueOf(now), now);
        redis.expire(key, Duration.ofSeconds(windowSeconds * 2));

        // Retorna contagem na janela
        return redis.opsForZSet().count(key, windowStart, now);
    }

    public VelocityResult check(String pan, String ip, String terminalId, BigDecimal amount) {
        List<VelocityViolation> violations = new ArrayList<>();

        // Regra 1: Mesmo cartão, máximo 5 transações em 10 minutos
        long cardCount = countInWindow("vel:card:" + pan, 600);
        if (cardCount > 5) {
            violations.add(new VelocityViolation("CARD_VELOCITY_10MIN", cardCount, 5));
        }

        // Regra 2: Mesmo IP, máximo 10 transações em 1 hora
        if (ip != null) {
            long ipCount = countInWindow("vel:ip:" + ip, 3600);
            if (ipCount > 10) {
                violations.add(new VelocityViolation("IP_VELOCITY_1H", ipCount, 10));
            }
        }

        // Regra 3: Mesmo terminal, máximo 3 cartões diferentes em 5 minutos
        String terminalKey = "vel:term:" + terminalId;
        redis.opsForSet().add(terminalKey, pan);
        redis.expire(terminalKey, Duration.ofMinutes(5));
        long uniqueCards = redis.opsForSet().size(terminalKey);
        if (uniqueCards > 3) {
            violations.add(new VelocityViolation("TERMINAL_MULTI_CARD", uniqueCards, 3));
        }

        // Regra 4: Mesmo cartão, total > R$ 5.000 em 24h
        String amountKey = "vel:amt:" + pan;
        String currentTotal = redis.opsForValue().get(amountKey);
        BigDecimal total = currentTotal != null ? new BigDecimal(currentTotal) : BigDecimal.ZERO;
        total = total.add(amount);
        redis.opsForValue().set(amountKey, total.toPlainString(), Duration.ofHours(24));
        if (total.compareTo(new BigDecimal("5000")) > 0) {
            violations.add(new VelocityViolation("AMOUNT_VELOCITY_24H", total.longValue(), 5000));
        }

        return new VelocityResult(violations.isEmpty() ? RiskLevel.LOW : RiskLevel.HIGH, violations);
    }
}
```

---

## 4. Device Fingerprint e Geolocalização (E-commerce)

### 4.1 Device fingerprint

Em e-commerce, o device fingerprint é a impressão digital do dispositivo do comprador:

```
Componentes típicos:
  - User Agent (navegador, versão, OS)
  - Idioma do navegador
  - Timezone
  - Resolução de tela
  - Fonts instaladas
  - WebGL fingerprint
  - Canvas fingerprint
  - Plugins do navegador
  - IP address

Como usar no ISO 8583:
  DE 48, subelemento proprietário do adquirente:
    Device hash: "d8e8fca2dc0f896fd7cb4cb0031ba249"
    IP: "187.23.45.67"
    Country: "BR"
    ASN: "AS7738" (Telemar/Oi)
```

### 4.2 Geolocalização como sinal de risco

```
Sinais de alerta geográfico:
  1. IP em país diferente do BIN do emissor
     BIN: Nubank Brasil → IP: Nigéria → ALTO RISCO

  2. Velocidade impossível
     Compra em São Paulo às 14h → Compra em Buenos Aires às 14h30
     (fisicamente impossível mesmo de avião)

  3. IP de proxy/VPN/Tor
     Portador escondendo localização real

  4. IP de datacenter
     Compra sendo feita por bot, não por humano
```

---

## 5. Modelos de Machine Learning em Fraude

### 5.1 Onde ML entra no fluxo

```
Real-time (< 30ms):
  Features pré-calculadas (velocity, histórico recente)
  Modelo leve: Gradient Boosting (XGBoost, LightGBM)
  Output: score 0-100

Near real-time (30ms - 2s):
  Features calculadas na hora (device, geo, behavioral)
  Modelo mais pesado: Neural network
  Output: score + reason codes

Batch (diário):
  Análise de padrões históricos
  Re-treinamento do modelo
  Identificação de novas campanhas de fraude
```

### 5.2 Features mais importantes para cartões

```
Features de cartão:
  - Dias desde último uso
  - Frequência de uso (transações/semana)
  - Valor médio histórico
  - Variação do valor atual vs média (zscore)
  - Países de uso histórico
  - MCCs históricos

Features de terminal:
  - Chargeback ratio do terminal
  - Idade do terminal no sistema
  - Quantidade de cartões únicos/dia

Features de comportamento:
  - Hora do dia vs padrão histórico
  - Dia da semana vs padrão histórico
  - Distância da última transação
```

### 5.3 O problema do false positive

```
False positive: transação legítima negada por erro do antifraude
  - Portador viajando (primeiro uso em país novo)
  - Compra de valor atípico (presente, emergência)
  - Novo cliente sem histórico

Custo do false positive:
  - Portador frustrado → abandona o cartão
  - Merchant perde a venda
  - Emissor perde receita de interchange

Métrica: False Positive Rate (FPR)
  Meta típica: < 0.5% das transações legítimas negadas incorretamente
  Tradeoff: menor FPR = maior fraude passa; menor fraude = mais falsos positivos
```

---

## 6. Integração Antifraude no Switch jPOS

```java
public class FraudScreeningParticipant implements TransactionParticipant {

    private final VelocityEngine velocityEngine;
    private final FraudScoreService fraudScoreService;

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg req = ctx.get("REQUEST");

        try {
            String pan = req.getString(2);
            BigDecimal amount = new BigDecimal(req.getString(4)).movePointLeft(2);
            String ip = extractIp(req); // De DE 48 se e-commerce
            String terminalId = req.getString(41);

            // 1. Velocity check (< 2ms com Redis)
            VelocityResult velocity = velocityEngine.check(pan, ip, terminalId, amount);
            if (velocity.riskLevel() == RiskLevel.CRITICAL) {
                ctx.put("RESPONSE_CODE", "59"); // Fraud decline
                return ABORTED;
            }

            // 2. Score do modelo (< 20ms)
            FraudScore score = fraudScoreService.score(req, velocity);
            ctx.put("FRAUD_SCORE", score.value());

            if (score.value() > 85) {
                ctx.put("RESPONSE_CODE", "59");
                return ABORTED;
            }

            if (score.value() > 60) {
                // Score médio: envia para bandeira mas marca para revisão
                ctx.put("RISK_FLAG", "REVIEW");
            }

            return PREPARED;

        } catch (Exception e) {
            // NUNCA deixar o antifraude bloquear transações por erro
            log.error("Fraud screening error — allowing transaction", e);
            return PREPARED;
        }
    }
}
```

---

## Resumo da Semana

| Conceito | O que você deve dominar |
|----------|------------------------|
| Fluxo de risco | Adquirente → Antifraude → Bandeira → Emissor em cascata |
| Campos ISO relevantes | DE 22, 42, 43, 44, 48, 55 como fontes de sinal |
| Visa Advanced Authorization | Score na response, DE 44, reason codes |
| Velocity rules | Redis sorted sets para janelas deslizantes |
| Device fingerprint | Componentes, como passar no DE 48 |
| ML no fluxo | Score em < 30ms, features, false positive tradeoff |
| Integração jPOS | FraudScreeningParticipant sem bloquear por erros |
