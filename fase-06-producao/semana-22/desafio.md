# Semana 22 — Desafio Integrador

## O Cenário

Você acaba de ser contratado como engenheiro sênior em uma processadora que atende 3 adquirentes diferentes. No seu primeiro dia de trabalho, a gerente de operações te chama:

> "Bem-vindo. Você vai adorar aqui. Temos um sistema legado que funciona 'mais ou menos'. São 5 problemas conhecidos que ninguém resolveu ainda porque ninguém entende o suficiente. O time anterior saiu. Você tem 1 semana para diagnosticar e pelo menos 3 precisam estar resolvidos."

Ela te entrega os logs e dumps de 5 incidentes abertos:

---

**Incidente #1 — "O misterioso DE39=30"**
> "Desde ontem, 100% das transações com chip (DE22=051) para o emissor BANCO_CENTRAL retornam DE39=30 (Format Error). Transações de tarja do mesmo emissor funcionam normalmente. Não mudamos nada."

Dump de uma transação que falha (hex, espaços para legibilidade):
```
0200 B2780001 8EA0000000000000 19 4532015112830366
001200000000015000 0031416 00012345 6 16 00003141
160000000314 051 00 0000000000000000 SÃO PAULO / MERCHANT 001
[DE55 = 45 bytes de dados EMV]
```

---

**Incidente #2 — "O BIN fugitivo"**
> "Todo cartão Elo emitido pelo Itaú (BIN 6504xxxx) está indo para a rede Elo quando deveria ir on-us. Isso custa interchange desnecessário — R$ 45.000/mês estimado."

---

**Incidente #3 — "O terminal fantasma"**
> "O terminal TERM0099 manda echo a cada 30s e recebe resposta. Mas qualquer 0200 dele dá timeout. Estamos com 127 transações travadas no mux. O portador passou o cartão, a tela da maquininha está processando há 2 horas."

---

**Incidente #4 — "PIN quebrado só no chip"**
> "Terminal TERM0042 — contactless (DE22=071) aprova tudo. Chip contact (DE22=051) com PIN retorna DE39=55 em 100% dos casos. O ZPK foi trocado ontem. Coincidência?"

---

**Incidente #5 — "47 auths órfãs"**
> "Na reconciliação de ontem: 47 transações autorizadas sem clearing correspondente. Todas do merchant MERCHANT00042 (restaurante). O merchant diz que fez o batch close normalmente."

---

## Sua Missão

### Parte 1 — Triagem e Priorização (15 min)

Escreva `triagem-s22.md`:

1. **Ordene os 5 incidentes por prioridade.** Justifique usando: impacto financeiro, impacto no portador, urgência operacional.

2. **Para cada incidente, estime:** É resolvível em 1 dia? 3 dias? 1 semana? Por quê?

3. **Dependências:** Algum incidente depende de outro ser resolvido primeiro?

### Parte 2 — Diagnóstico de Cada Incidente (45 min)

Escreva `diagnostico-completo-s22.md`:

Para cada um dos 5 incidentes, aplique os 4 passos do playbook e chegue a um diagnóstico:

**Incidente #1:** Analise o dump fornecido. O DE 22=051 indica chip. O DE 55 está presente. O DE39=30 indica format error. Onde está o problema? (Dica: examine o encoding do DE 43 — nome do merchant — e os caracteres especiais.)

**Incidente #2:** Descreva passo a passo como você investigaria a tabela de BINs. O que você esperaria encontrar? Qual seria a correção?

**Incidente #3:** Liste as hipóteses ordenadas por probabilidade. Qual é a mais provável dado o contexto ("127 transações travadas no mux")? O que o estado do mux indica?

**Incidente #4:** A troca de ZPK ontem é a chave do diagnóstico. Explique exatamente o que aconteceu tecnicamente e por que afeta chip mas não contactless.

**Incidente #5:** O merchant fez batch close mas as transações não chegaram no clearing. Liste 5 pontos de falha possíveis entre "batch close no terminal" e "arquivo de clearing entregue à bandeira".

### Parte 3 — Runbook para o Time de Operações (20 min)

Escreva `runbook-ops-s22.md` no formato que o time de operações vai usar em produção:

Para cada um dos 10 incidentes mais comuns (inclua os 5 deste desafio + 5 outros), documente:

```markdown
## Incidente: [Nome]

### Detecção
- Alerta disparado: [nome do alerta]
- Métrica afetada: [nome da métrica, threshold]
- Sintoma observável: [o que o portador/merchant sente]

### Diagnóstico (5 passos)
1. [Verificar X no sistema Y]
2. ...

### Resolução
#### Quick Fix (< 15 min)
[Ação imediata que reduz impacto]

#### Fix Permanente (1+ dias)
[Correção de código/configuração]

### Prevenção
[Como evitar no futuro]

### Escalonamento
[Quando escalar? Para quem?]
```

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| Priorização dos incidentes com justificativa técnica e financeira | /15 |
| Diagnóstico correto para cada um dos 5 incidentes | /40 |
| Aplicação do playbook (4 passos) de forma sistemática | /15 |
| Runbook completo, acionável e no formato correto | /20 |
| Identificação da causa raiz do incidente #4 (ZPK) | /10 |

**Meta:** 80+ pontos = Semana 22 dominada.

---

## Dicas

- O incidente #4 é o mais sutil e o mais valioso: chip contact usa PIN (que passa pelo ZPK), contactless abaixo do limite não pede PIN. Logo, o problema é no fluxo de PIN — e o ZPK trocado ontem é a causa óbvia.
- O incidente #3 com 127 transações "travadas no mux": quando o mux acumula requests sem response, geralmente é porque o key de correlação está errado (a response chega mas o mux não consegue associar com o request original).
- O incidente #1: caracteres especiais (como "SÃO PAULO" com Ã) em campos ASCII podem causar Format Error se o encoding não for tratado (UTF-8 vs Latin-1 vs EBCDIC).
- O Runbook é o documento que vai salvar você às 3h da manhã. Escreva para uma versão de si mesmo com sono — específico, sem ambiguidade, passos numerados.
- Cronometre. Em produção, você tem 1 semana. Escolha os 3 incidentes de maior impacto para resolver primeiro.
