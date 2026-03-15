# Semana 16 — Desafio Integrador

## O Cenário

Você é engenheiro sênior no time de switch de um banco emissor. Na terça-feira, o time de contestações envia um relatório de anomalias:

> "Identificamos 312 portadores com cobranças duplicadas no extrato da semana passada. Os valores duplicados somam R$ 47.832,00. Os clientes estão reclamando e acionando o Procon. O padrão: todas as cobranças duplicadas são de terminais de um único adquirente (AcqID=00011223344) e ocorreram entre 14:00 e 14:15 de sexta-feira."

Investigação preliminar do time de operações:
> "No nosso lado (emissor), vemos os 312 casos de cobranças duplas. Cada portador foi debitado duas vezes pelo mesmo valor. O adquirente diz que enviou cada transação apenas uma vez. Os STANs são diferentes entre as duas cobranças de cada portador."

Esse é um cenário mais complexo do que parece: os STANs são diferentes, o que significa que não é uma retransmissão óbvia.

## Sua Missão

### Parte 1 — Investigação Forense (30 min)

Escreva `investigacao-s16.md` respondendo:

1. **Se os STANs são diferentes, como pode ser duplicata?** Explique o mecanismo técnico pelo qual dois STANs diferentes podem representar a mesma transação econômica.

2. **Reconstrua o que provavelmente aconteceu** entre 14:00 e 14:15 de sexta-feira. Crie uma hipótese técnica completa:
   - O que o adquirente fez?
   - O que o switch fez?
   - O que o emissor fez?
   - Por que os STANs são diferentes?

3. **Quais evidências você buscaria** para confirmar a hipótese? Liste com precisão:
   - Qual tabela/log no emissor?
   - Qual campo?
   - O que você esperaria encontrar?

4. **Hipóteses alternativas:** E se não for um problema de deduplicação, mas sim:
   - Um bug no processamento de timeout do adquirente?
   - Uma ataque/fraude?
   - Um problema de sincronização de relógio entre sistemas?
   Como você eliminaria cada hipótese?

5. **Query SQL de investigação:** Escreva uma query (ou descreva em pseudocódigo) que, dado a tabela de autorizações do emissor, identifica pares de transações que parecem duplicatas:

```sql
-- Encontrar pares de transações do mesmo portador,
-- mesmo valor, mesma data, mesma merchant, com STANs diferentes,
-- dentro de uma janela de 15 minutos
SELECT ...
```

### Parte 2 — Solução Técnica (25 min)

Escreva `solucao-tecnica-s16.md` com:

1. **O DuplicateChecker existente não teria capturado esse caso.** Por quê? (Dica: os STANs são diferentes.)

2. **Implemente** (ou descreva detalhadamente) uma segunda camada de deduplicação no lado do emissor, chamada `SemanticDuplicateChecker`:

```java
public class SemanticDuplicateChecker {
    /**
     * Detecta duplicatas "semânticas": mesma combinação econômica
     * mesmo que o STAN seja diferente.
     *
     * Chave: PAN (mascarado) + Amount + MerchantID + ProcessingCode
     * Janela: 10 minutos
     */
    public boolean isSemanticallyDuplicate(ISOMsg msg) { /* ... */ }
}
```

3. **Qual é a tensão entre segurança e falsos positivos?** Explique:
   - Por que uma janela de 10 minutos pode rejeitar transações legítimas (ex: portador compra duas cervejas de R$ 8,50 no mesmo bar)
   - Como calibrar a política para reduzir falsos positivos sem sacrificar a proteção

4. **Duas camadas de deduplicação:** Desenhe o fluxo com o `DuplicateChecker` (baseado em STAN) E o `SemanticDuplicateChecker` (baseado em semântica) trabalhando juntos.

### Parte 3 — Resolução e Comunicação (15 min)

1. **Para os 312 portadores afetados:** Descreva o processo de estorno. Quem executa? Em quanto tempo? Qual a mensagem para o portador?

2. **Prevenção:** Além do código, o que mais precisa mudar?
   - Processo com o adquirente?
   - Monitoramento?
   - Alerta automático?

3. **Comunicação em 3 versões** (máximo 5 linhas cada):
   - Para o **time jurídico** (que está respondendo ao Procon)
   - Para o **adquirente** (AcqID=00011223344) responsável
   - Para o **VP de tecnologia** do banco

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| Hipótese técnica coerente e completa (Parte 1) | /25 |
| Query SQL ou pseudocódigo de investigação correto | /15 |
| Identificação correta da limitação do DuplicateChecker existente | /15 |
| SemanticDuplicateChecker implementado ou descrito corretamente | /20 |
| Análise da tensão falso positivo vs segurança | /15 |
| Comunicação adaptada para cada audiência | /10 |

**Meta:** 80+ pontos = Semana 16 dominada.

---

## Dicas

- STANs diferentes não significa transações diferentes. O STAN é gerado pelo terminal — se o terminal reiniciou ou tem bug, pode gerar novo STAN para a mesma transação econômica.
- "Janela de 15 minutos" no incidente é um sinal claro: algo aconteceu naquele período (queda de conexão? restart do adquirente?).
- A deduplicação por STAN é necessária mas não suficiente. Emissores sofisticados têm múltiplas camadas.
- R$ 47.832 em cobranças duplicadas é um incidente P1 — a resposta precisa ser rápida E correta.
- Cronometre. Em produção, o Procon tem prazo de resposta de 5 dias úteis.
