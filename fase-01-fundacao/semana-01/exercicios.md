# Semana 1 — Exercícios de Fixação

## Exercício 1 — Mapeamento de Atores (Nível: Iniciante)

Preencha a tabela abaixo sem consultar a teoria:

| Ator | Função principal | Exemplo no Brasil | Com quem tem contrato? |
|------|-----------------|-------------------|----------------------|
| Portador | | | |
| Merchant | | | |
| Adquirente | | | |
| Emissor | | | |
| Bandeira | | | |
| Processadora | | | |

**Critério de sucesso:** Preencheu tudo corretamente, sem consultar.

---

## Exercício 2 — Jornada da Transação (Nível: Iniciante)

Escreva, com suas palavras (sem copiar), o que acontece em cada etapa quando um portador compra um café de R$ 8,50 com cartão Mastercard emitido pelo Nubank, numa maquininha Stone:

1. O que o terminal faz?
2. Para onde o terminal envia a mensagem?
3. O que a Stone (adquirente) faz?
4. Para onde a Stone roteia?
5. O que a Mastercard (bandeira) faz?
6. O que o Nubank (emissor) verifica antes de aprovar?
7. O que acontece na volta (resposta)?
8. O que acontece no dia seguinte (clearing)?
9. Quando o dinheiro efetivamente muda de mãos?

**Critério de sucesso:** Conseguiu descrever todas as 9 etapas com precisão técnica.

---

## Exercício 3 — On-Us ou Off-Us? (Nível: Intermediário)

Classifique cada cenário como **on-us** ou **off-us** e justifique:

1. Cartão Itaú Visa passado na maquininha Rede
2. Cartão Bradesco Mastercard passado na maquininha Cielo
3. Cartão Hipercard passado em terminal Itaú
4. Cartão Nubank Mastercard passado na maquininha Stone
5. Cartão Elo Caixa passado na maquininha do Banco do Brasil
6. Cartão Itaú Visa passado na maquininha Stone

**Para cada resposta, indique:**
- É on-us ou off-us?
- Por quê?
- A mensagem passa pela bandeira?
- Quem paga interchange?

---

## Exercício 4 — Dual vs Single Message (Nível: Intermediário)

Para cada cenário, defina se o fluxo é **dual message** ou **single message** e explique por quê:

1. Compra de supermercado com cartão de débito
2. Check-in de hotel com cartão de crédito (pré-autorização de R$ 2.000)
3. Saque de R$ 500 no ATM com cartão de débito
4. Compra online de R$ 150 com crédito
5. Pagamento em restaurante com gorjeta (R$ 80 + R$ 15 gorjeta)
6. Assinatura mensal de streaming R$ 39,90

**Para cada um:**
- Dual ou Single?
- Qual MTI é usado na autorização?
- Existe captura separada?
- O valor da captura pode ser diferente da autorização?

---

## Exercício 5 — Calculando Taxas (Nível: Intermediário)

Um restaurante vende R$ 10.000,00 em um dia. Composição:
- 40% débito (MDR 1.2%)
- 35% crédito à vista (MDR 2.8%)
- 25% crédito parcelado 3x (MDR 3.5%)

Calcule:
1. Quanto o merchant recebe líquido no total?
2. Quanto cada tipo de transação gerou de MDR?
3. Se o interchange de débito é 0.5%, crédito à vista 1.5% e parcelado 2.0%, quanto o emissor recebeu?
4. Se o network fee é 0.1% para todas, quanto a bandeira recebeu?
5. Qual a margem do adquirente?

---

## Exercício 6 — Roteamento por BIN (Nível: Intermediário)

Dada a tabela de BINs simplificada:

| Faixa BIN | Bandeira | Emissor |
|-----------|----------|---------|
| 4532xxxx | Visa | Itaú |
| 5412xxxx | Mastercard | Bradesco |
| 6362xxxx | Elo | Caixa |
| 5123xxxx | Mastercard | Nubank |
| 4389xxxx | Visa | BB |
| 6504xxxx | Elo | Itaú |

O switch é do **Itaú**. Para cada PAN, defina:
1. Qual bandeira?
2. Qual emissor?
3. On-us ou off-us?
4. A mensagem vai para qual destino?

PANs:
- `4532 0151 1283 0366`
- `5412 7890 1234 5678`
- `6362 1234 5678 9012`
- `6504 9999 8888 7777`
- `4389 5555 4444 3333`
- `5123 1111 2222 3333`

---

## Exercício 7 — Parcelamento (Nível: Avançado)

O portador compra um televisor de R$ 3.000,00 em 10x sem juros (parcelamento lojista) com cartão Visa Itaú na Cielo.

Responda:
1. Qual é o tipo de parcelamento?
2. Quem absorve o custo financeiro?
3. Como o merchant recebe? (valor e cronograma)
4. Se o merchant quiser antecipar, quanto aproximadamente recebe hoje? (use taxa de 1.5% a.m.)
5. Como isso aparece na mensagem ISO 8583? (quais DEs carregam info de parcelas)
6. No clearing, são gerados quantos registros?
7. No settlement, quantas liquidações acontecem?

---

## Exercício 8 — Diagrama Mermaid (Nível: Avançado)

Crie um diagrama Mermaid (sequence diagram) para o seguinte cenário:

**Cenário:** Compra de R$ 500 em 3x sem juros, cartão Mastercard Bradesco, maquininha Cielo. O emissor aprova. Inclua:
- Autorização (auth request/response)
- Clearing (D+1)
- Settlement (quando cada parcela é liquidada)
- Quem paga quem e quanto (use MDR 3.2%, interchange 1.8%, assessment 0.12%)

Salve o diagrama em `exercicio-08-diagrama.mermaid`.

---

## Exercício 9 — Glossário Pessoal (Nível: Iniciante)

Crie um arquivo `meu-glossario.md` com a definição **nas suas próprias palavras** de pelo menos 20 termos. Não copie — escreva como explicaria para um colega.

Termos obrigatórios:
- Authorization
- Clearing
- Settlement
- On-Us
- Off-Us
- BIN/IIN
- Interchange Fee
- MDR
- Dual Message
- Single Message
- Parcelamento Lojista
- Parcelamento Emissor
- Antecipação de Recebíveis
- Stand-In
- Reversal
- PAN
- STAN
- MTI
- Adquirente
- Emissor

---

## Exercício 10 — Pesquisa Independente (Nível: Avançado)

Pesquise e documente (em no máximo 1 página cada):

1. **O que é o arranjo de pagamento Elo?** Quem são os donos, qual a diferença para Visa/Master no Brasil?
2. **O que é um sub-adquirente/facilitador?** Dê 3 exemplos brasileiros e explique como o fluxo de autorização muda.
3. **O que é o registro de recebíveis?** Quem são CIP, TAG e CERC? Por que o BACEN exigiu isso?

Salve em `pesquisa-semana-01.md`.
