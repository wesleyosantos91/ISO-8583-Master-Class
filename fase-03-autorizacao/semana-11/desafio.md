# Semana 11 — Desafio Integrador
## "A Expansão de BIN que Custou R$ 2 Milhões"

---

## O Cenário

A Mastercard enviou um comunicado há 6 meses:

> "A partir de 1º de janeiro de 2026, os BINs de 6 dígitos na faixa `5412xx` serão expandidos para 8 dígitos. Os BINs `54120000` a `54124999` continuam roteando para o Bradesco. Os BINs `54125000` a `54129999` são novos cartões do Nubank. A tabela de BINs deve ser atualizada até 31/12/2025."

O comunicado foi recebido, arquivado, e **esquecido**.

No dia 2 de janeiro de 2026, o switch da FinTechBR continua usando apenas BINs de 6 dígitos. Resultado: todos os cartões `5412xxxx` do Nubank estão sendo roteados para o Bradesco.

O emissor Bradesco, ao receber cartões que não são seus, devolve DE39=`14` (Invalid card number).

**Métricas do dia 2 de janeiro (8h às 12h):**
```
BIN 541250xx ao 541299xx (Nubank):
  Total tentativas:   47.230
  DE39=14 (Bradesco): 47.230 (100% decline)

BIN 541200xx ao 541249xx (Bradesco):
  Total tentativas:   51.890
  DE39=00 (Aprovado): 46.101 (88.8% — normal)
```

**Impacto nos clientes Nubank:** 47.230 transações negadas em 4 horas. Ticket médio R$ 87.

---

## Sua Missão

### Parte 1 — Quantificação do Problema (15 min)

Crie `quantificacao-impacto.md`:

1. **Impacto financeiro total (projeção do dia completo):**
   - A janela de 8h-12h representa ~1/6 do dia. Extrapole para 24 horas.
   - Valor total em transações bloqueadas?
   - Receita perdida para a FinTechBR (MDR médio 2.3%)?

2. **Impacto para os portadores:**
   - Quantos clientes únicos afetados (assuma 3.2 transações/cliente/dia)?
   - Quais os cenários mais prejudiciais para os clientes? (ex: supermercado, posto de gasolina, emergência médica)

3. **Por que o DE39=14 é particularmente ruim aqui?**
   - O que o cliente vê no terminal?
   - O que o cliente pensa ter acontecido com seu cartão?
   - Qual o risco reputacional para o Nubank e para a FinTechBR?

### Parte 2 — Migração de BIN Table (50 min)

Implemente a solução técnica completa para a migração de BINs:

**Passo 1: `BINTableMigration.java`**

```java
public class BINTableMigration {

    /**
     * Lê a tabela de BINs atual (apenas 6 dígitos)
     * e retorna uma lista de BINs que precisam ser expandidos
     * para 8 dígitos baseado no comunicado da Mastercard.
     */
    public List<BINMigrationEntry> findBINsToExpand(BINTable current) { ... }

    /**
     * Aplica a expansão: para cada BIN de 6 dígitos na faixa afetada,
     * cria duas entradas de 8 dígitos (Bradesco e Nubank) e remove a de 6.
     */
    public BINTable applyExpansion(BINTable current, List<BINMigrationEntry> entries) { ... }

    /**
     * Valida a tabela migrada: verifica que nenhum BIN Nubank aponta para Bradesco
     * e nenhum BIN Bradesco aponta para Nubank.
     */
    public ValidationResult validateMigration(BINTable migrated) { ... }
}
```

**Passo 2: Implemente a tabela migrada com os BINs corretos:**

| BIN Prefix | Emissor | On-Us? | QMUX destino |
|------------|---------|--------|--------------|
| `54120000` a `54124999` | Bradesco | Não | `bradesco-mux` |
| `54125000` a `54129999` | Nubank | Não | `nubank-mux` |

(Para o exercício, simplifique: use `54120000` e `54125000` como representantes das faixas.)

**Passo 3: Escreva o teste de migração:**

```java
@Test
void afterMigrationNubankBINsRouteToNubank() {
    String nubankPAN = "5412750012345678"; // BIN 54127500 → Nubank
    String bradescoPAN = "5412300012345678"; // BIN 54123000 → Bradesco

    // Com tabela antiga: ambos vão para Bradesco (ERRADO)
    // Com tabela migrada: Nubank vai para nubank-mux, Bradesco para bradesco-mux
}

@Test
void migrationDoesNotAffectOtherBINs() {
    // BINs não relacionados à faixa 5412 devem permanecer iguais após migração
}
```

**Passo 4: Hot reload da BINTable sem restart do switch:**

Implemente `BINTableReloader.java` que:
- Monitora um arquivo `bin-table.json` por mudanças (a cada 30 segundos)
- Ao detectar mudança, carrega a nova tabela
- **Faz a troca atomicamente** (sem janela onde a tabela está pela metade)
- Não perde nenhuma transação em andamento durante a troca
- Loga: `[BIN-TABLE] Reloaded: 1.247 routes (was 1.245). Added: [54125000, 54126000]. Removed: []`

```java
public class BINTableReloader {

    private final AtomicReference<BINTable> currentTable;

    public void startMonitoring(String filePath) { ... }

    // Troca atômica — todas as novas requests usam a nova tabela
    // mas as requests em andamento terminam com a tabela que começaram
    private void reload(String filePath) { ... }
}
```

### Parte 3 — Processo de Gestão de BINs (15 min)

Escreva `processo-gestao-bins.md` com:

1. **Por que o comunicado da Mastercard foi ignorado?** Liste as falhas de processo possíveis.

2. **Proposta de processo para nunca perder um comunicado de BIN:**
   - Quem deve receber (e ler) comunicados das bandeiras?
   - Como transformar um comunicado em uma tarefa técnica com prazo?
   - Como testar uma mudança de BIN antes de ir para produção?
   - Qual o processo de rollback se a nova tabela tiver erro?

3. **Monitoramento proativo de anomalias de roteamento:**
   - Que métrica teria alertado que Nubank estava sendo roteado para Bradesco?
   - Qual o threshold de alerta? (ex: "se DE39=14 para um BIN específico > X% → alerta")
   - Em quanto tempo o alerta deveria ter disparado?

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| Quantificação de impacto correta e completa | /15 |
| BINTable migrada com BINs corretos | /20 |
| Teste de migração passando (Nubank → nubank-mux) | /20 |
| Hot reload atômico implementado corretamente | /25 |
| Processo de gestão de BINs realista e acionável | /20 |

**Meta:** 80+ pontos = Semana 11 dominada.

---

## Dicas

- A troca atômica da BINTable é crítica. Se você simplesmente fizer `this.binTable = newTable`, pode haver threads lendo a tabela antiga no meio de uma atualização parcial. Use `AtomicReference<BINTable>` para garantir que a troca é atômica.
- Em sistemas de produção, a BINTable pode ter 500.000+ entradas. Carregar um arquivo grande pode levar segundos. A solução é: carregar em background, validar, depois fazer o swap.
- O monitoramento de anomalias de roteamento é tão importante quanto o código. Um alerta de "BIN X com 100% decline para emissor Y" teria pego esse problema em minutos, não horas.
- Comunicados de bandeiras (Visa, Mastercard, Elo) têm prazos reais e multas por descumprimento. Uma boa empresa de pagamentos tem um processo formal para rastreá-los — geralmente um Jira board dedicado só para compliance de bandeiras.
