# Exercícios — Semana 27 — PCI-DSS Prático

## Exercício 1 — CHD vs SAD: O que pode e o que não pode (Fixação)

Classifique cada dado abaixo como **CHD (pode armazenar com proteção)**, **SAD (nunca armazenar após autorização)** ou **Não está no escopo PCI**:

| Dado | Campo ISO 8583 | Classificação | Pode armazenar? |
|------|----------------|---------------|----------------|
| PAN completo (16 dígitos) | DE 2 | ? | ? |
| Track 2 data (tarja magnética) | DE 35 | ? | ? |
| CVV2 / CVC2 | DE 48 subelemento | ? | ? |
| Data de expiração (YYMM) | DE 14 | ? | ? |
| PIN block cifrado | DE 52 | ? | ? |
| ARQC (criptograma EMV) | DE 55 | ? | ? |
| Nome do portador | DE 45 | ? | ? |
| Valor da transação | DE 4 | ? | ? |
| RRN (número de referência) | DE 37 | ? | ? |

Após classificar, responda: Por que a regra "nunca armazenar SAD após autorização" existe? O que um atacante pode fazer com o Track 2?

**Objetivo:** Internalizar a distinção CHD vs SAD que fundamenta toda a arquitetura PCI.

**Dica:** Track 2 permite clonar o cartão físico. CVV2 permite fraude em e-commerce. PIN permite saque em ATM. Esses três, juntos, permitem comprometer completamente um cartão.

---

## Exercício 2 — PAN Masking no Switch (Iniciante/Intermediário)

Implemente `PANMasker` que aplica mascaramento conforme o requisito 3 do PCI-DSS 4.0:

```java
public class PANMasker {

    /**
     * Mascara o PAN para logs: exibe primeiros 6 e últimos 4 dígitos.
     * "4532010000001234" → "453201******1234"
     * Deve funcionar para PANs de 13 a 19 dígitos.
     * PAN nulo ou inválido: retorna "INVALID"
     */
    public static String mask(String pan) { /* ... */ }

    /**
     * Trunca para armazenamento em banco de dados:
     * retorna apenas os últimos 4 dígitos.
     * "4532010000001234" → "1234"
     */
    public static String truncate(String pan) { /* ... */ }

    /**
     * Valida se um log contém PAN não mascarado (para testes de auditoria).
     * Usa algoritmo de Luhn para detectar sequências que parecem PANs.
     * Retorna lista de PANs detectados não mascarados.
     */
    public static List<String> detectUnmaskedPans(String logLine) { /* ... */ }
}
```

Escreva testes para `detectUnmaskedPans` com:
- Log contendo PAN em claro → deve detectar
- Log com PAN mascarado (453201******1234) → não deve detectar
- Log sem PAN → retorna lista vazia

**Objetivo:** Implementar a proteção de PAN em logs que é auditada no PCI-DSS.

---

## Exercício 3 — CDE Scoping: O que está dentro e fora (Intermediário)

Você é o engenheiro responsável por definir o escopo do CDE (Cardholder Data Environment) para o payment-switch-lab.

Para cada componente abaixo, determine se está **dentro do CDE** ou **fora do CDE**, e justifique:

1. Servidor jPOS que processa mensagens ISO 8583 com PAN
2. Banco de dados que armazena PAN mascarado (últimos 4) e token
3. Sistema de BI que recebe apenas tokens (sem PAN)
4. Servidor de logs que captura todas as transações (com `PANMasker` aplicado)
5. Ambiente de desenvolvimento que usa PANs gerados por Luhn (nunca reais)
6. Servidor de monitoramento (Prometheus/Grafana) que coleta métricas sem dados de cartão
7. HSM (Hardware Security Module) que processa PIN blocks

Depois, escreva `cde-scope.md` com:
- Diagrama de rede mostrando o CDE isolado em VLAN
- Justificativa para cada componente incluído/excluído
- Como a tokenização pode remover o sistema de BI do escopo

**Objetivo:** Aprender a minimizar o CDE para reduzir o custo e esforço da certificação PCI.

---

## Exercício 4 — Auditoria de Logs: Detectar Violações PCI (Avançado)

Implemente `PciLogAuditor` que analisa arquivos de log e detecta violações:

```java
public class PciLogAuditor {

    public record Violation(
        String file,
        int lineNumber,
        String violationType,  // UNMASKED_PAN, TRACK_DATA, CVV, PIN_BLOCK
        String evidence,       // trecho do log com a violação
        Severity severity
    ) {}

    /**
     * Analisa um arquivo de log e retorna todas as violações PCI detectadas.
     * Detecta:
     * - PANs não mascarados (via Luhn)
     * - Padrões de track data (;NNNN...=YYMM...)
     * - CVV2 em contexto suspeito (3-4 dígitos após dados de cartão)
     * - PIN blocks em hex (16 chars hex após DE 52)
     */
    public List<Violation> audit(Path logFile) throws IOException { /* ... */ }

    /**
     * Gera relatório de auditoria formatado para o QSA (auditor PCI).
     */
    public String generateReport(List<Violation> violations, LocalDate auditDate) { /* ... */ }
}
```

Crie arquivos de log de teste com violações intencionais e valide que todas são detectadas.

**Objetivo:** Construir a ferramenta que você usaria para auditar o próprio sistema antes de uma certificação PCI.

**Dica:** Padrão de track 2: `;` + 13-19 dígitos + `=` + 4 dígitos (YYMM) + dígitos + `?`. Isso é suficiente para detectar a maioria dos casos.

---

## Exercício 5 — Revisão: PCI-DSS 4.0

**a)** Qual a diferença entre SAQ A e SAQ D? Em qual um adquirente/switch se enquadra?

**b)** O Requisito 6 do PCI-DSS exige que dados reais de produção não sejam usados em ambientes de desenvolvimento. Como você geraria massa de teste com PANs tecnicamente válidos (passam no Luhn) mas que nunca foram cartões reais?

**c)** O Requisito 10 exige retenção de logs por 12 meses (3 meses online, 9 em arquivo). Se seu switch processa 1 milhão de transações/dia e cada linha de log tem 500 bytes, qual o volume de armazenamento necessário para 90 dias online?

**d)** Um desenvolvedor argumenta que armazenar o ARQC (DE 55, criptograma EMV) é seguro porque ele é criptografado e não contém dados do portador. Você concorda? O que o PCI-DSS diz sobre ARQC?

**e)** Qual é o processo correto quando um servidor do CDE é comprometido? Liste as 5 primeiras ações em ordem.

**Objetivo:** Preparar para o desafio da semana e solidificar o conhecimento PCI-DSS 4.0.
