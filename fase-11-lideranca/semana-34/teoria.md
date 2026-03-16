# Semana 34 — Comunicação Técnica e Liderança de Decisões

## Por que comunicação é parte de ser especialista?

Especialistas que só sabem conversar com máquinas não são referências de mercado — são bons executores. Referências de mercado **influenciam decisões**, explicam sistemas complexos para pessoas não-técnicas e escrevem documentos que duram anos. Esta semana é sobre as habilidades que multiplicam seu impacto técnico.

---

## 1. Como Escrever uma RFC (Request for Comments)

### 1.1 Quando escrever uma RFC

```
Escreva uma RFC quando:
  - A decisão afeta múltiplas equipes ou sistemas
  - A mudança é difícil de reverter
  - Há trade-offs significativos que precisam de alinhamento
  - A proposta tem implicações de segurança, compliance ou performance

NÃO escreva RFC para:
  - Correções de bug simples
  - Refatorações internas sem impacto externo
  - Mudanças de configuração de baixo risco
```

### 1.2 Template de RFC para sistemas de pagamento

```markdown
# RFC-XXX: [Título da proposta]

**Status:** Draft | In Review | Accepted | Rejected | Implemented
**Autor:** [Nome]
**Data:** YYYY-MM-DD
**Revisores:** [Lista de pessoas que devem revisar]

## Resumo
[1-2 parágrafos explicando o que está sendo proposto e por quê]

## Motivação e Contexto
[Qual problema estamos resolvendo? Por que agora?
Inclua dados: volume, latência, incidentes recentes]

## Proposta Detalhada
[Como a solução funciona? Diagramas, código de exemplo, campos ISO alterados]

## Alternativas Consideradas
[Que outras abordagens foram avaliadas? Por que foram rejeitadas?]

## Impacto
### Performance
[Latência esperada, TPS, uso de memória]
### Segurança
[Riscos de segurança, impacto em PCI-DSS]
### Compatibilidade
[Quebra de API? Requer coordenação com emissores/adquirentes?]
### Operacional
[Mudanças no runbook, novos alertas, treinamento necessário]

## Plano de Implementação
[Fases, dependências, como fazer rollback se necessário]

## Critérios de Sucesso
[Como saberemos que funcionou? Métricas específicas]

## Questões em Aberto
[O que ainda não sabemos / precisa ser decidido]
```

### 1.3 Exemplo real: RFC para implementar 3DS 2.0

```markdown
# RFC-042: Implementar 3DS 2.0 no Gateway de E-commerce

**Status:** In Review

## Resumo
Proposta de implementar 3DS 2.0 no gateway de e-commerce para
transferir liability de fraude CNP do merchant para o emissor,
reduzindo nosso chargeback ratio de 0.8% para < 0.4% estimado.

## Motivação
- Chargeback ratio atual: 0.8% (threshold Visa Standard: 0.9%)
- 89% dos chargebacks CNP são reason code 10.4 (sem 3DS)
- Custo mensal de chargebacks: R$ 340.000
- Com 3DS autenticado (ECI 05), merchant não paga chargeback 10.4

## Proposta
1. Integrar com Visa 3DS Server e Mastercard 3DS Server
2. Fluxo frictionless para transações de baixo risco (ECI 05)
3. Step-up (OTP) para alto risco (ECI 06)
4. Fallback sem 3DS mantido para emissores que não suportam

## Impacto de Performance
- Latência adicional no checkout: +50ms (frictionless) ou +3s (step-up)
- Taxa de conversão esperada: queda de 2% (step-up force)
```

---

## 2. Como Escrever ADRs (Architecture Decision Records)

### 2.1 O que é um ADR

ADR é um documento curto que registra **uma decisão arquitetural**: o contexto, a decisão tomada, as alternativas e as consequências. ADRs são documentos históricos — não se apagam, só se deprecam.

### 2.2 Template ADR

```markdown
# ADR-XXX: [Título da decisão]

**Status:** Proposed | Accepted | Deprecated | Superseded by ADR-YYY
**Data:** YYYY-MM-DD

## Contexto
[Qual era a situação que forçou a decisão?
Descreva as forças em jogo: requisitos técnicos, prazo, custo, compliance]

## Decisão
[O que foi decidido? Seja direto: "Decidimos usar X porque..."]

## Consequências
### Positivas
- [Benefícios da decisão]

### Negativas
- [Trade-offs aceitos, débitos técnicos introduzidos]

### Neutras
- [Impactos que não são claramente bons nem ruins]
```

### 2.3 ADRs essenciais para um switch

```
ADR-001: Escolha do jPOS como framework
ADR-002: Redis para cache de deduplicação (vs in-memory)
ADR-003: Estratégia de timeout: 30s para off-us, 10s para on-us
ADR-004: Chave de correlação: STAN + TerminalID (vs apenas STAN)
ADR-005: Retenção de logs: 13 meses (obrigação PCI-DSS)
ADR-006: Circuit breaker por emissor (vs global)
ADR-007: SAF persistente em banco (vs in-memory)
ADR-008: Mascaramento PAN: 6+4 (vs 4+4)
```

---

## 3. Apresentando para Diferentes Audiências

### 3.1 Para o CTO (5 minutos)

**O que o CTO quer saber:**
- O que o sistema faz (em termos de negócio)
- Por que ele importa (impacto em receita, risco)
- Se está saudável (está dentro dos SLAs?)
- O que precisa de decisão dele

**Template de atualização semanal:**
```
"Switch de pagamentos processou X transações esta semana.
Taxa de aprovação: Y% (meta: >85%). Latência P95: Zms (meta: <150ms).
Um incident na quinta-feira (emissor Banco X indisponível 15min)
foi tratado com stand-in. 3 melhorias entregues: [lista].
Decisão necessária: precisamos de budget para Redis Cluster antes do pico de novembro."
```

### 3.2 Para o time de produto (30 minutos)

**Foco:** O que é possível tecnicamente, o que tem custo, o que tem risco.

```
Pergunta do produto: "Podemos aprovar parcelamento em 24x?"

Resposta técnica estruturada:
  1. O que muda no protocolo: [campos DE 3, DE 48, clearing em 24 parcelas]
  2. O que precisamos implementar: [parser de 24x, 24 registros de clearing]
  3. Dependências: [bandeira precisa suportar, emissor precisa suportar]
  4. Risco: [mais registros de reconciliação, mais chance de mismatch]
  5. Estimativa: [2 sprints para implementar + 1 para certificar]
```

### 3.3 Para times de operações (20 minutos)

**Foco:** Como monitorar, o que fazer quando dá errado.

Apresente sempre com exemplos concretos:
```
"Quando este alerta disparar [mostra print do Grafana],
 significa isso [explica a causa].
 Você deve fazer isso [passo a passo no runbook].
 Isso vai acontecer em situações como [exemplo real]."
```

---

## 4. Code Review em Sistemas de Pagamento

### 4.1 O que revisar diferente de código normal

```
Revisão de código em pagamentos exige atenção especial a:

SEGURANÇA:
  [ ] PAN sendo logado sem mascaramento?
  [ ] Track data / PIN persistido após autorização?
  [ ] TLS na nova conexão?
  [ ] Injection de campos ISO possível?

DADOS FINANCEIROS:
  [ ] Aritmética de dinheiro usando BigDecimal (nunca float/double)?
  [ ] Conversão centavos ↔ reais está correta?
  [ ] Amount pode ser negativo? E se for?

IDEMPOTÊNCIA:
  [ ] Nova operação pode ser chamada duas vezes com o mesmo resultado?
  [ ] Timeout gera retry? O retry é seguro?

TIMEOUT E ERROS:
  [ ] Onde está o timeout desta chamada?
  [ ] O que acontece se o emissor demorar 60s?
  [ ] Qual response code é retornado em cada tipo de erro?

PROTOCOLO:
  [ ] MTI correto para o fluxo?
  [ ] DE 90 (Original Data Elements) presente no reversal?
  [ ] STAN é único por sessão?
```

### 4.2 Dar feedback de forma produtiva

```
❌ "Isso está errado"

✓ "O DE 4 está sendo convertido com movePointLeft(2), mas a spec
   Elo define que o campo já vem em centavos inteiros — não precisa
   dividir por 100. Veja página 43 da spec Elo 3.1."

❌ "Por que você fez assim?"

✓ "Entendo a abordagem — usar float para amounts parece mais simples.
   O problema é que float não representa centavos exatamente
   (0.1 + 0.2 ≠ 0.3 em float). A convenção do mercado é usar
   BigDecimal ou long (centavos inteiros). Qual você prefere?"
```

---

## Resumo da Semana

| Skill | Entregável concreto |
|-------|---------------------|
| RFC | Escreva uma RFC para uma melhoria real no payment-switch-lab |
| ADR | Documente 3 ADRs das decisões mais importantes do seu switch |
| Apresentação para CTO | Prepare e grave (1 take) uma apresentação de 5 minutos |
| Code review | Faça code review de um PR fictício com bugs de pagamento |
