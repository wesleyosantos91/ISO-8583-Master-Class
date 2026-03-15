# Semana 23 — Desafio Integrador

## O Cenário

Você é o engenheiro responsável pela reconciliação em um adquirente de médio porte. É segunda-feira, 07h30. Você abre o e-mail e encontra:

> **De:** Sistema de Reconciliação Automático
> **Assunto:** FALHA NA RECONCILIAÇÃO — SEXTA-FEIRA 14/03
>
> "O processo de reconciliação automático falhou ao processar o arquivo de clearing de sexta-feira. Motivo: `java.lang.OutOfMemoryError: Java heap space` ao tentar carregar o arquivo de 847MB em memória.
>
> Dados não reconciliados:
> - Autorizações de sexta: 47.832 transações, R$ 8.234.100,00
> - Arquivo de clearing da bandeira: 46.991 transações, R$ 8.047.830,00
> - Diferença aparente: 841 transações, R$ 186.270,00"

Ao mesmo tempo, você recebe outro alerta:

> "3 merchants estão reclamando que não receberam o pagamento referente às vendas de sexta. Total reclamado: R$ 47.200,00"

E um terceiro:

> "Time de prevenção a fraudes: identificamos um padrão suspeito — 15 transações autorizadas de alto valor (> R$ 5.000 cada) de um único BIN (4532xxxx) que não aparecem no clearing."

## Sua Missão

### Parte 1 — Resolução do OutOfMemoryError (25 min)

Escreva `fix-oom-s23.md`:

1. **Por que o sistema falhou?** O arquivo de clearing de sexta (847MB) é muito grande para carregar em memória. Explique o problema técnico.

2. **Implemente** `StreamingClearingParser` que processa o arquivo em chunks sem carregar tudo em memória:

```java
public class StreamingClearingParser {
    /**
     * Processa o arquivo de clearing linha a linha,
     * chamando o consumer para cada registro.
     * Não mantém mais de um chunk em memória.
     */
    public void processStream(
        Path clearingFile,
        int chunkSize,
        Consumer<List<ClearingRecord>> chunkConsumer
    ) throws IOException { /* ... */ }
}
```

3. **Calcule o uso de memória:** Com chunks de 1.000 registros e cada `ClearingRecord` ocupando ~200 bytes, qual é o uso máximo de memória durante o processamento de 47.000 registros?

4. **Implemente a reconciliação streaming** que usa o `StreamingClearingParser` para processar os 47.000 registros sem OOM.

### Parte 2 — Investigação dos 3 Casos (35 min)

Escreva `investigacao-casos-s23.md`:

**Caso 1: 841 transações sem clearing**

Dos 841 = 47.832 - 46.991, algumas podem ser:
- Transações de sexta-feira que o merchant faz batch close apenas segunda
- Reversals que chegaram após o fechamento do clearing
- Transações com timeout no processo de captura

Implemente a análise:
```java
public class UnmatchedAuthAnalyzer {
    /**
     * Para cada auth sem clearing, classifica como:
     * - PENDING_CAPTURE (< 2 dias): normal, merchant pode capturar ainda
     * - EXPIRED (> 7 dias): auth expirada, limite deve ser liberado
     * - SUSPICIOUS (alto valor + padrão incomum): encaminhar para fraudes
     */
    public List<ClassifiedUnmatch> classify(List<AuthRecord> unmatchedAuths) { /* ... */ }
}
```

**Caso 2: 3 merchants sem pagamento**

Para cada merchant reclamante, o processo de investigação é:
1. Confirmar se as transações estão no arquivo de clearing
2. Confirmar se o arquivo chegou à bandeira
3. Confirmar se o settlement foi executado
4. Verificar se o merchant tem conta bancária correta cadastrada

Escreva `merchant-payment-investigation.md` com o processo de investigação para cada possível ponto de falha.

**Caso 3: 15 transações de alto valor sem clearing**

São transações suspeitas: valores altos (> R$ 5.000), mesmo BIN, sem clearing. O time de fraudes quer saber se são legítimas.

Implemente `FraudIndicatorAnalyzer`:
```java
public class FraudIndicatorAnalyzer {
    /**
     * Analisa um conjunto de transações e retorna indicadores de risco:
     * - Concentração por BIN
     * - Concentração por valor
     * - Concentração por merchant
     * - Velocidade (muitas transações em pouco tempo)
     * - Presença no clearing (ausência pode indicar fraude por clonagem)
     */
    public FraudRiskReport analyze(List<AuthRecord> transactions) { /* ... */ }
}
```

### Parte 3 — Comunicação e Processo (10 min)

Escreva `comunicacao-s23.md` com:

1. **Para os 3 merchants reclamantes:** Resposta objetiva de quanto tempo para resolver e o que está sendo feito.

2. **Para o time de fraudes:** O que fazer com as 15 transações suspeitas? Bloquear os cartões? Reverter as transações? Aguardar?

3. **Melhoria do processo:** O arquivo de clearing de 847MB não deveria chegar na segunda-feira — deveria ter sido processado na sexta. Quais são as mudanças de processo e técnicas para garantir que isso não aconteça novamente?

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| `StreamingClearingParser` implementado corretamente sem OOM | /25 |
| Cálculo correto do uso de memória | /10 |
| `UnmatchedAuthAnalyzer` com classificação correta | /20 |
| Processo de investigação dos merchants documentado | /15 |
| `FraudIndicatorAnalyzer` com indicadores relevantes | /20 |
| Comunicação adequada para cada audiência | /10 |

**Meta:** 80+ pontos = Semana 23 dominada.

---

## Dicas

- 847MB em heap com uma única lista Java → OOM em qualquer JVM com menos de 3-4GB. Streaming não é opção — é obrigação para arquivos grandes.
- 841 transações sem clearing em 47.832 = 1.76%. Para um adquirente saudável, < 1% é normal. 1.76% é aceitável mas merece investigação.
- As 15 transações de alto valor sem clearing: podem ser legítimas (merchant com batch close semanal) ou fraude (autorização obtida fraudulentamente mas não capturada porque o fraudador só queria bloquear o limite). Ambas as hipóteses precisam ser investigadas.
- Merchants reclamando de não receber pagamento: quase sempre é um problema de dados bancários ou de agendamento na câmara de compensação, não de transação.
- Cronometre. Em produção, a reconciliação de sexta precisa estar resolvida antes de terça-feira (D+2 de settlement). Você tem menos de 2 dias.
