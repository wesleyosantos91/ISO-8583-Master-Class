# Fase 7 — Especialização (Semanas 25-28)

---

# Semana 25 — Mercado Brasileiro

## 1. Estude e documente

- **Arranjos de pagamento:** Lei 12.865/2013, papel do BACEN
- **Elo:** Quem são os donos (BB, Bradesco, Caixa), specs próprias
- **Hiper/Hipercard:** Bandeira Itaú, processamento on-us
- **Sub-adquirência:** PagSeguro, Mercado Pago, iFood
- **Teto de interchange:** Circular BACEN para débito (0.5%)
- **Antecipação de recebíveis:** O que é, regulação, registradoras (CIP, TAG, CERC)
- **PIX vs Cartão:** Onde competem, onde coexistem

## 2. Exercícios

1. **Desenhe o fluxo** de uma transação sub-adquirente: POS iFood → Stone (adquirente) → Visa → Itaú
2. **Calcule a diferença de receita** entre on-us Hiper vs off-us Visa para o Itaú
3. **Documente 10 diferenças** entre o ecossistema de cartões BR vs EUA

### Desafio
Escreva um artigo técnico (LinkedIn-ready) explicando por que parcelamento sem juros é uma peculiaridade brasileira e como impacta a infraestrutura técnica de pagamentos. Mínimo 800 palavras.

---

# Semana 26 — Fluxos Avançados

## 1. Implemente

### Pre-authorization
```
0100 DE25=06 → Pre-auth R$ 2.000 (hotel check-in)
0200 DE25=06 → Completion R$ 1.500 (checkout)
0400 → Reversal da diferença (se necessário)
```

### Incremental Authorization
```
0100 #1 → Pre-auth R$ 2.000
0100 #2 → Incremental +R$ 500 (referencia #1)
0200 → Completion R$ 2.300
```

### Partial Approval
```
0100 DE4=10000 → Request R$ 100
0110 DE4=7500 DE39=10 → Approved R$ 75 (partial)
Terminal: "Aprovado parcial. Deseja pagar R$ 25 com outro meio?"
```

### Balance Inquiry
```
0100 DE3=300000 → Consulta saldo
0110 DE39=00 DE54=1001986C000001500000 → Saldo R$ 15.000,00
```

## 2. Exercícios

1. **Implemente cada fluxo** no payment-switch-lab
2. **Teste cenário de hotel completo:** check-in → minibar → checkout
3. **Teste partial approval:** terminal lida corretamente com valor reduzido
4. **Implemente balance inquiry** com DE 54

### Desafio
Monte um cenário complexo: locadora de veículos. Pre-auth de R$ 5.000, cliente devolve carro com dano (incremental +R$ 2.000), paga R$ 4.500 no checkout, R$ 2.500 fica como chargeback potencial. Quais mensagens são trocadas?

---

# Semana 27 — Certificação e ISO 20022

## 1. Certificação com Bandeiras

- **Test deck:** Conjunto de ~200-500 cenários de teste
- Cada cenário: "envie esta mensagem, espere esta resposta"
- Inclui: happy path, declines, reversals, timeouts, EMV, contactless, recurring
- **Precisa passar 100%** para ir a produção

## 2. ISO 20022 — O Futuro

- XML/JSON based (vs binário do ISO 8583)
- Visa e Mastercard migrando clearing para ISO 20022
- PIX já é ISO 20022 nativo
- Mapeamento ISO 8583 ↔ ISO 20022 é habilidade valiosa

```
ISO 8583 DE 2 (PAN) → ISO 20022 /AcctId/IBAN ou /Acct/Id/Othr/Id
ISO 8583 DE 4 (Amount) → ISO 20022 /IntrBkSttlmAmt
ISO 8583 DE 39 (Response Code) → ISO 20022 /TxSts
```

## 3. Exercícios

1. **Crie um "mini test deck"** com 30 cenários e implemente runner automático
2. **Documente o processo de certificação** Visa e Mastercard (públicamente disponível)
3. **Mapeie 10 campos** ISO 8583 → ISO 20022

### Desafio
Rode seu mini test deck contra o payment-switch-lab. Quantos cenários passam? Corrija os que falham. Meta: 100%.

---

# Semana 28 — Projeto Final

## Entregáveis

### 1. Mini-switch funcional
- [ ] 0800/0810 (echo, sign-on)
- [ ] 0200/0210 (autorização single message)
- [ ] 0100/0110 (autorização dual message)
- [ ] 0400/0410 (reversal)
- [ ] 0220/0230 (advice)
- [ ] Roteamento por BIN (on-us/off-us)
- [ ] Parcelamento (DE 48/60/63)
- [ ] Deduplicação
- [ ] Auto-reversal por timeout
- [ ] Health check de canais
- [ ] Logs mascarados (PCI)
- [ ] Métricas (latência, volume, RC, timeouts)

### 2. Documentação
- [ ] README técnico completo
- [ ] ARCHITECTURE.md com C4 e ADRs
- [ ] Runbook operacional
- [ ] Catálogo de response codes
- [ ] Playbook de troubleshooting
- [ ] Glossário de 50+ termos
- [ ] Diagrama da jornada end-to-end

### 3. Testes
- [ ] > 80% de cobertura
- [ ] Testes unitários por participant
- [ ] Testes de integração E2E
- [ ] Mini test deck com 30+ cenários
- [ ] Testes de falha (timeout, conexão down, duplicata)

### 4. Apresentação
Prepare apresentação do sistema para:
- [ ] Arquiteto (10 min — decisões técnicas)
- [ ] Head de produto (5 min — valor de negócio)
- [ ] Time de operações (10 min — como monitorar e operar)
- [ ] Desenvolvedor júnior (15 min — como funciona)

### Exercício Final
Resolva este incidente simulado do início ao fim:

> "Às 14:32 de sexta-feira, o monitoring alertou que a taxa de timeout subiu de 0.1% para 15% em transações off-us via Visa. Transações on-us e Mastercard estão normais. O time de negócio está cobrando — é Black Friday e estamos perdendo vendas."

1. Qual sua primeira ação?
2. Que dados pede?
3. Qual seu diagnóstico inicial?
4. Qual a correção?
5. Como prevenir no futuro?
6. Como comunica ao negócio?

Documente tudo como se fosse um RCA (Root Cause Analysis) real.
