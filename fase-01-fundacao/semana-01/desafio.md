# Semana 1 — Desafio Integrador

## O Cenário

Você é Staff Engineer no time de pagamentos de um grande banco brasileiro. Na reunião de segunda-feira, o Head de Produto traz a seguinte situação:

> "Estamos perdendo dinheiro. Nossas transações on-us estão sendo roteadas como off-us — passando pela bandeira e pagando interchange desnecessário. O time de operações estima que 15% das transações que deveriam ser on-us estão indo como off-us. Com um volume de 2 milhões de transações/dia e ticket médio de R$ 85, precisamos resolver isso rápido."

## Sua Missão

### Parte 1 — Diagnóstico (30 min)

Escreva um documento `diagnostico.md` respondendo:

1. **Quais são as causas mais prováveis** de transações on-us serem roteadas como off-us? Liste pelo menos 5 causas possíveis.

2. **Qual é o impacto financeiro?** Calcule:
   - Quantas transações/dia estão sendo roteadas errado?
   - Quanto de interchange está sendo pago desnecessariamente? (use interchange médio de 1.5%)
   - Qual o impacto mensal e anual?

3. **Como você investigaria?** Descreva passo a passo:
   - Que dados pediria?
   - Que logs olharia?
   - Que métricas analisaria?
   - Com quem falaria?

### Parte 2 — Proposta de Solução (30 min)

Escreva um documento `proposta.md` com:

1. **Solução técnica:** Como corrigir o roteamento? Considere:
   - Tabela de BINs desatualizada (6 vs 8 dígitos?)
   - Regras de roteamento incorretas
   - Produtos novos (cartões novos do banco) sem cadastro na tabela
   - BINs compartilhados entre bandeiras

2. **Plano de ação:** Ordene por prioridade e estime esforço:
   - Quick wins (< 1 semana)
   - Médio prazo (1-4 semanas)
   - Longo prazo (1-3 meses)

3. **Como garantir que não volta a acontecer?** Proponha:
   - Monitoramento contínuo
   - Alerta automático
   - Processo de atualização de BINs
   - Métricas de acompanhamento

### Parte 3 — Comunicação (20 min)

Escreva um resumo de **no máximo 10 linhas** que você apresentaria para:

1. O **Head de Produto** (foco em receita e prazo)
2. O **time de arquitetura** (foco em solução técnica)
3. O **time de operações** (foco em monitoramento e procedimento)

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| Diagnóstico cobre causas técnicas e operacionais | /25 |
| Cálculo financeiro correto | /15 |
| Investigação é metódica e realista | /20 |
| Solução técnica é viável e bem priorizada | /20 |
| Comunicação é adaptada ao público | /20 |

**Meta:** 80+ pontos = Semana 1 dominada.

---

## Dicas

- Não existe resposta única. O que importa é o **raciocínio**.
- Especialista se diferencia por pensar em **impacto financeiro**, não só em código.
- A comunicação para diferentes audiências é tão importante quanto a solução técnica.
- Cronometre. Em produção, incidentes não esperam.
