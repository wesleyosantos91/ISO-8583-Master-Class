# Semana 8 — Desafio Integrador
## "O Dinheiro que Saiu Duas Vezes"

---

## O Cenário

São 14h23 de uma terça-feira. O time de SAC recebe uma reclamação incomum:

> "Senhor Rodrigo efetuou uma compra de R$ 2.500,00 em uma loja de eletrônicos às 14h15. O terminal deu 'transação não aprovada' após aguardar mais de um minuto. O cliente pagou com outro cartão. Mas ao verificar o extrato bancário às 14h22, a compra de R$ 2.500,00 foi debitada da conta. E o reversal também apareceu como outra movimentação. E agora há uma segunda compra pendente."

O DBA puxa os logs do banco de dados do switch e encontra:

```
14:15:03.001  TXN#8821001  0200 enviado para emissor  pan=5123****3333  amount=250000  stan=001001  terminal=TERM0042
14:15:33.001  TXN#8821001  TIMEOUT após 30s           stan=001001       terminal=TERM0042
14:15:33.050  TXN#8821001  0200 response = null        RESPONSE_CODE=68  NEEDS_REVERSAL=true
14:15:33.100  TXN#8821002  0400 enviado para emissor  stan=001001       terminal=TERM0042
14:15:34.200  TXN#8821002  0400 response = DE39=00    ← reversal confirmado
14:15:34.210  TXN#8821001  STATUS = REVERSED

14:15:35.500  LATE_RESPONSE 0210 chegou             stan=001001  terminal=TERM0042  DE39=00  authcode=XK4729
             ↑ O EMISSOR APROVOU! A resposta atrasou 32.5 segundos!

14:15:35.510  WARN  LateResponseHandler - Late response recebida. DE39=00. Gerando 0400.
14:15:35.520  TXN#8821003  0400 enviado para emissor  [reversal do reversal???]
14:15:36.800  TXN#8821003  0400 response = DE76       ← emissor diz: transação não encontrada
```

O emissor (banco do Rodrigo) aplicou os débitos:
1. Débito R$ 2.500,00 (autorização original chegou com atraso)
2. Crédito estorno (primeiro reversal confirmado)
3. Débito novamente? (o segundo 0400 gerou confusão no emissor)

**O cliente está com o extrato inconsistente. O dinheiro saiu efetivamente uma vez, mas aparece debitado e creditado de forma confusa.**

---

## Sua Missão

### Parte 1 — Reconstituição do Incidente (25 min)

Crie `reconstituicao-incidente.md` com:

1. **Linha do tempo completa:** Reconstrua o que aconteceu segundo a segundo, incluindo o que cada sistema (switch, emissor, terminal) acreditava ser verdade em cada momento.

2. **Diagrama de sequência Mermaid** mostrando todas as mensagens trocadas:
   - Terminal → Switch: 0200
   - Switch → Emissor: 0200
   - Switch → Terminal: timeout response (DE39=68)
   - Switch → Emissor: 0400 (primeiro reversal)
   - Emissor → Switch: 0400 response (DE39=00)
   - Emissor → Switch: 0210 LATE (DE39=00)
   - Switch → Emissor: 0400 (segundo reversal, tentativa)
   - Emissor → Switch: 0400 response (DE39=76)

3. **Qual é o estado final** de cada sistema? Quem acha que o cliente tem dinheiro debitado? Quem acha que foi estornado?

4. **Qual o impacto financeiro real** para o cliente e para o banco?

### Parte 2 — Correção do Switch (40 min)

O problema foi identificado: o `LateResponseHandler` gerou um segundo 0400 para uma transação que **já tinha sido revertida**. O emissor (corretamente) devolveu DE39=76 ("transação não encontrada").

Implemente `LateResponseHandler.java` corrigido:

```java
public class LateResponseHandler implements ISORequestListener {

    @Override
    public boolean process(ISOSource source, ISOMsg lateResponse) throws ISOException {
        // Implemente a lógica correta aqui
    }
}
```

**A lógica correta deve:**

1. Verificar no banco/cache de transações qual é o **status atual** da transação original:
   - Se status = `REVERSED`: A transação já foi revertida com sucesso.
     - Se DE39=`"00"` (emissor aprovou): o cliente recebeu 2 movimentações → criar exceção para reconciliação manual, alertar operações, **NÃO enviar novo 0400**
     - Se DE39≠`"00"`: estado consistente, apenas logar
   - Se status = `APPROVED`: Situação rara mas possível. Logar e investigar.
   - Se status = `PENDING`: ainda aguardando, processar normalmente

2. Para o caso crítico (REVERSED + DE39=`"00"`):
   - Criar registro em tabela `RECONCILIATION_QUEUE` com todos os detalhes
   - Enviar alerta para o time de operações
   - **Não tentar nenhuma mensagem automática** — deixar para reconciliação humana

**Justifique no código:** por que não enviar o segundo 0400?

### Parte 3 — Processo de Reconciliação (15 min)

Escreva `processo-reconciliacao.md` com:

1. **Como resolver o caso do Rodrigo hoje** (o incidente já aconteceu):
   - O que você faria manualmente para deixar o extrato consistente?
   - Quem precisa ser acionado (emissor, adquirente, banco)?
   - Prazo razoável para resolução?

2. **Como prevenir que isso aconteça novamente:**
   - Qual a regra de negócio que previne o segundo 0400 "cego"?
   - Qual tabela/cache você precisa manter no switch?
   - Como garantir que esse cache sobrevive a um restart do switch?

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| Linha do tempo correta e diagrama de sequência preciso | /20 |
| Identifica corretamente a causa raiz (segundo 0400 cego) | /15 |
| LateResponseHandler corrigido: não envia 0400 quando já revertido | /25 |
| Cria exceção de reconciliação com dados corretos | /20 |
| Processo de reconciliação realista e completo | /20 |

**Meta:** 80+ pontos = Semana 8 dominada.

---

## Dicas

- O principal erro foi tentar corrigir automaticamente uma inconsistência que **já havia sido resolvida**. Às vezes a melhor ação automática é **não fazer nada** e alertar um humano.
- O código DE39=76 ("Unable to locate previous message") do emissor é um sinal claro: o emissor não sabe de nenhuma transação pendente com esse STAN. O switch deveria ter tratado isso como "estado consistente no emissor" e parado.
- Em sistemas financeiros, **idempotência** é sagrada. Antes de fazer qualquer operação financeira automática, verifique o estado atual — não assuma.
- A `RECONCILIATION_QUEUE` é uma tabela real em switches de produção. Toda transação que não pode ser resolvida automaticamente cai nela para análise humana às 9h da manhã seguinte.
