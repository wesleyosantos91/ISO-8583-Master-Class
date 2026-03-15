# Semana 29 — Exercícios de Fixação

## Exercício 1 — Identificando o Tipo de Contestação (Nível: Iniciante)

Para cada cenário abaixo, classifique se é **reversal**, **refund/estorno** ou **chargeback**, e justifique:

1. A conexão TCP caiu durante o envio da resposta de autorização. O switch não confirmou se o emissor aprovou. O adquirente dispara um 0400 automaticamente.

2. O portador comprou um tênis online, recebeu o produto errado e ligou para a loja pedindo devolução. A loja processou a devolução no mesmo sistema.

3. O portador não reconhece uma cobrança de R$ 349,00 de uma loja de eletrônicos que nunca visitou. Ligou para o banco e contestou.

4. O portador comprou uma assinatura de software por R$ 99/mês, cancelou em janeiro, mas foi cobrado em fevereiro e março.

5. O terminal enviou a transação em duplicidade (bug no software do POS). O adquirente identificou e cancelou a segunda cobrança antes do clearing.

**Para cada resposta, indique também:**
- Quem inicia o processo?
- O merchant perde o valor automaticamente?
- Há impacto no chargeback ratio?

---

## Exercício 2 — Reason Codes Visa (Nível: Intermediário)

Associe cada situação ao reason code Visa correto:

| Situação | Reason Code |
|----------|-------------|
| 1. Portador usou cartão com chip, mas o terminal só tinha leitora de tarja. Cartão era clonado. | |
| 2. O merchant cobrou R$ 1.200 mas a autorização foi de R$ 1.000. | |
| 3. O portador comprou passagem aérea, a companhia faliu, voo não aconteceu. | |
| 4. O adquirente enviou o arquivo de clearing 15 dias após a autorização. | |
| 5. A mesma compra de R$ 450 apareceu duas vezes na fatura. | |
| 6. Compra online com cartão clonado — portador nunca esteve no site. | |
| 7. Portador cancelou assinatura de academia mas foi cobrado por mais 3 meses. | |
| 8. Transação processada sem qualquer código de autorização. | |

**Reason codes disponíveis:** 10.1, 10.4, 11.3, 12.1, 12.5, 12.6, 13.1, 13.2, 13.7

---

## Exercício 3 — Ciclo de Vida (Nível: Intermediário)

Uma transação foi realizada em 01/03/2025 (R$ 800,00, crédito, e-commerce).

Monte a linha do tempo completa com as datas limite para cada etapa, considerando:
- Portador tem até 120 dias para contestar
- Emissor tem até 30 dias para enviar o chargeback
- Adquirente tem 30 dias para representment
- Bandeira tem 10 dias para pre-arbitration

Preencha:

| Etapa | Data máxima | O que acontece se o prazo vencer? |
|-------|-------------|----------------------------------|
| Portador contesta | | |
| Emissor emite chargeback | | |
| Adquirente recebe notificação | | |
| Adquirente envia representment | | |
| Pre-Arbitration | | |

---

## Exercício 4 — Chargeback Ratio (Nível: Intermediário)

Um merchant de e-commerce teve o seguinte volume em outubro:

- Total de transações: 8.500
- Total de chargebacks recebidos: 72
- Chargebacks de fraude: 48
- Chargebacks de disputa do portador: 24

Calcule:
1. O chargeback ratio total
2. O fraud chargeback ratio
3. O merchant está no Early Warning, Standard ou Excessive do VDMP?
4. Se o adquirente cobra multa de R$ 50 por chargeback além de 0.5%, quanto o merchant pagará de multa?
5. Que ações preventivas você recomendaria para reduzir os CBs de fraude?

---

## Exercício 5 — Friendly Fraud vs True Fraud (Nível: Avançado)

Para cada situação, determine se é **true fraud** ou **friendly fraud** e qual evidência o merchant deve apresentar na defesa:

**Situação A:**
- Compra de R$ 2.500 em loja de eletrônicos online
- Entrega confirmada com assinatura do próprio portador
- 45 dias depois, portador contesta: "não reconheço esta compra"
- IP da compra é o mesmo IP de outras compras do portador no mesmo site
- Login com senha do portador foi utilizado

**Situação B:**
- Compra de R$ 3.800 em site de viagens
- Portador afirma que o cartão foi roubado na semana anterior
- Boletim de ocorrência registrado 2 dias antes da compra
- 3DS não foi utilizado na transação
- Terminal (loja física): não tinha leitor de chip, usou tarja

**Situação C:**
- Assinatura de streaming R$ 39,90/mês
- Portador cancelou há 2 meses (e-mail de confirmação de cancelamento existe)
- Sistema continuou cobrando por falha no processo de cancelamento

**Para cada situação:**
- True fraud ou friendly fraud?
- O merchant deve disputar ou aceitar o chargeback?
- Qual evidência é mais forte na defesa?
- Qual o resultado provável?

---

## Exercício 6 — Impacto Técnico no Switch (Nível: Avançado)

Analise o seguinte cenário:

Um switch processa 50.000 transações/dia. O DBA percebe que os logs de transação têm retenção de apenas 3 meses e não armazenam o DE 55 (dados EMV).

1. Qual é o impacto disso em chargebacks de fraude (reason code 10.1/10.2)?
2. O que o adquirente perde ao não ter o DE 55?
3. Qual é o prazo mínimo de retenção exigido por Visa e Mastercard?
4. Liste os campos mínimos que um `TransactionRecord` deve persistir para suportar o processo de chargeback.
5. Por que o RRN (DE 37) deve ser único e imutável?

---

## Desafio Integrador

**Contexto:** Você é o engenheiro sênior de um adquirente. O time de operações chegou com o seguinte relatório do mês de setembro:

```
Total transações:          120.000
Total chargebacks:             890
  - Reason code 10.4:          420  (CNP fraud)
  - Reason code 13.2:          180  (cancelled recurring)
  - Reason code 12.6:          150  (duplicate processing)
  - Reason code 11.3:           80  (no authorization)
  - Outros:                     60

Chargeback ratio:            0.74%
Representments enviados:      310
  - Ganhos:                    180  (58%)
  - Perdidos:                  130  (42%)
```

**Sua missão:**
1. Calcule o prejuízo financeiro estimado (assuma ticket médio de R$ 350)
2. Os 150 chargebacks de "duplicate processing" (12.6) indicam que problema técnico?
3. Os 80 de "no authorization" (11.3) — qual falha de processo pode ter causado isso?
4. Proponha 3 melhorias técnicas no switch para reduzir os CBs dos reason codes mais frequentes
5. O adquirente está no programa de monitoramento da Visa? (VDMP)
6. Crie um relatório de 1 página para o diretor de operações explicando a situação e as ações
