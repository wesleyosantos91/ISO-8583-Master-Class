# Fase 9 — Antifraude e Risco (Semanas 31-32)

---

# Semana 31 — Risk Scoring em Tempo Real

## 1. Fluxo de Decisão de Risco

```
Transação → [Adquirente] → [Antifraude] → [Bandeira] → [Emissor]
                             ↓
                        Velocity rules
                        Device fingerprint
                        Geolocalização
                        Risk score
                             ↓
                      Score < threshold → PASS
                      Score > threshold → BLOCK (DE39=59)
```

## 2. Campos ISO com Informações de Risco

| Campo | Nome | Uso em Antifraude |
|-------|------|-------------------|
| DE 22 | POS Entry Mode | CP vs CNP (051=chip, 010=e-commerce) |
| DE 42 | Merchant ID | Histórico de fraude no merchant |
| DE 43 | Merchant Name/Location | Geolocalização |
| DE 44 | Additional Response Data | Visa Advanced Auth (risk score bandeira) |
| DE 48 | Additional Data | Device fingerprint, 3DS data |
| DE 55 | EMV Data | Criptograma — prova que o chip está presente |

## 3. Velocity Rules com Redis Sorted Sets

```java
// Chave: vel:count:{pan_hash} → Sorted Set (score = timestamp)
// Janela deslizante de 1 hora
ZREMRANGEBYSCORE vel:count:abc123 0 (now - 3600)
ZADD vel:count:abc123 {now} {txn_id}
ZCARD vel:count:abc123 → contagem na janela

// Regras típicas:
// - Max 10 transações/hora por PAN
// - Max R$5.000/hora por PAN
// - Max 3 transações/5min no mesmo terminal
// - Max 5 declines consecutivos → block 24h
```

## 4. FraudScreeningParticipant

Novo participant no TransactionManager, posicionado **antes** do ForwardToIssuer:

```
ValidateMessage → CheckDuplicate → RouteByBIN → FraudScreening → ForwardToIssuer
```

---

# Semana 32 — Operação e Resposta a Incidentes de Fraude

## 1. Tipos de Fraude

### Presencial (CP)
- **Counterfeit**: cartão clonado (tarja magnética)
- **Skimming**: dispositivo no terminal captura dados
- **Shimming**: dispositivo no slot de chip intercepta comunicação

### Card Not Present (CNP)
- **Card testing**: transações de baixo valor para validar cartões roubados
- **ATO (Account Takeover)**: acesso não autorizado à conta
- **Synthetic identity**: identidade fabricada com dados parcialmente reais
- **BIN attack**: enumerar PANs válidos a partir de um BIN

## 2. NegativeListParticipant

```
Listas de bloqueio:
  - PAN list (cartões comprometidos)
  - Terminal list (terminais suspeitos)
  - Merchant list (merchants com alto chargeback)
  - IP list (IPs de fraude conhecidos)
  - BIN list (BINs com padrão de card testing)
```

## 3. Playbook de Resposta a Incidente

1. **Contenção** (0-1h): bloquear PAN/terminal/merchant afetado
2. **Análise** (1-4h): identificar padrão, escopo do comprometimento
3. **Erradicação** (4-24h): atualizar regras, notificar bandeiras
4. **Recuperação** (1-7d): monitoramento intensificado, desbloqueio gradual

## 4. Métricas de Fraude

```
Precision = TP / (TP + FP) → "Das transações que bloquei, quantas eram fraude?"
Recall    = TP / (TP + FN) → "De toda fraude que aconteceu, quanto eu peguei?"

Priorize Precision quando: volume alto, custo de falso positivo é alto (perda de venda)
Priorize Recall quando: fraude é catastrófica (valores altos, reputação)
```

---

## Checklist de Conclusão da Fase 9

- [ ] Entender o fluxo de decisão de risco por ator
- [ ] Implementar velocity rules com Redis sorted sets
- [ ] Implementar FraudScreeningParticipant no TransactionManager
- [ ] Implementar NegativeListParticipant com múltiplas listas
- [ ] Saber identificar tipos de fraude CP e CNP
- [ ] Conhecer o playbook de resposta a incidentes
- [ ] Entender precision vs recall aplicados a antifraude
- [ ] Dashboard de métricas de fraude funcionando
