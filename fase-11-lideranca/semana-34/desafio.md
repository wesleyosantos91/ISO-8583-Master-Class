# Desafio — Semana 34 — RFC para Mudança Arquitetural de Alto Impacto

## Contexto

Você é Staff Engineer numa adquirente que processa R$ 4 bilhões/mês em transações.
O time de produto pediu uma nova funcionalidade: **split payment** — uma única transação
do portador dividida entre dois merchants (ex: marketplace que repassa para seller).

Hoje o switch só conhece um `merchantId` por transação (DE 42). O split exige
que a autorização carregue dois merchants e que o clearing distribua o valor
corretamente para cada um.

O time de produto quer entregar isso em 3 meses. O time de certificação estima que
qualquer mudança no DE 42 ou DE 48 exige nova certificação com Visa e Mastercard.
O time financeiro alerta que o clearing atual não suporta splitting.

## O Problema

Você precisa:
1. Analisar a viabilidade técnica e o impacto no protocolo ISO 8583
2. Escrever uma RFC completa que permita o time alinhar a decisão
3. Apresentar a proposta para o CTO em 5 minutos

Há pressão para "só implementar logo" sem RFC, mas você sabe que a mudança afeta
5 times diferentes e é difícil de reverter após ir para produção.

## Missão

### Parte 1 — Análise Técnica Prévia

Antes de escrever a RFC, responda:

1. Por que não é possível simplesmente adicionar um segundo merchant no DE 42?
   (Dica: qual é o tamanho máximo do DE 42 e o que a spec define para ele?)

2. Quais campos ISO 8583 poderiam carregar os dados do segundo merchant?
   Considere DE 48 (private data) como opção principal. Quais são as vantagens e
   desvantagens de usar um subelemento proprietário do DE 48 em vez de propor
   um campo dedicado à bandeira?

3. O clearing atual gera um registro de crédito para o merchant por transação.
   Como seria o novo fluxo com split? Quantos registros seriam gerados?

4. Se a Visa não suportar split payment no protocolo padrão, quais alternativas
   o switch tem para implementar o split sem mudar o protocolo com a bandeira?

### Parte 2 — RFC Completa

Escreva a RFC-047 para implementar Split Payment, usando o template completo da teoria.
A RFC deve cobrir obrigatoriamente:

- **Resumo:** o que é e por que estamos fazendo
- **Motivação:** dados de negócio que justificam (ex: volume de marketplace, receita esperada)
- **Proposta Detalhada:** campos ISO afetados, formato do DE 48 proposto, fluxo de clearing
- **Alternativas Consideradas:** pelo menos 2 alternativas rejeitadas com justificativa
- **Impacto:** performance, segurança (PCI-DSS), compatibilidade com emissores/bandeiras, operacional
- **Plano de Implementação:** fases e como fazer rollback
- **Critérios de Sucesso:** métricas específicas
- **Questões em Aberto:** o que ainda não está decidido

### Parte 3 — Apresentação para o CTO

Escreva o script da apresentação de 5 minutos para o CTO.
Use o template da teoria e estruture em:

```
[0:00-0:45] O que é o split payment e por que o produto quer
[0:45-1:30] O que muda tecnicamente (sem jargão de protocolo)
[1:30-2:30] Os 3 riscos principais e como a RFC os mitiga
[2:30-3:30] O cronograma proposto e o que pode atrasar
[3:30-5:00] A decisão que você precisa do CTO agora
```

Para cada bloco, escreva as frases que você diria — não tópicos, frases completas.

## Critérios de Avaliação

- [ ] Explica corretamente por que DE 42 não pode ser reutilizado (Parte 1.1)
- [ ] Propõe solução viável com DE 48 e identifica trade-offs (Parte 1.2)
- [ ] Descreve o novo fluxo de clearing com número de registros (Parte 1.3)
- [ ] Apresenta alternativa sem mudança de protocolo com bandeira (Parte 1.4)
- [ ] RFC cobre todas as seções obrigatórias com conteúdo técnico preciso (Parte 2)
- [ ] RFC inclui pelo menos 2 alternativas rejeitadas com justificativa (Parte 2)
- [ ] Apresentação para CTO usa linguagem de negócio, não de protocolo (Parte 3)
- [ ] Apresentação termina com uma decisão clara solicitada (Parte 3)

## Dicas

- DE 42 tem 15 posições fixas — não há como adicionar um segundo merchant sem quebrar a spec
- DE 48 é o campo padrão para extensões proprietárias — mas não é visível para emissores via bandeira
- Uma alternativa sem mudar protocolo: processar o split no clearing (pós-autorização),
  mantendo apenas um merchant na autorização
- O CTO não quer saber o número do campo ISO — quer saber: custo, risco, prazo, receita
- A RFC não precisa ter a decisão final — ela precisa ter as informações para que outros decidam
