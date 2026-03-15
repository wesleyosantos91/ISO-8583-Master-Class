# Semana 14 — Desafio Integrador

## O Cenário

Você é engenheiro sênior em uma processadora que gerencia terminais de uma rede de postos de combustível. Na segunda-feira às 09h15, o gerente de operações envia a seguinte mensagem:

> "Tivemos uma queda de internet em 12 postos durante o feriado (das 14h00 às 17h30 de sábado). Os terminais continuaram operando offline. Agora que a conexão voltou, os terminais estão tentando enviar as transações acumuladas como SAF. Mas estamos vendo problemas: alguns postos enviaram as mesmas transações 3 vezes, o host está recusando algumas por 'data muito antiga', e temos portadores reclamando de dupla cobrança."

Dados da situação:
- 12 postos afetados
- Tempo offline: 3h30 (14:00–17:30 sábado)
- Estimativa: ~340 transações acumuladas por posto = ~4.080 transações no total
- Horário atual: segunda 09h15 → transações têm 19h15 de idade
- TTL configurado no SAF: 24 horas
- Reclamações de dupla cobrança: 7 portadores confirmados

## Sua Missão

### Parte 1 — Triagem e Diagnóstico (30 min)

Escreva `diagnostico-s14.md` respondendo:

1. **Qual é a urgência real?** As transações ainda podem ser drenadas? (calcule: 19h15 + tempo para processar 4.080 transações vs TTL de 24h)

2. **Por que há duplicatas?** Liste as causas prováveis para as mesmas transações chegando 3 vezes:
   - Causa no terminal (firmware/configuração)
   - Causa no protocolo SAF
   - Causa na ausência de deduplicação

3. **Por que o host recusa com "data muito antiga"?** Qual campo e qual validação está causando isso? Como contornar sem criar risco?

4. **Mapeie o estado atual:** Para as 4.080 transações, quais podem estar:
   - Processadas corretamente (1 vez)
   - Processadas em duplicata (2+ vezes)
   - Expiradas/descartadas
   - Ainda na fila aguardando

5. **Prioridade de ação:** O que você faz primeiro nos próximos 30 minutos?

### Parte 2 — Resolução Técnica (30 min)

Implemente ou descreva detalhadamente em `resolucao-s14.md`:

1. **Deduplicação de SAF em emergência:** Escreva um script/procedure SQL que:
   - Recebe a tabela de SAF pendente
   - Identifica duplicatas (mesma combinação de: STAN + TerminalID + Amount + TransactionDate)
   - Mantém apenas 1 de cada grupo
   - Gera relatório: quantas duplicatas foram removidas, quais STANs

2. **Processo de drenagem controlada:** Como você drenaria 4.080 transações sem sobrecarregar o host? Descreva:
   - Rate limiting (quantas por segundo?)
   - Monitoramento durante a drenagem
   - Critério para pausar/abortar

3. **Tratamento dos 7 casos de dupla cobrança confirmados:** Qual é o processo para cada portador? Quem faz o quê?

### Parte 3 — Prevenção e Comunicação (20 min)

1. **Escreva `prevention-saf.md`** com melhorias técnicas para evitar que isso aconteça novamente:
   - Deduplicação automática no próprio SAF antes de drenar
   - Alerta quando fila SAF excede N itens ou Y horas
   - Processo para feriados/eventos especiais

2. **Comunicação em 3 versões** (máximo 5 linhas cada):
   - Para os **7 portadores afetados:** o que dizer sobre a dupla cobrança?
   - Para o **gerente de operações:** o que aconteceu e qual é o status?
   - Para o **time técnico:** o que precisamos mudar no sistema?

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| Diagnóstico correto das causas técnicas (duplicata, TTL, protocolo) | /25 |
| Cálculo de urgência e priorização correta | /15 |
| Solução de deduplicação é funcional e segura | /20 |
| Processo de resolução dos 7 casos é completo | /20 |
| Comunicação adaptada para cada audiência | /20 |

**Meta:** 80+ pontos = Semana 14 dominada.

---

## Dicas

- SAF parece simples, mas em produção é uma das fontes mais frequentes de incidentes.
- A "solução" de rejeitar transações antigas pode ser pior do que o problema (portador fica sem receber e sem cobrança — ninguém sabe o que aconteceu).
- Duplicata financeira é sério: pode gerar chargeback automático da bandeira contra o adquirente.
- Em operações de combustível, o ticket médio é ~R$ 150 — 7 duplicatas = R$ 1.050 em risco imediato.
- Cronometre. Em produção, o TTL de 24h pode estar prestes a expirar enquanto você diagnostica.
