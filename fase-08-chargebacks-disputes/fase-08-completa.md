# Fase 8 — Chargebacks e Disputes

## Visão Geral

Esta fase cobre o ciclo completo de chargebacks e disputes — o tema que mais gera custo operacional e litígio no ecossistema de cartões, e que a maioria dos desenvolvedores de pagamentos nunca estudou de forma estruturada.

---

## Por que uma fase extra?

As 28 semanas anteriores cobriram o fluxo **feliz** da transação: autorização, clearing, settlement. Mas o ecossistema real inclui contestações, e entender o chargeback é fundamental para:

- Projetar switches que preservam rastreabilidade completa
- Construir sistemas de antifraude com liability shift em mente
- Suportar operações de adquirência com automação de disputes
- Tomar decisões técnicas (3DS, CVV2, AVS) com impacto financeiro claro

---

## Conteúdo

### Semana 29 — Fundamentos e Ciclo de Vida
- O que é chargeback e como difere de reversal e refund
- Os atores: portador, emissor, bandeira, adquirente, merchant
- Ciclo de vida: disputa → chargeback → representment → pre-arb → arbitration
- Reason codes Visa (10.x, 11.x, 12.x, 13.x) e Mastercard
- Prazos e consequências de não responder
- Chargeback ratio e programas de monitoramento (VDMP, ECP)
- Impacto técnico: DE 37 (RRN), DE 90, retenção de dados
- Friendly fraud vs true fraud

### Semana 30 — Representment, Prevenção e Operação
- Quando e como defender (representment)
- Documentação por reason code
- 3DS e liability shift (ECI values)
- Pre-Arbitration e Arbitration — custos e quando usar
- Ciclo financeiro completo do chargeback
- Prevenção: CVV2/CVC2, AVS, velocity rules, MATCH
- 3DS 2.0 frictionless vs step-up
- Operação em escala: automação, SLAs, dashboard
- Brasil: Elo, BACEN, diferenças regulatórias
- Arquitetura do switch para suportar disputes

---

## Competências ao Final

Após completar esta fase, você consegue:

- [ ] Explicar o ciclo completo de chargeback sem consultar
- [ ] Identificar o reason code correto para qualquer situação
- [ ] Decidir quando defender e quando aceitar um chargeback
- [ ] Implementar lógica de decisão de representment em código
- [ ] Projetar o modelo de dados mínimo para suportar disputes
- [ ] Entender como 3DS muda a liability e quando usar
- [ ] Calcular chargeback ratio e identificar programa de monitoramento
- [ ] Construir automação básica do processo de disputes
- [ ] Aplicar os conceitos no contexto brasileiro (Elo, BACEN)
