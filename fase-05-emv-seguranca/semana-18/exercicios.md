# Semana 18 — Exercícios de Fixação

## Exercício 1 — Hierarquia de Chaves: Mapeamento (Nível: Iniciante)

Preencha a tabela sem consultar a teoria:

| Sigla | Nome completo | Onde fica? | Para que serve? | Quem conhece? |
|-------|--------------|------------|-----------------|---------------|
| LMK | | | | |
| ZMK | | | | |
| ZPK | | | | |
| ZAK | | | | |
| ZEK | | | | |
| BDK | | | | |
| IPEK | | | | |

Depois responda:
1. Por que o LMK NUNCA sai do HSM?
2. Se o ZPK vazar, quais transações são comprometidas? Qual é o impacto?
3. O que é necessário para trocar o ZPK sem interromper o processamento de transações?

**Critério de sucesso:** Tabela completa e correta, 3 perguntas respondidas com precisão.

---

## Exercício 2 — DUKPT: Rastreando a Derivação de Chaves (Nível: Iniciante/Intermediário)

Dado o seguinte cenário DUKPT:
- Terminal ID: `TERM001`
- Contador de transações: `000042` (42ª transação)
- KSN = Terminal ID + Contador = `TERM001000042`

Responda (sem implementar criptografia, apenas conceitualmente):
1. Qual é a relação entre BDK, IPEK e Session Key?
2. Por que o KSN inclui o contador de transações?
3. Se um atacante capturar a Session Key da transação 42, quais outras transações ele pode descriptografar?
4. Se um atacante capturar a IPEK, quais outras transações ele pode descriptografar?
5. Por que DUKPT é melhor que usar uma única ZPK para todos os terminais?

**Critério de sucesso:** Todas as 5 perguntas respondidas com entendimento claro do isolamento de chaves.

---

## Exercício 3 — Implemente PIN Block Format 0 (Nível: Intermediário)

O PIN Block Format 0 (ISO 9564) é construído assim:

```
PIN Block = XOR(PIN Block claro, PAN Block)

PIN Block claro: 0 + Length(PIN) + PIN + F(padding)
PAN Block:       0000 + PAN[posições 3-14] (12 dígitos do PAN)
```

Exemplo:
- PIN: `1234`
- PAN: `4532 0151 1283 0366`
- PIN Block claro: `0412 34FF FFFF FFFF`
- PAN Block:      `0000 3201 5112 8303`
- PIN Block final: XOR dos dois

Implemente:

```java
public class PINBlockFormat0 {
    /**
     * Gera o PIN Block (sem criptografia — apenas o XOR)
     * @param pin String com os dígitos do PIN (ex: "1234")
     * @param pan String com o PAN completo (ex: "4532015112830366")
     * @return byte[] com o PIN Block de 8 bytes
     */
    public static byte[] encode(String pin, String pan) { /* ... */ }

    /**
     * Decodifica o PIN Block para obter o PIN original
     * (só possível com o PAN — privacidade por design)
     */
    public static String decode(byte[] pinBlock, String pan) { /* ... */ }
}
```

Escreva testes:
1. PIN `1234` + PAN correto → encode → decode → `1234`
2. PIN `0000` (limite inferior válido) → encode correto
3. PIN `999999` (6 dígitos) → encode correto
4. PAN diferente no decode → resultado incorreto (não o PIN original) — isso é o esperado

**Critério de sucesso:** encode/decode funcional, round-trip correto com o PAN correto.

---

## Exercício 4 — Implemente PANMasker com Testes Extensivos (Nível: Intermediário)

Implemente `PANMasker` seguindo as regras PCI DSS:

```java
public class PANMasker {
    // PCI DSS: mostrar no máximo first 6 + last 4
    public static String mask(String pan) { /* ... */ }

    // Para exibição de BIN (8 dígitos): mostrar first 8 + last 4
    public static String maskWithBin8(String pan) { /* ... */ }

    // Para logs: never log PAN, track, PIN, CVV
    public static String sanitizeForLog(ISOMsg msg) throws ISOException { /* ... */ }
}
```

Escreva **pelo menos 10 casos de teste** cobrindo:
1. PAN de 13 dígitos (menor PAN válido)
2. PAN de 16 dígitos (padrão Visa/MC)
3. PAN de 19 dígitos (máximo)
4. PAN nulo → retornar "INVALID_PAN"
5. PAN vazio → retornar "INVALID_PAN"
6. PAN com 12 dígitos (inválido) → retornar "INVALID_PAN"
7. PAN com caracteres não numéricos → comportamento definido
8. Verificar que o PAN completo NÃO aparece no resultado
9. Verificar que os últimos 4 dígitos SÃO visíveis
10. Verificar que o resultado tem o mesmo comprimento do PAN original

**Critério de sucesso:** Todos os 10+ testes passam, PAN nunca exposto.

---

## Exercício 5 — Auditoria de Segurança: Encontre os Problemas (Nível: Intermediário)

O código abaixo tem **5 violações de segurança PCI DSS**. Encontre todas e corrija.

```java
public class InsecurePaymentLogger {

    private static final Logger log = LoggerFactory.getLogger(InsecurePaymentLogger.class);

    public void logTransaction(ISOMsg msg) throws ISOException {
        // Log completo da transação
        log.info("Received transaction: MTI={} PAN={} Amount={} STAN={}",
            msg.getMTI(),
            msg.getString(2),           // VIOLAÇÃO?
            msg.getString(4),
            msg.getString(11));

        // Log do PIN para debug
        if (msg.hasField(52)) {
            log.debug("PIN block received: {}", msg.getString(52));  // VIOLAÇÃO?
        }

        // Log do track data
        if (msg.hasField(35)) {
            log.info("Track 2 data: {}", msg.getString(35));         // VIOLAÇÃO?
        }

        // Salvar em arquivo de auditoria
        try (FileWriter fw = new FileWriter("/tmp/transactions.log", true)) {
            fw.write("PAN=" + msg.getString(2) + "\n");              // VIOLAÇÃO?
            fw.write("Amount=" + msg.getString(4) + "\n");
        }

        // Retornar PAN no response para facilitar debug
        ISOMsg response = new ISOMsg();
        response.set(2, msg.getString(2));  // VIOLAÇÃO?
        response.set(39, "00");
    }
}
```

Para cada violação: identifique, explique o risco e escreva o código corrigido.

**Critério de sucesso:** 5 de 5 violações encontradas e corrigidas.

---

## Exercício 6 — Política de Segurança: security-policy.md (Nível: Avançado)

Escreva `security-policy.md` com a política de segurança do payment-switch-lab. O documento deve cobrir:

**1. Dados que NUNCA podem ser logados:**
- Lista completa com: campo ISO 8583, nome, motivo da restrição

**2. Dados que PODEM ser logados com mascaramento:**
- PAN: regra de mascaramento, formato, exemplo
- Outros campos sensíveis com regras específicas

**3. Retenção de dados:**
- Logs de transação: por quanto tempo?
- Logs de segurança (auditoria): por quanto tempo?
- Chaves criptográficas: ciclo de vida

**4. Transmissão segura:**
- Quais canais usam TLS?
- Versão mínima de TLS
- Quais dados são criptografados em repouso?

**5. Resposta a incidentes de segurança:**
- Quem notificar?
- Em quanto tempo?
- O que preservar como evidência?

**Critério de sucesso:** Política completa, tecnicamente precisa, alinhada com PCI DSS.

---

## Exercício 7 — PIN Translation: O Fluxo Completo (Nível: Avançado)

**Cenário:** Portador digita PIN no terminal do adquirente A. A transação precisa chegar ao emissor B, passando pela bandeira C (que atua como intermediário). Cada nó tem sua própria ZPK.

1. Desenhe o fluxo completo de PIN translation em um diagrama Mermaid (`pin-translation.mermaid`)
2. Identifique em que momento o PIN em claro existe (spoiler: nunca fora do HSM)
3. Quantas operações criptográficas o HSM do adquirente A realiza?
4. Quantas operações criptográficas o HSM da bandeira C realiza?
5. Se o HSM do adquirente A estiver fora do ar, o que acontece com as transações que precisam de PIN?

**Critério de sucesso:** Diagrama correto, PIN nunca em claro fora do HSM, impacto do HSM fora do ar analisado.
