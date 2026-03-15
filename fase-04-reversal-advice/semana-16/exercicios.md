# Semana 16 — Exercícios de Fixação

## Exercício 1 — O Problema da Duplicata (Nível: Iniciante)

**Cenário:** Um portador tenta pagar R$ 150,00 em uma farmácia. O terminal envia o 0200. O roteador de rede tem um problema e a mensagem é entregue duas vezes para o switch. O switch encaminha as duas para o emissor. O emissor processa as duas.

Responda sem código:
1. O portador será cobrado R$ 150 ou R$ 300?
2. O terminal recebeu duas respostas ou uma?
3. Quem tem a responsabilidade de detectar e prevenir isso: o terminal, o switch ou o emissor? Por quê?
4. Como o STAN (DE 11) pode ajudar a detectar duplicatas?
5. Por que usar apenas o STAN como chave de deduplicação não é suficiente?

**Critério de sucesso:** 5 de 5 perguntas respondidas corretamente, com raciocínio claro.

---

## Exercício 2 — Montando a Chave de Deduplicação (Nível: Iniciante/Intermediário)

A chave de deduplicação deve ser única para cada transação legítima, mas igual para todas as retransmissões da mesma transação.

Para cada proposta de chave abaixo, analise se é boa ou ruim e por quê:

| Proposta de Chave | Boa ou Ruim? | Por quê? |
|-------------------|-------------|----------|
| Apenas STAN (DE 11) | | |
| STAN + TerminalID (DE 41) | | |
| STAN + TerminalID + Amount (DE 4) | | |
| STAN + TerminalID + Amount + PAN últimos 4 | | |
| STAN + TerminalID + Amount + ProcessingCode (DE 3) | | |
| PAN completo + Amount + DateTime | | |
| RRN (DE 37) | | |

Depois defina a chave ideal e justifique sua escolha considerando: unicidade, privacy, colisões possíveis, campos sempre presentes.

**Critério de sucesso:** Análise correta para todas as propostas, chave ideal escolhida com justificativa sólida.

---

## Exercício 3 — Implemente DuplicateChecker (Nível: Intermediário)

Implemente `DuplicateChecker` como `TransactionParticipant` usando Caffeine cache:

```java
public class DuplicateChecker implements TransactionParticipant {
    private final Cache<String, ISOMsg> recentTransactions;

    public DuplicateChecker(Duration ttl, int maxSize) { /* ... */ }

    @Override
    public int prepare(long id, Serializable context) { /* ... */ }

    @Override
    public void commit(long id, Serializable context) { /* ... */ }

    private String buildDedupKey(ISOMsg msg) throws ISOException { /* ... */ }
}
```

Requisitos:
- TTL: configurável (default 5 minutos)
- Tamanho máximo: configurável (default 100.000 entradas)
- Em caso de duplicata: retornar `ABORTED` com a resposta anterior no contexto
- Em caso de duplicata: incrementar métrica `duplicates_detected_total`
- No `commit`: armazenar a resposta para futuras detecções
- Chave: STAN + TerminalID + Amount + PAN(últimos 4) + ProcessingCode

Escreva testes:
1. Mesma mensagem 2x → segunda retorna resposta cacheada (DE39 igual)
2. Mensagens com STAN diferente → ambas processadas normalmente
3. Mensagem após TTL expirar → processada como nova
4. Mensagem com mesmo STAN mas TerminalID diferente → processada normalmente (não é duplicata)

**Critério de sucesso:** Todos os 4 testes passam, métricas incrementadas.

---

## Exercício 4 — Retransmissão vs Duplicata: As Diferenças (Nível: Intermediário)

Preencha a tabela comparativa:

| Aspecto | Retransmissão Legítima | Duplicata Indevida |
|---------|------------------------|-------------------|
| MTI | | |
| Quem gera? | | |
| Motivo | | |
| O receptor deve processar de novo? | | |
| Como detectar? | | |
| O que retornar? | | |

**Scenario Quiz:** Para cada mensagem abaixo, classifique como "retransmissão legítima", "duplicata indevida" ou "nova transação":

1. MTI=0201, STAN=111111, TerminalID=TERM01, Amount=10000 (anterior: MTI=0200, STAN=111111, TERM01, 10000)
2. MTI=0200, STAN=111111, TerminalID=TERM01, Amount=10000 (anterior: MTI=0200, STAN=111111, TERM01, 10000)
3. MTI=0200, STAN=111112, TerminalID=TERM01, Amount=10000 (anterior: MTI=0200, STAN=111111, TERM01, 10000)
4. MTI=0200, STAN=111111, TerminalID=TERM02, Amount=10000 (anterior: MTI=0200, STAN=111111, TERM01, 10000)

**Critério de sucesso:** Tabela e quiz todos corretos.

---

## Exercício 5 — Teste de Carga com Duplicatas (Nível: Intermediário/Avançado)

Implemente um teste automatizado que:

1. Gera 1.000 transações únicas (ISOMsg com STANs e PANs diferentes)
2. Para 10% delas (100 transações): cria uma cópia exata (simula duplicata)
3. Embaralha a ordem das 1.100 mensagens
4. Envia todas pelo `DuplicateChecker`
5. Verifica ao final:
   - Exatamente 1.000 transações foram processadas (não mais)
   - Exatamente 100 foram detectadas como duplicatas
   - Nenhuma das 100 duplicatas gerou uma nova entrada no cache

```java
@Test
void testDuplicateDetectionAtScale() {
    // Implemente aqui
    assertThat(processed).isEqualTo(1000);
    assertThat(duplicatesDetected).isEqualTo(100);
    assertThat(cacheSize).isLessThanOrEqualTo(1000);
}
```

**Critério de sucesso:** Teste passa, contagens exatas, sem vazamento de memória (cache dentro do limite).

---

## Exercício 6 — Política de Deduplicação: Documentação (Nível: Intermediário)

Escreva `dedup-policy.md` documentando a política de deduplicação do seu switch. O documento deve cobrir:

1. **Definição:** O que é considerada duplicata no nosso sistema?
2. **Chave de deduplicação:** Quais campos, por que esses campos, e não outros?
3. **Janela temporal:** Por que 5 minutos? O que acontece se dois STANs iguais chegam com 6 minutos de diferença?
4. **Casos especiais:**
   - Retransmissão legítima (0201): tratada como duplicata ou não?
   - SAF duplicado: como diferenciar de duplicata maliciosa?
   - Dois terminais diferentes com mesmo STAN (por que isso pode acontecer e como tratar?)
5. **Limites do sistema:** O que o DuplicateChecker NÃO cobre (e precisa de controle no emissor)?

**Critério de sucesso:** Documento técnico completo, casos especiais cobertos.

---

## Exercício 7 — Análise de Memória e Sizing (Nível: Avançado)

O switch processa 500 transações por segundo no pico. O TTL do cache de dedup é 5 minutos.

Calcule:
1. Quantas entradas o cache pode ter no máximo durante o pico?
2. Cada entrada ocupa aproximadamente: chave (50 bytes) + ISOMsg serializada (~500 bytes) = 550 bytes. Qual o uso total de memória do cache no pico?
3. Se a JVM tem 512 MB disponível para o cache, quantas entradas podem existir?
4. Com base nesses números, o `maximumSize(100_000)` é adequado ou deve ser ajustado?
5. Qual é a estratégia de eviction do Caffeine quando o cache está cheio? Como isso pode causar falsos negativos (duplicata não detectada)?

Documente em `cache-sizing-analysis.md`.

**Critério de sucesso:** Cálculos corretos, sizing justificado, risco de falso negativo identificado.
