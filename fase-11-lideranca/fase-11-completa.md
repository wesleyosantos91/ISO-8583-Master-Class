# Fase 11 — Liderança e Referência de Mercado (Semanas 34-36)

---

# Semana 34 — Comunicação Técnica e Liderança

## 1. RFC (Request for Comments)

Estrutura de uma RFC para mudanças no switch:

```
RFC-001: Migração do Circuit Breaker para Resilience4j

Autor: [seu nome]
Status: Proposta
Data: [data]

## Contexto
O switch atual usa retry manual com Thread.sleep(). Isso causa...

## Proposta
Migrar para Resilience4j com circuit breaker por emissor...

## Alternativas Consideradas
1. Sentinel — descartado porque...
2. Hystrix — deprecated...

## Impacto
- Performance: +15% throughput estimado
- Disponibilidade: de 99.9% para 99.99% estimado
- Risco: médio (precisa de testes de carga)

## Plano de Rollout
1. Implementar em staging (1 semana)
2. Canary com 5% do tráfego (1 semana)
3. Rollout gradual: 25% → 50% → 100%
```

## 2. ADRs (Architecture Decision Records)

```
ADR-001: Usar TreeMap para BINTable

Status: Aceito
Contexto: Precisamos de lookup por prefix match
Decisão: TreeMap com floorKey()
Consequências: O(log n) lookup, não thread-safe (aceitável — read-only após load)

ADR-002: Deduplicação com Caffeine + TTL
ADR-003: STAN como chave de correlação no QMUX
ADR-004: NO_JOIN para AuditLog
ADR-005: ZGC para latência < 1ms
```

## 3. Comunicação para 4 Audiências

| Audiência | Foco | Vocabulário | Formato |
|-----------|------|-------------|---------|
| Devs | Como funciona, trade-offs técnicos | Código, diagramas, benchmarks | RFC/PR description |
| PMs | O que muda para o produto, métricas impactadas | Approval rate, latência, UX | 1-pager com métricas |
| CTO | Risco, custo, timeline, ROI | Disponibilidade, custo/txn | 5 min pitch + 1 slide |
| Regulador | Compliance, segurança, rastreabilidade | PCI, BACEN, audit trail | Documento formal |

---

# Semana 35 — Contribuição e Comunidade

## 1. Contribuição Open Source

- jPOS: bugs, documentação, implementações de packager
- moov-io/iso8583: specs brasileiros, testes, exemplos
- Escrever artigos técnicos sobre ISO 8583 e pagamentos
- Responder perguntas em StackOverflow/GitHub Discussions

## 2. Networking Autêntico

```
Estratégia dos 90 dias:
  Mês 1: Publicar 4 posts técnicos (1/semana) sobre o que aprendeu
  Mês 2: Conectar com 5+ especialistas, comentar conteúdo deles
  Mês 3: Propor palestra em meetup local ou post aprofundado
```

## 3. GitHub como Portfólio

- README que conta uma história (não apenas docs)
- Commits consistentes mostrando evolução
- Issues abertos para roadmap público
- CI verde sempre

---

# Semana 36 — Plano de Carreira e Referência de Mercado

## 1. Níveis de Especialista

```
Nível 1 — Implementador: Sabe usar jPOS, montar mensagens, debugar campos
Nível 2 — Arquiteto: Desenha switches, decide trade-offs, escreve ADRs
Nível 3 — Referência: Outros especialistas consultam, contribui para o ecossistema
Nível 4 — Autoridade: Palestras, publicações, influencia padrões do mercado
```

## 2. Nichos de Especialização

```
Primário (escolha 1):
  → Switch/Core: arquitetura de processamento de transações
  → Antifraude/Risk: ML, velocity rules, incident response
  → Compliance/PCI: certificação, auditoria, segurança
  → Terminal/EMV: firmware, kernel, certificação de hardware

Secundário (escolha 1):
  → Produto: PM técnico de pagamentos
  → Infraestrutura: performance, observabilidade, SRE
  → Dados: reconciliação, analytics, clearing
```

## 3. Roadmap de 12 Meses

```
Meses 1-3:   Consolidar base (revisitar semanas fracas, completar exercícios)
Meses 4-6:   Escolher trilha do proximos-passos.md, implementar projeto público
Meses 7-9:   Contribuir open source, publicar 3+ artigos, networking ativo
Meses 10-12: Propor palestra, revisar carreira, definir próximo nível
```

---

## Checklist de Conclusão da Fase 11

- [ ] RFC escrita para uma melhoria do switch
- [ ] 3+ ADRs documentados
- [ ] Apresentação de 5 min para CTO preparada
- [ ] Code review de PR com bugs de pagamento
- [ ] Primeira contribuição open source feita
- [ ] Post técnico publicado
- [ ] Rede de 5+ contatos no setor
- [ ] Plano de carreira de 12 meses definido
- [ ] payment-switch-lab público e bem documentado
- [ ] Nicho primário e secundário escolhidos
