# Semana 20 — Desafio Integrador

## O Cenário

Você é engenheiro no time de back-office de um adquirente de médio porte. Na manhã de segunda-feira, o CFO envia um e-mail urgente:

> "Nosso time financeiro identificou uma diferença de R$ 187.432,00 entre o que autorizamos e o que recebemos no settlement de sexta-feira. O banco nos pagou R$ 187.432,00 a menos do que esperávamos. Precisamos de um relatório completo até o final do dia."

Dados disponíveis:
- Total de autorizações da sexta: 12.847 transações, R$ 2.341.200,00
- Total no arquivo de clearing enviado pelo adquirente: 12.692 transações, R$ 2.153.768,00
- Total recebido no settlement: R$ 2.153.768,00 (igual ao clearing — OK)
- Diferença: R$ 187.432,00 (no lado da autorização × clearing)

O time financeiro quer saber:
1. A diferença é legítima (mismatches normais) ou indica problema?
2. Há risco financeiro para o adquirente?
3. Algum merchant foi prejudicado?

## Sua Missão

### Parte 1 — Análise da Diferença (30 min)

Escreva `analise-diferenca-s20.md` com:

1. **Decomponha a diferença de R$ 187.432,00.** Explique como ela pode ser composta por diferentes tipos de mismatch:
   - Quantas transações (155 = 12.847 - 12.692) podem ser auths sem clearing
   - Se o valor médio é R$ 182,00, calcule o impacto
   - Qual percentual da diferença pode ser mismatches normais (gorjeta, posto, hotel)?

2. **Classifique os cenários possíveis de alta a baixa preocupação:**
   - 155 auths sem clearing por merchant que não bateu o terminal (void automático = normal)
   - 155 auths sem clearing por problema técnico no arquivo de clearing
   - Amount mismatches legítimos (gorjeta/hotel/posto)
   - Amount mismatches ilegítimos (erro de sistema)

3. **Quais dados você precisaria do banco de dados** para fazer a reconciliação? Escreva as queries (ou pseudocódigo) para:
   - Identificar as 155 transações autorizadas sem correspondência no clearing
   - Calcular o total de amount mismatch e classificar como dentro/fora da tolerância
   - Identificar merchants com maior volume de exceções

4. **Urgência regulatória:** Se 155 transações autorizadas não foram liquidadas, os portadores podem ter o limite bloqueado indevidamente. Qual é o prazo para liberar esses bloqueios? Quem é responsável?

### Parte 2 — Reconciliação Técnica (25 min)

Escreva `reconciliacao-s20.md` com:

1. **Execute o ReconciliationEngine** (do exercício 3) com os dados simulados abaixo e apresente o relatório completo:

```
Autorizações (amostra de 20):
RRN=001 | PAN=****3366 | AMT=15000 | Merchant=RESTAURANTE_A
RRN=002 | PAN=****7890 | AMT=50000 | Merchant=HOTEL_B
RRN=003 | PAN=****1234 | AMT=8500  | Merchant=FARMACIA_C
RRN=004 | PAN=****5678 | AMT=20000 | Merchant=POSTO_D
RRN=005 | PAN=****9012 | AMT=30000 | Merchant=SUPERMERCADO_E
... (15 mais, você define)

Clearing (amostra correspondente):
RRN=001 | AMT=17300  (gorjeta) | Merchant=RESTAURANTE_A
RRN=002 | AMT=45000  (checkout menor) | Merchant=HOTEL_B
RRN=003 | AMT=8500   (igual) | Merchant=FARMACIA_C
-- RRN=004 ausente (posto não capturou)
RRN=005 | AMT=30000 | Merchant=SUPERMERCADO_E
... (mais clearing sem auth, mismatches)
```

2. **Classifique cada exceção** como: normal/aceita, warning, ou error crítico.

3. **Calcule o impacto real:** Qual o valor que o adquirente efetivamente perdeu (se algum) vs qual é diferença explicada por mismatches normais?

### Parte 3 — Comunicação com o CFO (15 min)

O CFO precisa de uma resposta até o final do dia. Escreva dois documentos:

**Para o CFO (`executive-summary-s20.md`):**
- Máximo 1 página
- O que causou a diferença de R$ 187.432,00
- Quanto é "diferença normal" e quanto é problema real
- Qual o impacto financeiro real para o adquirente
- Quais ações estão sendo tomadas

**Para o time financeiro (`financial-team-briefing-s20.md`):**
- Processo de reconciliação completo
- Lista de exceções com severidade
- Prazo para resolver cada categoria
- Como evitar no futuro

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| Análise correta da composição da diferença | /25 |
| Queries/pseudocódigo de investigação precisos | /15 |
| ReconciliationEngine executado com classificação correta | /25 |
| Cálculo correto do impacto real vs mismatches normais | /15 |
| Comunicação adequada para CFO (não-técnico) e time financeiro | /20 |

**Meta:** 80+ pontos = Semana 20 dominada.

---

## Dicas

- R$ 187.432 parece muito, mas 155 transações a R$ 182 médio = R$ 28.210. A diferença maior vem de amount mismatches acumulados. Faça a matemática.
- 155 auths sem clearing é ~1.2% do total. Se a tolerância da operação é 2%, pode ser dentro do normal.
- O CFO quer saber uma coisa: "a empresa perdeu dinheiro ou não?". Responda isso primeiro, depois explique os detalhes.
- Merchants de restaurante: até 20% de variação por gorjeta é normal. Hotel: pode ser qualquer valor menor. Posto: a autorização é sempre uma estimativa.
- Em produção, esse tipo de análise precisa ser automatizada e rodar diariamente. R$ 187k descoberto só na segunda-feira (referente à sexta) já é tarde demais.
