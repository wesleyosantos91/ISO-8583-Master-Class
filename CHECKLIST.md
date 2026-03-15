# CHECKLIST — Progresso da Masterclass

Marque `[x]` ao completar cada item. Seja honesto — só marque quando realmente dominar.

---

## Fase 1 — Fundação (Semanas 1-4)

### Semana 1 — Ecossistema de Cartões
- [ ] Li a teoria completa
- [ ] Consigo explicar a jornada POS → Emissor → Resposta sem consultar
- [ ] Sei diferenciar on-us de off-us com exemplos práticos
- [ ] Sei diferenciar dual message de single message
- [ ] Entendo o modelo econômico básico (interchange, MDR)
- [ ] Completei todos os exercícios
- [ ] Entreguei o desafio da semana
- [ ] Criei o diagrama Mermaid da jornada

### Semana 2 — Anatomia ISO 8583
- [ ] Domino a estrutura MTI + Bitmap + Data Elements
- [ ] Consigo decompor um MTI em versão/classe/função/origem
- [ ] Sei ler um bitmap hexadecimal e listar os DEs presentes
- [ ] Completei todos os exercícios
- [ ] Entreguei o desafio (parser manual)

### Semana 3 — Encoding e Formatos
- [ ] Entendo ASCII, BCD, binário na prática
- [ ] Sei a diferença entre LLVAR e LLLVAR
- [ ] Implementei HexUtils, BcdUtils, BitmapUtils
- [ ] Testes unitários passando com cobertura > 90%
- [ ] Completei todos os exercícios
- [ ] Entreguei o desafio (debug de encoding)

### Semana 4 — MTIs e Classes de Mensagens
- [ ] Domino todos os MTIs principais e quando usar cada um
- [ ] Sei explicar request vs response vs advice vs reversal
- [ ] Completei a matriz de mensagens
- [ ] Entreguei o desafio

---

## Fase 2 — jPOS (Semanas 5-8)

### Semana 5 — Setup jPOS + Q2
- [ ] Projeto `payment-switch-lab` rodando
- [ ] Entendo a estrutura Q2 (deploy, cfg, log)
- [ ] README com instruções de setup
- [ ] Completei exercícios

### Semana 6 — ISOMsg + Packager
- [ ] Sei montar e desmontar mensagens com GenericPackager
- [ ] Tenho packager XML customizado
- [ ] Mensagens 0800 e 0200 serializando corretamente
- [ ] Testes unitários cobrindo pack/unpack

### Semana 7 — TransactionManager + Participants
- [ ] Entendo o ciclo prepare/commit/abort
- [ ] Implementei pelo menos 3 participants
- [ ] TransactionManager configurado no deploy XML
- [ ] Context fluindo entre participants

### Semana 8 — QMUX + Correlação
- [ ] QMUX configurado e funcionando
- [ ] Correlação por STAN + Terminal
- [ ] Teste de late response
- [ ] Documento de decisão sobre chave de correlação

---

## Fase 3 — Autorização (Semanas 9-12)

### Semana 9 — Campos Críticos
- [ ] Domino os 19 campos essenciais de auth
- [ ] Sei o papel de cada campo em troubleshooting
- [ ] Validações Java implementadas

### Semana 10 — 0200/0210 E2E
- [ ] Auth request → response funcionando
- [ ] Regras de approve/decline (00, 05, 51, 54, 55)
- [ ] Happy path documentado

### Semana 11 — On-Us/Off-Us + Parcelamento
- [ ] Routing engine por BIN funcionando
- [ ] Decisão on-us vs off-us automática
- [ ] Parcelamento lojista e emissor nos campos corretos
- [ ] 8-digit BIN implementado

### Semana 12 — Timeout e Stand-in
- [ ] Estratégia de timeout documentada com SLAs
- [ ] Simulação de emissor indisponível
- [ ] Tratamento de late response

---

## Fase 4 — Reversal e Advice (Semanas 13-16)

### Semana 13 — Reversões
- [ ] 0400/0410 funcionando
- [ ] Auto-reversal por timeout
- [ ] Matriz de decisão "quando reverter"

### Semana 14 — Advice e Retransmissão
- [ ] Advice vs Response: sei a diferença prática
- [ ] Retransmissão sem duplicidade financeira

### Semana 15 — Network Management
- [ ] 0800/0810 com sign-on, echo, key exchange
- [ ] Health check de canal
- [ ] Bloqueio de envio sem logon

### Semana 16 — Deduplicação e Idempotência
- [ ] Engine de deduplicação funcionando
- [ ] Política de replay documentada
- [ ] Teste: mesma mensagem 2x → mesmo resultado

---

## Fase 5 — EMV, Segurança, Clearing (Semanas 17-20)

### Semana 17 — EMV/Chip/Contactless
- [ ] Parser TLV implementado
- [ ] Tags principais mapeadas
- [ ] Fluxo chip vs fallback documentado

### Semana 18 — HSM, PIN, Segurança
- [ ] PIN Block Format 0 implementado
- [ ] Key hierarchy documentada
- [ ] DUKPT conceitual dominado

### Semana 19 — E-commerce, COF, Tokenização
- [ ] CP vs CNP: campos e diferenças
- [ ] CIT/MIT framework documentado
- [ ] Tokenização network (VTS/MDES) conceitual

### Semana 20 — Clearing e Settlement
- [ ] Ciclo auth → clearing → settlement documentado
- [ ] Mismatches identificados e tratados
- [ ] Reconciliação básica funcionando

---

## Fase 6 — Produção (Semanas 21-24)

### Semana 21 — Observabilidade
- [ ] Métricas por MTI, DE39, rota, latência
- [ ] Logs estruturados com mascaramento
- [ ] Dashboard operacional

### Semana 22 — Troubleshooting
- [ ] Playbook de troubleshooting criado
- [ ] 10+ cenários de falha diagnosticados
- [ ] Catálogo de incidentes

### Semana 23 — Reconciliação Real
- [ ] Clearing file parser
- [ ] Detecção de mismatches auth vs clearing
- [ ] Relatório de exceções

### Semana 24 — Arquitetura de Switch
- [ ] ADRs documentadas
- [ ] Diagrama C4/Mermaid da arquitetura
- [ ] Trade-offs justificados

---

## Fase 7 — Especialização (Semanas 25-28)

### Semana 25 — Mercado Brasileiro
- [ ] Arranjos de pagamento, BACEN, Elo
- [ ] Sub-adquirência e facilitadores
- [ ] Antecipação de recebíveis

### Semana 26 — Fluxos Avançados
- [ ] Pre-auth, incremental, partial approval
- [ ] Balance inquiry
- [ ] Recurring/COF completo

### Semana 27 — Certificação e ISO 20022
- [ ] Processo de certificação Visa/Master documentado
- [ ] Mapeamento ISO 8583 ↔ ISO 20022
- [ ] Test deck conceitual

### Semana 28 — Projeto Final
- [ ] Mini-switch completo e funcional
- [ ] README técnico forte
- [ ] Runbook operacional
- [ ] Apresentação para diferentes audiências
- [ ] Portfólio publicado no GitHub
