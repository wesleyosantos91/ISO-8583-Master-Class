# Semana 12 — Exercícios de Fixação
## Timeout, Stand-in e Resiliência

---

## Exercício 1 — SLAs de Timeout (Nível: Iniciante)

Para cada tipo de mensagem, preencha a tabela de SLAs e justifique:

| Tipo | MTI | Timeout recomendado | Por que este valor? | O que acontece se exceder? |
|------|-----|--------------------|--------------------|---------------------------|
| Authorization | 0200 | 30s | | |
| Reversal | 0400 | 45s | | |
| Network Management | 0800 | 15s | | |
| Pre-authorization | 0100 | 30s | | |

**Responda também:**
1. Por que o Reversal tem timeout maior que a Autorização?
2. Por que o Network Management tem timeout menor?
3. O que é mais perigoso: timeout na autorização ou timeout no reversal? Justifique.
4. Se você pudesse escolher, preferiria um timeout de 10s ou 60s para autorizações? Por quê?

**Critério de sucesso:** Tabela completa, justificativas técnicas corretas.

---

## Exercício 2 — Cenários de Falha (Nível: Iniciante)

Para cada cenário, descreva o comportamento esperado do switch e o que o portador verá no terminal:

**Cenário A:** Emissor responde em 28s (dentro do timeout de 30s) com DE39=`00`
- O que o switch faz?
- O que o terminal exibe?

**Cenário B:** Emissor não responde (timeout de 30s atingido)
- O que o switch faz?
- O que o terminal exibe?
- Deve ser gerado um reversal? Por quê?

**Cenário C:** Emissor retorna DE39=`91` (Issuer unavailable) em 2s
- O que o switch faz?
- É diferente de um timeout? Por quê?
- Deve ser gerado um reversal?

**Cenário D:** Switch perde conexão TCP com o emissor no meio do envio
- O que o QMUX detecta?
- O que o switch faz?
- A mensagem chegou ao emissor?

**Cenário E:** Stand-in — a bandeira responde em lugar do emissor
- Como o switch sabe que foi stand-in e não o emissor?
- Qual campo indica isso?
- O que acontece quando o emissor voltar?

**Critério de sucesso:** Todos os 5 cenários com comportamento correto descrito.

---

## Exercício 3 — Auto-Reversal por Timeout (Nível: Intermediário)

Implemente `AutoReversalParticipant.java`:

```java
public class AutoReversalParticipant implements TransactionParticipant {

    private MUX reversalMux;
    private long reversalTimeout = 45000L;

    @Override
    public int prepare(long id, Serializable context) {
        // NO_JOIN — apenas observa, não bloqueia o fluxo
        return NO_JOIN;
    }

    @Override
    public void abort(long id, Serializable context) {
        Context ctx = (Context) context;
        Boolean needsReversal = ctx.get("NEEDS_REVERSAL");

        if (Boolean.TRUE.equals(needsReversal)) {
            // Gera 0400 (reversal) baseado no REQUEST original
            // Envia via reversalMux com timeout de 45s
            // Se o reversal também der timeout: loga WARN e adiciona à fila de reconciliação
            // Nunca lança exceção — reversal é melhor esforço
        }
    }
}
```

**Regras do reversal automático:**
1. Copiar DE2, DE3, DE4, DE7, DE11, DE41, DE42 do request original para o 0400
2. DE39 do 0400 deve ser vazio (será preenchido pelo emissor na response)
3. Se o reversal for confirmado (DE39=`00`): logar como `REVERSAL_CONFIRMED`
4. Se o reversal for negado (DE39=`76`): logar como `REVERSAL_NOT_FOUND` (pode ser OK)
5. Se o reversal der timeout: adicionar à `RECONCILIATION_QUEUE`

Implemente testes para os 3 casos de resposta do reversal.

**Critério de sucesso:** Auto-reversal implementado, 3 casos testados.

---

## Exercício 4 — Emissor Lento (Nível: Intermediário)

Implemente `SlowIssuerSimulator.java` que permite controlar a latência de resposta:

```java
public class SlowIssuerSimulator implements ISORequestListener {

    private final long responseDelayMs;  // 0 = normal, 35000 = timeout

    @Override
    public boolean process(ISOSource source, ISOMsg request) {
        // Simula latência antes de responder
        // Responde com DE39=00 (aprovado)
    }
}
```

Configure dois simuladores:
- `normalIssuer`: responde em 150ms
- `slowIssuer`: responde em 35.000ms (35s — acima do timeout de 30s)

Escreva testes:
1. `normalIssuer` → resposta recebida em < 500ms, DE39=`00`
2. `slowIssuer` → timeout após 30s, DE39=`68`, reversal enviado automaticamente
3. `slowIssuer` → resposta chega após timeout → `LateResponseHandler` detecta

**Critério de sucesso:** Os 3 testes passando, tempos corretos.

---

## Exercício 5 — Stand-In: Diferenciando Resposta (Nível: Intermediário)

Quando a bandeira responde em stand-in, pode colocar um indicador no DE44 ou DE63.
Para este exercício, use DE44 com valor `STIP` como indicador de stand-in.

Implemente `StandInDetector.java`:

```java
public class StandInDetector {

    /**
     * Retorna true se a response veio da bandeira em stand-in,
     * false se veio diretamente do emissor.
     */
    public static boolean isStandIn(ISOMsg response) throws ISOException {
        // Verifica DE44 por indicador de stand-in
        // Verifica DE39 por códigos típicos de stand-in
    }

    /**
     * Registra a transação stand-in para envio de advice ao emissor
     * quando ele voltar a ficar disponível.
     */
    public void recordForAdvice(ISOMsg request, ISOMsg standInResponse) { ... }
}
```

Implemente um teste que distingue uma resposta do emissor de uma resposta stand-in.

**Critério de sucesso:** Diferenciação correta, registro para advice implementado.

---

## Exercício 6 — Documento de Estratégia de Timeout (Nível: Intermediário)

Escreva `timeout-strategy.md` como se fosse a documentação oficial do switch:

**Seções obrigatórias:**
1. **SLAs por tipo de mensagem** (tabela com valores e justificativas)
2. **Flowchart de decisão por timeout** (ASCII art ou Mermaid)
3. **Política de auto-reversal** (quando gerar, quando não gerar)
4. **Tratamento de late response** (3 cenários com ação para cada)
5. **Janela de inconsistência** (qual é o pior caso e como mitigar)
6. **Reconciliação** (o que vai para a fila de reconciliação e por quê)

**Critério de sucesso:** Documento completo, decisões claras e bem justificadas.

---

## Exercício 7 — Teste de Stress (Nível: Avançado)

Implemente `StressTest.java` que simula o cenário da teoria:

```
100 transações simultâneas para o switch.
O emissor simulado responde:
  - 70% em < 200ms (normal)
  - 20% em 5-10s (lento)
  - 10% nunca responde (timeout em 30s)
```

```java
public class StressTest {

    @Test
    void stressTest100Transactions() throws Exception {
        // 1. Configura o switch e o emissor simulado
        // 2. Envia 100 transações em paralelo (threads ou CompletableFuture)
        // 3. Coleta resultados
        // 4. Verifica:
        //    - ~70 aprovadas (DE39=00)
        //    - ~20 aprovadas mas lentas (DE39=00, latência > 5s)
        //    - ~10 timeout (DE39=68 ou reversal automático)
        // 5. Calcula e loga P50/P95/P99
    }
}
```

**Métricas obrigatórias no output do teste:**
```
=== Resultado do Stress Test ===
Total enviadas:    100
Aprovadas:          72 (72.0%)
Lentas (>5s):       19 (19.0%)
Timeout:             9  (9.0%)
Reversals gerados:   9
Reversals OK:        7
Reversals timeout:   2 → RECONCILIATION_QUEUE

Latência (ms):
  P50:    87
  P95:  8.432
  P99: 30.001

Tempo total: 32.4s
```

**Critério de sucesso:** Teste roda até o fim, métricas calculadas corretamente.

---

## Exercício 8 — Resiliência: Análise Comparativa (Nível: Avançado)

Escreva `resilience-analysis.md` comparando três estratégias de lidar com emissor indisponível:

| Estratégia | Descrição | Prós | Contras | Quando usar |
|------------|-----------|------|---------|-------------|
| **Fail Fast** | Timeout curto (5s), nega imediatamente | | | |
| **Retry com backoff** | Timeout 30s, tenta 2x com 5s entre tentativas | | | |
| **Stand-In** | Bandeira decide após timeout do emissor | | | |

**Para cada estratégia, calcule o impacto para:**
- Emissor fora por 5 minutos (1.000 transações/minuto)
- Emissor lento (responde em 25s)
- Emissor retornando intermitentemente (responde 50% das vezes)

**Recomendação final:** Qual estratégia você implementaria no seu switch, ou qual combinação? Justifique.

**Critério de sucesso:** Análise completa, recomendação com justificativa técnica e de negócio.
