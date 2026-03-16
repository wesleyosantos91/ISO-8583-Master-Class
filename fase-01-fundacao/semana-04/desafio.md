# Semana 4 — Desafio Integrador

## O Cenário

Você entrou no time de pagamentos de uma fintech que está construindo um gateway. O tech lead pede:

> "Precisamos de um simulador de conversa ISO 8583. Quero poder ver no terminal a troca de mensagens entre adquirente e host, com diferentes cenários — happy path, timeout, reversal. Isso vai ajudar o time inteiro a entender o protocolo antes de integrar com a bandeira."

## Sua Missão

### Parte 1 — Simulador de Conversa (60 min)

Implemente um programa Java que simula a troca de mensagens entre terminal (adquirente) e host (switch/emissor):

**Cenários obrigatórios:**

1. **Echo Test (Network Management)**
   - Terminal envia 0800 (echo request)
   - Host responde 0810 com DE39=00

2. **Compra aprovada (Happy Path)**
   - Terminal envia 0200 com PAN, Amount, STAN, Terminal
   - Host responde 0210 com DE39=00, DE38=AUTH_CODE

3. **Compra negada por saldo insuficiente**
   - Terminal envia 0200
   - Host responde 0210 com DE39=51, sem DE38

4. **Timeout do emissor + Reversal**
   - Terminal envia 0200
   - [30s timeout — sem resposta]
   - Terminal envia 0400 (reversal) com DE90 referenciando o request original
   - Host responde 0410 com DE39=00

5. **Retransmissão**
   - Terminal envia 0200
   - [timeout]
   - Terminal envia 0201 (repeat)
   - Host responde 0210

**Formato de saída:**

```
══════════════════════════════════════════════════════════
  ISO 8583 Message Flow Simulator
══════════════════════════════════════════════════════════

[14:30:25.001] TERMINAL → HOST    0800  STAN=000001  (Echo Request)
[14:30:25.045] HOST → TERMINAL    0810  STAN=000001  DE39=00  (Echo OK)

[14:30:26.001] TERMINAL → HOST    0200  STAN=123456  PAN=4532****0366  AMT=R$150,00
[14:30:26.150] HOST → TERMINAL    0210  STAN=123456  DE39=00  AUTH=A1B2C3  (Approved)

[14:30:27.001] TERMINAL → HOST    0200  STAN=789012  PAN=5412****5678  AMT=R$2.500,00
[14:30:27.098] HOST → TERMINAL    0210  STAN=789012  DE39=51  (Insufficient Funds)

[14:30:28.001] TERMINAL → HOST    0200  STAN=345678  PAN=4532****0366  AMT=R$300,00
[14:30:58.001] [TIMEOUT — 30s sem resposta]
[14:30:58.005] TERMINAL → HOST    0400  STAN=345679  DE90=0200+345678  (Auto-Reversal)
[14:30:58.050] HOST → TERMINAL    0410  STAN=345679  DE39=00  (Reversal OK)
```

### Parte 2 — Documentação (30 min)

Crie um `README.md` para o simulador explicando:

1. O que cada cenário demonstra
2. Quais campos são obrigatórios em cada MTI
3. Quais campos são copiados do request para o response
4. A diferença entre request (0200) e repeat (0201)
5. Por que o reversal é necessário após timeout

## Requisitos Técnicos

- Use `ISOMsg` do jPOS para montar as mensagens
- PAN deve ser mascarado na saída (PCI mindset desde o dia 1)
- STAN deve ser incrementado sequencialmente
- Cada mensagem deve ter timestamp real
- Amount formatado em reais (R$ X.XXX,XX)

## Critério de Avaliação

| Item | Peso |
|------|------|
| 5 cenários funcionando corretamente | 40% |
| Saída formatada e legível | 15% |
| PAN mascarado em toda saída | 15% |
| Documentação clara e completa | 20% |
| Código limpo e organizado | 10% |

**Tempo total:** 90 minutos
