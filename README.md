# ISO 8583 Masterclass — Especialista em Pagamentos com Java

```
   ╔══════════════════════════════════════════════════════════════╗
   ║        ISO 8583 MASTERCLASS — PAYMENTS SPECIALIST           ║
   ║                                                              ║
   ║   36 semanas · Java 25 · jPOS · Mercado Brasileiro          ║
   ║   Do zero à referência de mercado em pagamentos             ║
   ╚══════════════════════════════════════════════════════════════╝
```

## O que é esta masterclass?

Um programa de formação completa para se tornar **referência de mercado em mensageria de pagamentos com cartões**, cobrindo ISO 8583, autorização, clearing, settlement, chargebacks, antifraude, performance e o ecossistema brasileiro — tudo implementado em Java com jPOS.

**Não é um curso de "campos ISO".** É uma formação para quem quer ser a pessoa que outros especialistas consultam — entender protocolo, negócio, operação, arquitetura e liderança técnica de forma integrada.

---

## Estrutura

```
./
│
├── README.md                     ← Você está aqui
├── CHECKLIST.md                  ← Acompanhe seu progresso
│
├── fase-01-fundacao/             ← Semanas 1-4:   Ecossistema + Protocolo
├── fase-02-jpos/                 ← Semanas 5-8:   jPOS real com TxnManager
├── fase-03-autorizacao/          ← Semanas 9-12:  Auth E2E + On-Us/Off-Us
├── fase-04-reversal-advice/      ← Semanas 13-16: Reversal + Idempotência
├── fase-05-emv-seguranca/        ← Semanas 17-20: EMV + HSM + Clearing
├── fase-06-producao/             ← Semanas 21-24: Observabilidade + Troubleshooting
├── fase-07-especializacao/       ← Semanas 25-28: Brasil + PCI-DSS + Certificação
├── fase-08-chargebacks-disputes/ ← Semanas 29-30: Chargebacks, Disputes e Representment
├── fase-09-antifraude/           ← Semanas 31-32: Antifraude e Risk Scoring
├── fase-10-performance/          ← Semana  33:    Performance: 5.000+ TPS
├── fase-11-lideranca/            ← Semanas 34-36: Liderança, Carreira e Referência
│
├── referencias/                  ← Glossário, Response Codes, Modelo Econômico
│
└── laboratorio/
    └── payment-switch-lab/       ← Projeto central com esqueleto Java
```

---

## Cada semana contém

| Arquivo | O que é |
|---------|---------|
| `teoria.md` | Explicação detalhada do tema, com diagramas e exemplos |
| `exercicios.md` | Exercícios de fixação progressivos (iniciante → avançado) |
| `desafio.md` | Desafio integrador — simula problema real de produção |

---

## Como usar

1. **Siga a ordem.** Cada semana constrói sobre a anterior.
2. **Leia a teoria primeiro**, sem pressa. Anote dúvidas.
3. **Faça TODOS os exercícios.** Mesmo os que parecem simples — eles fixam conceitos.
4. **Encare o desafio como incidente real.** Cronometre. Documente o raciocínio.
5. **Implemente no `payment-switch-lab`.** Código é a prova do aprendizado.
6. **Marque progresso no CHECKLIST.md.** Accountability é essencial.

---

## Pré-requisitos

- Java 25 instalado
- Maven ou Gradle
- Docker
- IDE (IntelliJ recomendado)
- Git
- Conhecimento sólido de Java (você já tem)
- Vontade de entender bytes, não só abstrações

---

## Níveis de competência

| Nível | Semanas | Você consegue... |
|-------|---------|-------------------|
| **Fundação** | 1-4 | Ler MTI, bitmap, montar mensagens, explicar auth vs clearing |
| **Implementador** | 5-12 | Construir auth E2E, rotear por BIN, tratar timeout |
| **Avançado** | 13-20 | Reversal, EMV, HSM, clearing/settlement, parcelamento |
| **Especialista** | 21-28 | Diagnosticar incidentes, desenhar switch, PCI-DSS, certificação |
| **Expert** | 29-30 | Operar disputes, automação de chargebacks, liability shift |
| **Sênior de Referência** | 31-33 | Antifraude em produção, 5.000+ TPS, circuit breaker distribuído |
| **Referência de Mercado** | 34-36 | RFC/ADR, palestras, contribuição open source, plano de carreira |

---

## Meta final

Ao completar as 36 semanas, você terá:

- [ ] Mini-switch Java com jPOS (TransactionManager + Participants)
- [ ] Fluxos 0100/0110, 0200/0210, 0400/0410, 0800/0810
- [ ] Roteamento por BIN (on-us/off-us)
- [ ] Parcelamento lojista e emissor
- [ ] Deduplicação e idempotência
- [ ] Reversal automático por timeout
- [ ] Logs mascarados (PCI mindset)
- [ ] Observabilidade com métricas por MTI, rota, response code
- [ ] Simulador de emissor e adquirente
- [ ] Documentação de arquitetura (ADRs, C4, Mermaid)
- [ ] Runbook operacional
- [ ] Portfólio público no GitHub
- [ ] Motor de automação de disputes (chargebacks)
- [ ] Domínio de reason codes Visa e Mastercard
- [ ] Implementação de liability shift via 3DS
- [ ] Engine de antifraude com velocity rules e scoring em tempo real
- [ ] Switch otimizado para 5.000+ TPS com circuit breaker
- [ ] RFC e ADRs escritas para decisões arquiteturais reais
- [ ] Contribuição open source (jPOS ou equivalente)
- [ ] Plano de carreira para referência de mercado
