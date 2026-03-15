# Desafio — Semana 28 — Certificação Visa: O Test Deck Precisa Passar 100%

## Contexto

Você finalizou o payment-switch-lab após 28 semanas. O sistema suporta autorização, reversal, parcelamento, pre-authorization, partial approval, reconciliação e tem métricas, logs estruturados e runbook operacional.

A empresa recebeu aprovação do Banco Central para operar como adquirente. O próximo passo obrigatório é a certificação com a Visa. O processo começa na semana que vem — você tem 5 dias úteis para preparar o sistema para o test deck.

A Visa enviou a lista dos 20 cenários que serão executados (a mesma tabela da semana 28, seção 2.3). O analista da Visa vai executar cada cenário e registrar: passou ou falhou. **Qualquer falha bloqueia a certificação.**

## O Problema

Ao rodar o test deck internamente antes da certificação, os seguintes cenários falharam:

```
TC-10: Duplicate STAN — switch retornou nova resposta do emissor em vez do cache
TC-15: Pre-auth (DE25=06) — switch enviou ao clearing automaticamente (não deveria)
TC-17: Partial approval — switch não propagou DE4 reduzido da resposta ao terminal
TC-19: On-us routing — BIN 453201 foi roteado off-us (tabela de BIN desatualizada)
```

Além disso, os cenários de network management (TC-11 e TC-12) estão passando, mas o sign-on não persiste estado — após restart, o switch precisa de sign-on manual.

## Missão

### Parte 1 — Correção dos Cenários Reprovados

Para cada cenário com falha, identifique a causa raiz e implemente a correção:

**TC-10 — Duplicate STAN:**

```java
// O problema: DuplicateChecker está com TTL de 0 (bug de configuração)
// Corrija o cache de deduplicação e escreva teste que:
// 1. Envia 0200 com STAN=000001
// 2. Envia novamente 0200 com STAN=000001 (mesmo STAN)
// 3. Verifica que a segunda resposta é IDÊNTICA à primeira (do cache)
// 4. Verifica que o emissor foi chamado apenas UMA vez
```

**TC-15 — Pre-auth enviada ao clearing:**

O switch tem um `AutoClearingParticipant` que envia ao clearing todas as 0200 aprovadas. Pre-auth com `DE25=06` não deve ir ao clearing — só o completion (`DE25=12`) deve acionar o clearing.

Corrija o `AutoClearingParticipant`:

```java
public class AutoClearingParticipant implements TransactionParticipant {
    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg req = ctx.get("REQUEST");
        // Adicione a verificação aqui:
        // Se DE25=06 (pre-auth), não enviar ao clearing
        /* ... */
    }
}
```

**TC-17 — Partial approval com DE4 incorreto:**

O switch estava retornando o DE4 do request original ao terminal, não o DE4 reduzido da resposta do emissor. Corrija o `ResponseBuilder`.

**TC-19 — BIN roteado incorretamente:**

Atualize a tabela de BIN para incluir o range `453201 → on-us` e escreva um teste que verifica o roteamento correto de 5 BINs diferentes (3 on-us, 2 off-us).

### Parte 2 — Sign-On Persistente

Implemente `SignOnStateManager` que persiste o estado do sign-on entre restarts:

```java
public class SignOnStateManager {

    /**
     * Registra que um sign-on bem-sucedido ocorreu para um destino.
     * Persiste em arquivo/banco para sobreviver restart.
     */
    public void recordSignOn(String destination, Instant timestamp) { /* ... */ }

    /**
     * Retorna true se o destino tem sign-on válido (menos de 24h).
     * Permite que o switch reuse sign-on pós-restart se foi recente.
     */
    public boolean isSignedOn(String destination) { /* ... */ }

    /**
     * Na inicialização do switch: verifica quais destinos precisam
     * de novo sign-on e agenda automaticamente.
     */
    public List<String> getDestinationsRequiringSignOn() { /* ... */ }
}
```

### Parte 3 — Evidências para a Visa

O analista da Visa vai pedir evidências de cada cenário. Gere automaticamente `test-evidence/` com:

1. Um arquivo por cenário: `TC-AUTH-001.log`, `TC-REVERSAL-001.log`, etc.
2. Cada arquivo deve conter: timestamp, campos enviados (hex dump), resposta recebida (hex dump), resultado (PASS/FAIL), latência
3. Um arquivo `TEST-SUMMARY.md` com a tabela dos 20 cenários e status de cada um

Implemente `EvidenceLogger`:

```java
public class EvidenceLogger {
    /**
     * Gera o arquivo de evidência para um cenário executado.
     * Formato exigido pelas bandeiras: inclui hex dump de request e response.
     */
    public void logEvidence(TestResult result, Path outputDir) throws IOException { /* ... */ }

    public void generateSummary(List<TestResult> results, Path outputFile) throws IOException { /* ... */ }
}
```

## Critérios de Avaliação

- [ ] TC-10 corrigido: deduplicação funciona com TTL configurado, teste comprova emissor chamado 1x
- [ ] TC-15 corrigido: pre-auth (DE25=06) não vai ao clearing; completion (DE25=12) vai
- [ ] TC-17 corrigido: DE4 da resposta do emissor (não do request) é propagado ao terminal
- [ ] TC-19 corrigido: tabela de BIN atualizada, teste cobre 5 BINs diferentes
- [ ] `SignOnStateManager` persiste estado entre restarts com teste de verificação
- [ ] `EvidenceLogger` gera arquivos no formato correto com hex dumps
- [ ] `TEST-SUMMARY.md` gerado com status de todos os 20 cenários
- [ ] 20/20 cenários passando ao final

## Dicas

- O analista da Visa vai verificar os hex dumps das mensagens — se o encoding estiver incorreto (BCD vs ASCII), o cenário falha mesmo que a lógica esteja certa. Verifique o packager.
- Para TC-10, a chave do cache é tipicamente `STAN + date + DE41 (terminal ID)`. Apenas STAN pode colidir entre terminais diferentes — inclua o terminal no cache key.
- Pre-auth e clearing: na teoria, `DE25=06` significa "pre-authorized" e o clearing acontece no completion (`DE25=12`). O switch precisa verificar o POS Condition Code antes de acionar qualquer clearing.
- As evidências precisam ser geradas pelo próprio sistema, não manualmente — o analista da Visa executa muitos cenários e não aceita evidências retroativas.
- Meta: 20/20 antes de enviar para a Visa. Cada falha adiciona semanas ao processo de certificação.
