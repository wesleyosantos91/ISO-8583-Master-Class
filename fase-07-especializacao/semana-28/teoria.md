# Semana 28 — Certificação com Bandeiras e Projeto Final

## 1. Certificação com Bandeiras — O Processo Real

### 1.1 Por que certificar?

Nenhuma instituição pode conectar diretamente ao host da Visa ou Mastercard sem certificação. O processo garante que:
- O sistema implementa corretamente o protocolo da bandeira
- Os campos críticos estão corretos (MTI, bitmaps, encoding)
- Os fluxos de erro são tratados adequadamente
- A segurança mínima está implementada

### 1.2 Tipos de certificação

| Tipo | Para quem | Processo |
|------|-----------|----------|
| Acquirer Host Certification | Adquirente conectando à bandeira | Full test deck |
| Issuer Host Certification | Emissor recebendo da bandeira | Full test deck |
| Terminal Certification | Fabricante de POS/ATM | EMVCo + bandeira |
| Third-party Processor | Processadora intermediária | Depende do arranjo |

### 1.3 Visa — Processo de certificação

```
1. Contato inicial:
   - Preencher formulário de onboarding Visa
   - Designar um Visa Relationship Manager
   - Receber documentação: VIS (Visa International Specification)

2. Self-assessment:
   - Equipe técnica estuda o VIS
   - Implementa os fluxos requeridos
   - Cria ambiente de teste (Visa fornece BINs de teste)

3. Test deck execution:
   - Visa fornece conjunto de cenários obrigatórios
   - Acquirer: ~150-300 cenários
   - Issuer: ~200-400 cenários
   - Cada cenário tem: input esperado, campos obrigatórios, output esperado

4. Evidência e documentação:
   - Logs de cada cenário executado
   - Comparação esperado vs realizado
   - Documentação de qualquer desvio com justificativa

5. Revisão pela Visa:
   - Analista Visa revisa os logs
   - Pode solicitar re-execução de cenários específicos
   - Processo: semanas a meses dependendo de complexidade

6. Letter of Approval (LOA):
   - Visa emite carta de aprovação
   - Válida por versão da spec implementada
   - Mudanças significativas podem exigir recertificação
```

### 1.4 Mastercard — Processo de certificação

```
Similar ao Visa, com terminologia diferente:
  - Documento base: Mastercard Transaction Processing Rules + MDES spec
  - Sistema de testes: MTF (Mastercard Testing Facility)
  - Cenários: MasterCard Test Cases (MTCs)
  - Aprovação: Certificate of Compliance
```

### 1.5 Elo — Certificação brasileira

```
Processo via Elo Serviços S.A.:
  - Documentação: Manual de Especificações Técnicas Elo
  - Ambiente de homologação: Elo fornece endpoint de teste
  - Cenários: validados pelo time técnico da Elo
  - Aprovação: Carta de Homologação Elo
  - Mais ágil que Visa/Master para players brasileiros
```

---

## 2. Test Deck — O que é e como montar

### 2.1 Anatomia de um caso de teste

```
Caso de teste: TC-AUTH-001
  Título:       Compra aprovada — chip, sem PIN, crédito à vista
  Pré-condição: BIN 453201, saldo disponível > R$ 50
  Entrada:
    MTI:    0100
    DE 2:   4532010000000001
    DE 3:   000000
    DE 4:   000000005000  (R$ 50,00)
    DE 22:  051           (chip inserido)
    DE 25:  00
    DE 55:  [dados EMV válidos]
  Saída esperada:
    MTI:    0110
    DE 39:  00
    DE 38:  [qualquer 6 chars alfanum]
    DE 55:  [ARPC válido]
  Critério: Transação aprovada, ARPC gerado, TC gerado pelo chip
```

### 2.2 Categorias de cenários obrigatórios

```
1. AUTORIZAÇÃO APROVADA:
   - Chip + sem PIN
   - Chip + PIN correto
   - Tarja (fallback de chip)
   - Contactless
   - CNP (e-commerce)
   - Débito aprovado
   - Crédito aprovado (à vista)
   - Crédito parcelado

2. AUTORIZAÇÕES NEGADAS:
   - Insufficient funds (DE39=51)
   - Expired card (DE39=54)
   - Do not honor (DE39=05)
   - Wrong PIN (DE39=55)
   - Exceeds withdrawal limit (DE39=61)
   - Card not activated (DE39=57)
   - Fraud decline (DE39=59)

3. ERROS DE PROTOCOLO:
   - MTI inválido
   - Bitmap inconsistente
   - Campo obrigatório ausente
   - Formato de campo inválido
   - Encoding incorreto

4. TIMEOUT E REVERSALS:
   - Timeout no adquirente → reversal
   - Reversal após queda de conexão
   - Late response após reversal enviado
   - Reversal negado (DE39=76 — not found)

5. NETWORK MANAGEMENT:
   - Sign-on bem-sucedido
   - Sign-on recusado
   - Echo test
   - Cutover

6. FLUXOS AVANÇADOS:
   - Pre-authorization + completion
   - Pre-authorization + cancel
   - Partial approval
   - Balance inquiry
   - Cash advance
```

### 2.3 Mini test deck para o payment-switch-lab

Implemente no lab pelo menos estes 20 cenários:

| # | Cenário | MTI | DE39 esperado |
|---|---------|-----|---------------|
| 1 | Compra aprovada chip | 0100/0110 | 00 |
| 2 | Compra aprovada tarja | 0100/0110 | 00 |
| 3 | Compra aprovada CNP | 0100/0110 | 00 |
| 4 | Saldo insuficiente | 0100/0110 | 51 |
| 5 | Cartão expirado | 0100/0110 | 54 |
| 6 | PIN incorreto | 0100/0110 | 55 |
| 7 | Do not honor | 0100/0110 | 05 |
| 8 | Timeout → reversal | 0100 timeout → 0400/0410 | 00 |
| 9 | Reversal not found | 0400/0410 | 76 |
| 10 | Duplicate STAN | 0200/0210 | cached response |
| 11 | Sign-on | 0800/0810 | 00 |
| 12 | Echo test | 0800/0810 | 00 |
| 13 | Campo DE 2 ausente | 0100/0110 | 30 |
| 14 | MTI inválido | - | connection error |
| 15 | Pre-auth | 0100 (DE25=06) | 00 |
| 16 | Pre-auth completion | 0200 (DE25=12) | 00 |
| 17 | Partial approval | 0200/0210 | 10 |
| 18 | Balance inquiry | 0200/0210 DE3=312000 | 00 |
| 19 | On-us routing | 0200 → local issuer | 00 |
| 20 | Off-us routing | 0200 → external | 00 |

---

## 3. Projeto Final — Mini Switch Completo

### 3.1 Visão do entregável

O payment-switch-lab deve ser, ao final desta semana, um sistema que qualquer engenheiro sênior de pagamentos reconheceria como profissional:

```
payment-switch-lab/
├── src/main/java/
│   ├── config/          ← Q2 configurações
│   ├── participants/    ← TransactionParticipants
│   ├── routing/         ← BIN table + routing engine
│   ├── security/        ← PAN masking, PIN handling
│   ├── reconciliation/  ← Reconciliation engine
│   ├── disputes/        ← Dispute automation
│   ├── metrics/         ← SwitchMetrics (Micrometer)
│   └── simulator/       ← Issuer simulator
├── src/test/
│   ├── unit/
│   ├── integration/
│   └── testdeck/        ← Os 20 cenários acima
├── deploy/              ← Q2 XML configs
├── docs/
│   ├── architecture/    ← C4, ADRs
│   ├── runbook/         ← Runbook operacional
│   └── api/             ← Documentação dos campos
└── README.md            ← README de nível sênior
```

### 3.2 README de nível sênior — O que deve conter

```markdown
# Payment Switch Lab

## O que é este projeto
[2-3 parágrafos explicando o que o sistema faz, para quem e por quê]

## Arquitetura
[Diagrama C4 ou Mermaid do sistema]

## Como rodar
[Pré-requisitos, instalação, primeiro start — max 5 comandos]

## Fluxos suportados
[Tabela: MTI, fluxo, campos obrigatórios]

## Test deck
[Como rodar os 20 cenários de teste]

## Configuração
[Como configurar BINs, rotas, timeouts]

## Operação
[Link para runbook, alertas, dashboards]

## Decisões de arquitetura
[Link para ADRs]

## Roadmap
[O que falta / próximos passos]
```

### 3.3 Runbook operacional mínimo

```markdown
# Runbook — Payment Switch Lab

## Alertas e resposta

### ALERT: auth_latency_p95 > 500ms
Causa provável: emissor lento ou timeout de rede
Investigação: verificar logs do QMUX, latência por destino
Ação: escalar timeout ou ativar stand-in

### ALERT: timeout_rate > 1%
Causa provável: emissor instável
Investigação: painel de saúde de canal (0800/0810)
Ação: verificar canal, disparar sign-on, verificar reversal pendentes

### ALERT: reversal_exhausted
Causa provável: emissor indisponível por período prolongado
Investigação: identificar STAN + valor
Ação: intervenção manual — contatar emissor, força exceção financeira

### ALERT: duplicate_detected
Causa provável: terminal com bug de retransmissão
Investigação: terminal ID + volume de duplicatas
Ação: alertar adquirente do terminal, verificar se há cobrança dupla

## Procedimentos de manutenção

### Deploy sem downtime
[Passos de rolling deploy com jPOS]

### Rotação de chaves (ZPK/ZAK)
[Procedimento de key exchange — 0800/DE70=101]

### Backup e recuperação
[Onde ficam os backups, como restaurar]
```

### 3.4 Apresentação para diferentes audiências

Você deve conseguir apresentar o mesmo sistema de formas diferentes:

**Para CTO (5 minutos):**
```
"Construí um switch de pagamentos ISO 8583 em Java com jPOS.
Suporta autorização, reversal, clearing e reconciliação.
Processa em média X TPS com latência P95 de Yms.
Tem 80%+ de cobertura de testes e runbook operacional completo."
```

**Para Engenheiro Sênior (30 minutos):**
- Mostrar arquitetura (C4)
- Explicar pipeline do TransactionManager
- Mostrar como roteamento por BIN funciona
- Demonstrar um cenário de timeout + reversal
- Mostrar métricas no dashboard

**Para Gerente de Produto (15 minutos):**
- O que o sistema faz em termos de negócio
- Quais casos de uso suporta (compra, parcelamento, pré-autorização)
- Como se encaixa no fluxo de aprovação até pagamento ao lojista
- Quais são as limitações atuais

**Para Time de Operações (20 minutos):**
- Como monitorar (dashboard, alertas)
- O que fazer em cada tipo de incidente
- Como investigar uma transação específica pelo RRN
- Como rodar os testes de saúde de canal

---

## 4. Checklist de Conclusão do Curso

### Fundação
- [ ] Consigo explicar a jornada POS → Emissor → resposta sem consultar
- [ ] Sei decompor um MTI e ler um bitmap hexadecimal
- [ ] Entendo encoding ASCII, BCD, binário na prática

### Implementação
- [ ] payment-switch-lab roda com jPOS + Q2
- [ ] TransactionManager com pipeline completo implementado
- [ ] QMUX com correlação e timeout funcionando
- [ ] Autorização 0100/0110 e 0200/0210 E2E
- [ ] Roteamento por BIN (on-us / off-us) implementado
- [ ] Parcelamento nos campos corretos
- [ ] Auto-reversal por timeout funcionando
- [ ] Deduplicação com cache TTL funcionando
- [ ] SAF com drenagem pós-reconexão

### Segurança e Compliance
- [ ] PAN nunca aparece nos logs sem mascaramento
- [ ] Track data e PIN nunca armazenados
- [ ] TLS nas conexões externas
- [ ] Sei explicar os 12 requisitos PCI-DSS
- [ ] Entendo scoping e como reduzir o CDE

### Especialização
- [ ] Domino o mercado brasileiro: Elo, arranjos, BACEN, antecipação
- [ ] Sei implementar pre-auth, incremental, partial approval
- [ ] Entendo o ciclo completo de chargeback e sei quando defender
- [ ] Consigo fazer um pentest básico no meu próprio sistema
- [ ] Sei o que é necessário para certificar com uma bandeira

### Operação
- [ ] Dashboard de métricas (Micrometer/Prometheus)
- [ ] Runbook operacional com os principais alertas
- [ ] Consigo diagnosticar um incidente pelo RRN
- [ ] Documentação de arquitetura (C4 + ADRs)

### Portfólio
- [ ] README de nível sênior no GitHub
- [ ] Consigo apresentar o projeto para CTO em 5 minutos
- [ ] Consigo apresentar para engenheiro sênior em 30 minutos

---

## Resumo do Curso

Você completou 28 semanas (+ fases extras). O que você construiu é real, não acadêmico — é o tipo de sistema que adquirentes e processadoras rodam em produção.

**O próximo passo:** Contribua para o jPOS (open source), escreva sobre o que aprendeu, e apareça nas comunidades de pagamentos. Especialistas de referência no mercado não são apenas bons tecnicamente — eles compartilham conhecimento.
