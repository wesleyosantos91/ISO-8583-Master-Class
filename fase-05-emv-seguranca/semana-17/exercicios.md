# Semana 17 — Exercícios de Fixação

## Exercício 1 — Decodificando TLV Manualmente (Nível: Iniciante)

Dado o seguinte DE 55 em hexadecimal:

```
9F2608AABBCCDDEE1122339F2701809F100706010A03A4A0009A03260315
```

Decodifique manualmente, preenchendo a tabela:

| Posição (bytes) | Tag (hex) | Length | Value (hex) | Significado |
|----------------|-----------|--------|-------------|-------------|
| 0-1 | 9F26 | | | |
| ... | 9F27 | | | |
| ... | 9F10 | | | |
| ... | 9A | | | |

Depois responda:
1. O criptograma (tag 9F26) é ARQC, TC ou AAC? Como você sabe?
2. A tag 9A tem valor `260315`. Que data isso representa?
3. Por que a tag 9F26 tem 2 bytes de identificação (9F + 26) em vez de apenas 1?

**Critério de sucesso:** Tabela preenchida corretamente, tipo de criptograma identificado.

---

## Exercício 2 — Tags Obrigatórias por Tipo de Transação (Nível: Iniciante)

Para cada tipo de transação abaixo, liste quais tags EMV são obrigatórias no DE 55:

1. **Chip contact (DE22=051), compra presencial:**
   - Mínimo: 9F26, 9F27, 9F10, 9F37, 9F36, 95, 9A, 9C, 9F02, 9F1A, 5F2A, 82, 84
   - Por que cada uma é necessária?

2. **Contactless (DE22=071), compra abaixo do limite de CVM:**
   - Quais tags da lista acima podem estar ausentes?
   - Quais tags adicionais aparecem?

3. **Fallback (DE22=801), tarja magnética após falha do chip:**
   - O DE 55 deve estar presente?
   - Se presente, quais tags estariam lá?

**Critério de sucesso:** Distinções corretas entre chip, contactless e fallback.

---

## Exercício 3 — Implemente TLVParser (Nível: Intermediário)

Implemente `TLVParser` em Java:

```java
public class TLVParser {
    /**
     * Parseia um byte array TLV e retorna Map<tag, value>
     * onde tag é String hex uppercase e value é byte[]
     */
    public static Map<String, byte[]> parse(byte[] data) { /* ... */ }

    /**
     * Constrói um byte array TLV a partir de um Map<tag, value>
     */
    public static byte[] build(Map<String, byte[]> tags) { /* ... */ }

    /**
     * Helper: converte byte[] para String hex uppercase
     */
    public static String toHex(byte[] data) { /* ... */ }
}
```

Testes obrigatórios:
1. Parse de tag 1 byte com length 1 byte: `9C0100` → `{"9C": [0x00]}`
2. Parse de tag 2 bytes com length 1 byte: `9F260801020304050607` → `{"9F26": [8 bytes]}`
3. Parse de múltiplas tags concatenadas (o exemplo do exercício 1)
4. Parse de tag com length multi-byte: `9F10` + `81 0A` + `10 bytes` (length = 10 usando encoding 0x81)
5. Build → Parse → deve produzir o mapa original (round-trip)

**Critério de sucesso:** Todos os 5 testes passam, incluindo tags multi-byte e lengths multi-byte.

---

## Exercício 4 — Implemente DE55Analyzer (Nível: Intermediário)

Implemente `DE55Analyzer` que recebe o DE 55 raw e produz uma análise legível:

```java
public class DE55Analyzer {
    public DE55Analysis analyze(byte[] de55) { /* ... */ }
}

public record DE55Analysis(
    String cryptogramType,    // "ARQC", "TC" ou "AAC"
    String cryptogramHex,     // valor da tag 9F26
    int atc,                  // Application Transaction Counter (9F36)
    String transactionDate,   // formatado YYYY-MM-DD (da tag 9A)
    String cvmResult,         // "PIN", "Signature", "No CVM", "Unknown"
    String transactionType,   // "Purchase", "Cash", "Refund"
    boolean isContactless,    // derivado das tags presentes
    List<String> warnings     // ex: "ATC=0 pode indicar replay attack"
) {}
```

Regras:
- CID (tag 9F27): 0x80 = ARQC, 0x40 = TC, 0x00 = AAC
- CVM Results (tag 9F34): bytes 1-2 interpretados conforme spec EMV
- ATC = 0: warning de possível replay
- ATC decrementou (menor que último visto para esse PAN): warning de replay

Teste com pelo menos 3 amostras diferentes de DE 55 (chip, contactless, fallback).

**Critério de sucesso:** Análise correta para os 3 tipos, warnings gerados quando aplicável.

---

## Exercício 5 — Chip vs Fallback: Análise de Risco (Nível: Intermediário)

**Cenário:** O portador passa o cartão chip. O terminal tenta ler o chip 3 vezes e falha (chip danificado). O terminal solicita ao portador que passe na tarja magnética (fallback). A transação é aprovada.

Responda:
1. O DE 22 na transação de fallback é qual valor?
2. O DE 55 está presente em uma transação de fallback? Com quais dados?
3. Qual bandeira pode exigir que o emissor não aprove fallback em certos mercados?
4. Por que fallback é mais arriscado que chip?
5. Se você fosse o emissor, quais regras aplicaria para transações de fallback? (ex: limite de valor, bloqueio em certos países)

**Critério de sucesso:** DE22 correto, análise de risco completa, política de emissor proposta.

---

## Exercício 6 — Comparativo: Chip Contact vs Contactless (Nível: Avançado)

Dado dois dumps de DE 55 (um chip contact, um contactless), construa uma tabela comparativa tag a tag.

Use os seguintes dados de exemplo:

**Chip Contact:**
```
9F2608A1B2C3D4E5F60102 9F270180 9F360200A5
9F370411223344 950500000000 9A03260315 9C0100
9F02060000000015009F1A0207649F33033F00C8
9F340103200000
```
(concatenado, espaços apenas para legibilidade)

**Contactless:**
```
9F2608F1E2D3C4B5A60102 9F270180 9F360200A6
9F370455667788 950500000000 9A03260315 9C0100
9F02060000000015009F1A020764
```
(sem tag 9F34 — CVM não executado abaixo do limite)

Compare:
1. Qual tag está presente no chip mas ausente no contactless?
2. O ATC é diferente (A5 vs A6) — o que isso indica?
3. O Unpredictable Number (9F37) é diferente — por quê isso é importante para segurança?
4. Qual transação tem maior risco de fraude e por quê?

Salve em `chip-vs-contactless-comparison.md`.

**Critério de sucesso:** Comparação tag a tag correta, análise de segurança precisa.

---

## Exercício 7 — ARQC e Anti-Replay (Nível: Avançado)

Um atacante captura um DE 55 completo de uma transação legítima (incluindo tag 9F26 com ARQC válido) e tenta reenviar a mesma mensagem para o emissor.

Explique por que o emissor deve detectar e rejeitar essa tentativa de replay:

1. **Unpredictable Number (tag 9F37):** Por que este campo previne replay? Quem gera este valor?
2. **ATC (tag 9F36):** Como o emissor usa o ATC para detectar replay? O que acontece se o ATC recebido é menor que o último ATC visto para aquele cartão?
3. **O ARQC em si:** Mesmo que o atacante reenvie o ARQC válido, por que o emissor pode calculá-lo novamente e detectar que os dados não batem?

Implemente um `AntiReplayValidator`:

```java
public class AntiReplayValidator {
    // Armazena último ATC por PAN (mascarado)
    private final Map<String, Integer> lastAtcByPan;

    /**
     * Retorna false se detectar possível replay
     */
    public boolean validate(ISOMsg msg) throws ISOException { /* ... */ }
}
```

**Critério de sucesso:** Três mecanismos de anti-replay explicados corretamente, `AntiReplayValidator` implementado.
