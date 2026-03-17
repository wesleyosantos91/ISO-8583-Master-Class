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
- [ ] Entreguei o desafio

### Semana 6 — ISOMsg + Packager
- [ ] Sei montar e desmontar mensagens com GenericPackager
- [ ] Tenho packager XML customizado
- [ ] Mensagens 0800 e 0200 serializando corretamente
- [ ] Testes unitários cobrindo pack/unpack
- [ ] Completei exercícios
- [ ] Entreguei o desafio

### Semana 7 — TransactionManager + Participants
- [ ] Entendo o ciclo prepare/commit/abort
- [ ] Implementei pelo menos 3 participants
- [ ] TransactionManager configurado no deploy XML
- [ ] Context fluindo entre participants
- [ ] Completei exercícios
- [ ] Entreguei o desafio

### Semana 8 — QMUX + Correlação
- [ ] QMUX configurado e funcionando
- [ ] Correlação por STAN + Terminal
- [ ] Teste de late response
- [ ] Documento de decisão sobre chave de correlação
- [ ] Completei exercícios
- [ ] Entreguei o desafio

---

## Fase 3 — Autorização (Semanas 9-12)

### Semana 9 — Campos Críticos
- [ ] Domino os 19 campos essenciais de auth
- [ ] Sei o papel de cada campo em troubleshooting
- [ ] Validações Java implementadas
- [ ] Completei exercícios
- [ ] Entreguei o desafio

### Semana 10 — 0200/0210 E2E
- [ ] Auth request → response funcionando
- [ ] Regras de approve/decline (00, 05, 51, 54, 55)
- [ ] Happy path documentado
- [ ] Completei exercícios
- [ ] Entreguei o desafio

### Semana 11 — On-Us/Off-Us + Parcelamento
- [ ] Routing engine por BIN funcionando
- [ ] Decisão on-us vs off-us automática
- [ ] Parcelamento lojista e emissor nos campos corretos
- [ ] 8-digit BIN implementado
- [ ] Completei exercícios
- [ ] Entreguei o desafio

### Semana 12 — Timeout e Stand-in
- [ ] Estratégia de timeout documentada com SLAs
- [ ] Simulação de emissor indisponível
- [ ] Tratamento de late response
- [ ] Completei exercícios
- [ ] Entreguei o desafio

---

## Fase 4 — Reversal e Advice (Semanas 13-16)

### Semana 13 — Reversões
- [ ] 0400/0410 funcionando
- [ ] Auto-reversal por timeout
- [ ] Matriz de decisão "quando reverter"
- [ ] Completei exercícios
- [ ] Entreguei o desafio

### Semana 14 — Advice e Retransmissão
- [ ] Advice vs Response: sei a diferença prática
- [ ] Retransmissão sem duplicidade financeira
- [ ] Completei exercícios
- [ ] Entreguei o desafio

### Semana 15 — Network Management
- [ ] 0800/0810 com sign-on, echo, key exchange
- [ ] Health check de canal
- [ ] Bloqueio de envio sem logon
- [ ] Completei exercícios
- [ ] Entreguei o desafio

### Semana 16 — Deduplicação e Idempotência
- [ ] Engine de deduplicação funcionando
- [ ] Política de replay documentada
- [ ] Teste: mesma mensagem 2x → mesmo resultado
- [ ] Completei exercícios
- [ ] Entreguei o desafio

---

## Fase 5 — EMV, Segurança, Clearing (Semanas 17-20)

### Semana 17 — EMV/Chip/Contactless
- [ ] Parser TLV implementado
- [ ] Tags principais mapeadas
- [ ] Fluxo chip vs fallback documentado
- [ ] Completei exercícios
- [ ] Entreguei o desafio

### Semana 18 — HSM, PIN, Segurança
- [ ] PIN Block Format 0 implementado
- [ ] Key hierarchy documentada
- [ ] DUKPT conceitual dominado
- [ ] Completei exercícios
- [ ] Entreguei o desafio

### Semana 19 — E-commerce, COF, Tokenização
- [ ] CP vs CNP: campos e diferenças
- [ ] CIT/MIT framework documentado
- [ ] Tokenização network (VTS/MDES) conceitual
- [ ] Completei exercícios
- [ ] Entreguei o desafio

### Semana 20 — Clearing e Settlement
- [ ] Ciclo auth → clearing → settlement documentado
- [ ] Mismatches identificados e tratados
- [ ] Reconciliação básica funcionando
- [ ] Completei exercícios
- [ ] Entreguei o desafio

---

## Fase 6 — Produção (Semanas 21-24)

### Semana 21 — Observabilidade
- [ ] Métricas por MTI, DE39, rota, latência
- [ ] Logs estruturados com mascaramento
- [ ] Dashboard operacional
- [ ] Completei exercícios
- [ ] Entreguei o desafio

### Semana 22 — Troubleshooting
- [ ] Playbook de troubleshooting criado
- [ ] 10+ cenários de falha diagnosticados
- [ ] Catálogo de incidentes
- [ ] Completei exercícios
- [ ] Entreguei o desafio

### Semana 23 — Reconciliação Real
- [ ] Clearing file parser implementado
- [ ] Detecção de mismatches auth vs clearing
- [ ] Relatório de exceções gerado
- [ ] Completei exercícios
- [ ] Entreguei o desafio

### Semana 24 — Arquitetura de Switch
- [ ] ADRs documentadas (mínimo 5 decisões)
- [ ] Diagrama C4/Mermaid da arquitetura
- [ ] Trade-offs justificados por escrito
- [ ] Completei exercícios
- [ ] Entreguei o desafio

---

## Fase 7 — Especialização (Semanas 25-28)

### Semana 25 — Mercado Brasileiro Profundo
- [ ] Entendo Lei 12.865/2013 e arranjos de pagamento
- [ ] Sei as diferenças técnicas do Elo vs Visa/Mastercard
- [ ] Entendo antecipação de recebíveis e registradoras (CIP/CERC/TAG)
- [ ] Conheço o teto de interchange BACEN (0.5% débito)
- [ ] Entendo Open Finance e DREX em contexto de pagamentos
- [ ] Entendo o que é PIX com cartão de crédito (PIX Garantido/PIX Crédito) e por que surgiu (Res. BCB nº 195/2022)
- [ ] Sei diferenciar as modalidades: PIX Crédito à vista, PIX Parcelado sem juros, PIX Parcelado com juros
- [ ] Consigo desenhar o fluxo dual-leg (Leg 1: ISO 8583 auth; Leg 2: PIX/SPI liquidação)
- [ ] Sei quais campos ISO 8583 carregam a chave DICT, txid e endToEndId (DE3, DE48, DE63)
- [ ] Entendo o modelo econômico: MDR, interchange e IOF no PIX crédito vs PIX tradicional
- [ ] Sei como chargebacks funcionam no PIX crédito e por que são mais complexos que no PIX tradicional
- [ ] Entendo o risco de idempotência dual-leg e como mitigá-lo no switch
- [ ] Implementei `PixCreditDetector` com identificação por DE48 e extração de chave/txid
- [ ] Completei exercícios
- [ ] Entreguei o desafio

### Semana 26 — Fluxos Avançados
- [ ] Pre-authorization (DE25=06/12) implementada
- [ ] Incremental authorization (DE25=10) dominada
- [ ] Partial approval (DE39=10) com split tender
- [ ] Balance inquiry (DE3=312000, DE54) funcionando
- [ ] MIT/COF framework documentado (tipos 01-08)
- [ ] Completei exercícios
- [ ] Entreguei o desafio

### Semana 27 — PCI-DSS Prático
- [ ] Sei diferenciar CHD de SAD (o que nunca armazenar)
- [ ] Entendo os 12 requisitos PCI-DSS 4.0
- [ ] Sei fazer CDE scoping com tokenização e segmentação
- [ ] Conheço os tipos de SAQ e quando cada um se aplica
- [ ] PAN masking implementado em todos os logs
- [ ] Completei exercícios
- [ ] Entreguei o desafio

### Semana 28 — Certificação e Projeto Final
- [ ] Entendo o processo de certificação Visa (6 etapas)
- [ ] Conheço as categorias de test deck obrigatórias
- [ ] payment-switch-lab com README de nível sênior
- [ ] Runbook operacional com alertas e procedimentos
- [ ] Apresentação para 4 audiências diferentes preparada
- [ ] Portfólio publicado no GitHub
- [ ] Completei exercícios
- [ ] Entreguei o desafio

---

## Fase 8 — Chargebacks e Disputes (Semanas 29-30)

### Semana 29 — Fundamentos e Ciclo de Vida
- [ ] Sei diferenciar reversal, refund e chargeback sem hesitar
- [ ] Conheço os reason codes Visa (10.x, 11.x, 12.x, 13.x) e os principais Mastercard
- [ ] Entendo o ciclo completo: disputa → CB → representment → pre-arb → arbitration
- [ ] Sei calcular e interpretar o chargeback ratio
- [ ] Entendo o EMV liability shift e como 3DS muda a responsabilidade
- [ ] Sei quais campos ISO precisam ser preservados para suportar disputes
- [ ] Completei todos os exercícios
- [ ] Entreguei o desafio do pico de chargebacks

### Semana 30 — Representment, Prevenção e Operação
- [ ] Sei decidir quando defender e quando aceitar um chargeback
- [ ] Conheço as evidências necessárias por reason code
- [ ] Entendo CVV2, AVS e velocity rules como ferramentas preventivas
- [ ] Implementei DisputeEvaluator com lógica de decisão automática
- [ ] Implementei DisputeOrchestrator com tracking de prazos
- [ ] Testes unitários cobrindo os cenários de decisão
- [ ] Completei todos os exercícios
- [ ] Entreguei o Motor de Disputes (desafio integrador)

---

## Fase 9 — Antifraude e Risco (Semanas 31-32)

### Semana 31 — Risk Scoring em Tempo Real
- [ ] Entendo o fluxo de decisão de risco (adquirente → antifraude → bandeira → emissor)
- [ ] Sei quais campos ISO carregam informações de risco (DE22, DE42, DE43, DE44, DE48, DE55)
- [ ] Implementei velocity rules com Redis sorted sets
- [ ] Entendo device fingerprint e geolocalização como sinais de risco
- [ ] Implementei FraudScreeningParticipant no TransactionManager
- [ ] Entendo Visa Advanced Authorization (DE44) e seus campos
- [ ] Completei todos os exercícios
- [ ] Entreguei o desafio de antifraude

### Semana 32 — Operação e Resposta a Incidentes
- [ ] Sei identificar os tipos de fraude presencial (counterfeit, skimming, shimming)
- [ ] Sei identificar os tipos de fraude CNP (card testing, ATO, synthetic identity, BIN attack)
- [ ] Entendo fraud rings e seus padrões de sinal
- [ ] Implementei dashboard de métricas de fraude
- [ ] Implementei NegativeListParticipant com múltiplas listas
- [ ] Conheço o playbook de resposta a incidente de fraude (4 etapas)
- [ ] Entendo precision vs recall e quando priorizar cada um
- [ ] Completei todos os exercícios
- [ ] Entreguei o desafio de incidente de fraude

---

## Fase 10 — Performance e Escala (Semana 33)

### Semana 33 — 5.000+ TPS em Produção
- [ ] Conheço os SLAs de referência (P50, P95, P99 on-us/off-us)
- [ ] Sei decompor a latência de uma transação por componente
- [ ] Apliquei tuning no jPOS (minSessions, maxSessions, max-outstanding)
- [ ] Configurei ZGC para baixa latência (pauses < 1ms)
- [ ] Implementei ISOLoadTester com RateLimiter
- [ ] Conheço os 5 cenários obrigatórios de load test
- [ ] Implementei circuit breaker com Resilience4j por emissor
- [ ] Entendo os 4 desafios do horizontal scaling (dedup, QMUX, SAF, conexões)
- [ ] Completei todos os exercícios
- [ ] Entreguei o desafio de performance

---

## Fase 11 — Liderança e Referência de Mercado (Semanas 34-36)

### Semana 34 — Comunicação Técnica e Liderança
- [ ] Escrevi uma RFC completa para uma melhoria do switch
- [ ] Documentei 3+ ADRs das decisões mais importantes
- [ ] Preparei apresentação de 5 minutos para CTO
- [ ] Fiz code review de PR com bugs de pagamento e dei feedback produtivo
- [ ] Sei adaptar comunicação técnica para 4 audiências diferentes
- [ ] Completei todos os exercícios
- [ ] Entreguei o desafio de liderança técnica

### Semana 35 — Contribuição e Comunidade
- [ ] Fiz minha primeira contribuição ao jPOS (ou projeto equivalente)
- [ ] Publiquei meu primeiro post técnico sobre pagamentos
- [ ] Participei ativamente de pelo menos 1 comunidade online
- [ ] Conectei com 5+ especialistas de forma autêntica
- [ ] LinkedIn strategy dos 90 dias em andamento
- [ ] GitHub com atividade consistente e README que conta uma história
- [ ] Completei todos os exercícios
- [ ] Entreguei o desafio de contribuição

### Semana 36 — Plano de Carreira e Referência
- [ ] Identifiquei em qual nível estou hoje (1-4) e o que preciso para o próximo
- [ ] Escolhi meu nicho primário e secundário de especialização
- [ ] Criei meu roadmap pessoal de 12 meses
- [ ] Redigi minha narrativa profissional forte (não fraca)
- [ ] Defini meu projeto de legado
- [ ] payment-switch-lab público e bem documentado
- [ ] Completei todos os exercícios
- [ ] Entreguei o plano de carreira (desafio final)

---

## Meta Final — Você é Referência de Mercado quando:

- [ ] Outros especialistas te consultam sobre ISO 8583 e pagamentos
- [ ] Tens histórico público de contribuição (GitHub, blog, palestras)
- [ ] Sabes comunicar decisões técnicas para qualquer audiência
- [ ] Tens rede de 50+ contatos no setor de pagamentos
- [ ] Conheces o ecossistema brasileiro de ponta a ponta
- [ ] Teu switch em produção (ou pronto para produção) é referência pública
