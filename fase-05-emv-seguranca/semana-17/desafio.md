# Semana 17 — Desafio Integrador

## O Cenário

Você é engenheiro no time de autorização de um emissor bancário. O time de prevenção a fraudes detectou uma anomalia:

> "Nos últimos 3 dias, identificamos 89 transações suspeitas de cartões chip que foram aprovadas mesmo com dados EMV inconsistentes. Os cartões afetados pertencem a portadores que viajaram recentemente para o exterior. Acreditamos que alguém copiou os dados do DE 55 de transações legítimas e está fazendo replay attacks."

Análise preliminar do time de fraude:
> "Olhamos os DE 55 dessas 89 transações. Os criptogramas (tag 9F26) parecem válidos, mas notamos que o ATC (tag 9F36) de algumas transações é menor que o ATC de transações anteriores do mesmo cartão. Em outras, o Unpredictable Number (tag 9F37) se repete."

## Sua Missão

### Parte 1 — Análise Forense do DE 55 (30 min)

Escreva `forensics-s17.md` com:

1. **Explique o ataque.** Como exatamente um atacante captura e replica o DE 55? Quais equipamentos e técnicas são usados? (Descreva em termos técnicos: skimming, shimming, etc.)

2. **Analise os dois indicadores de fraud detectados:**
   - **ATC menor que anterior:** Por que isso é fisicamente impossível em um cartão legítimo? O que indica quando acontece?
   - **Unpredictable Number repetido:** Quem gera o Unpredictable Number? Por que sua repetição indica replay?

3. **Por que as 89 transações foram aprovadas?** Liste as falhas no processo de validação que permitiram isso:
   - Falha na validação do ATC
   - Falha na validação do Unpredictable Number
   - Possível falha na validação do próprio ARQC

4. **Dado o DE 55 abaixo de uma das 89 transações suspeitas, e o DE 55 de uma transação legítima anterior do mesmo cartão, identifique as evidências de replay:**

   **Transação legítima (D-3):**
   - Tag 9F36 (ATC): `00C8` (= 200 decimal)
   - Tag 9F37 (UN): `A1B2C3D4`

   **Transação suspeita:**
   - Tag 9F36 (ATC): `00C5` (= 197 decimal)
   - Tag 9F37 (UN): `A1B2C3D4`

   Quais são as 2 evidências de replay nesta comparação?

### Parte 2 — Implementação da Defesa (25 min)

Implemente em `EMVReplayDefense.java`:

```java
public class EMVReplayDefense implements TransactionParticipant {

    /**
     * Valida o DE 55 contra ataques de replay.
     * Deve verificar:
     * 1. ATC > último ATC registrado para este PAN
     * 2. Unpredictable Number não foi visto recentemente (últimas 24h para este PAN)
     * 3. ATC não está muito acima do esperado (gap > 100 pode indicar clonagem em lote)
     */
    @Override
    public int prepare(long id, Serializable context) { /* ... */ }
}
```

Requisitos:
- Persistir último ATC por PAN (use Map em memória para o exercício)
- Janela de UNs vistos: últimas 24 horas por PAN (use Cache Caffeine)
- Se ATC recebido <= ATC armazenado: DE39=57 (Transaction Not Permitted)
- Se UN já visto nas últimas 24h: DE39=57
- Se gap de ATC > 100: DE39=57 + log de alerta para time de fraude
- Se DE 55 ausente em transação chip (DE22=051): DE39=55 (PIN Incorrect, código padrão para chip data missing)

Escreva testes para cada cenário de falha.

### Parte 3 — Comunicação e Remediação (15 min)

1. **Para os 89 cartões afetados:** Que ação imediata tomar? (Bloqueio, substituição, investigação?)

2. **Relatório para o CISO** (Chief Information Security Officer): Em 10 linhas, explique:
   - O que aconteceu
   - Por que as defesas falharam
   - O que foi implementado
   - O que ainda precisa ser feito

3. **Melhoria de longo prazo:** Além da validação de ATC e UN, quais outras verificações um emissor robusto deve fazer no DE 55? Liste pelo menos 3.

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| Análise forense técnica e precisa do ataque | /25 |
| Identificação correta das evidências de replay nos dados fornecidos | /20 |
| `EMVReplayDefense` implementado corretamente com todos os cenários | /25 |
| Testes cobrindo todos os casos de falha | /15 |
| Comunicação ao CISO é técnica e clara | /15 |

**Meta:** 80+ pontos = Semana 17 dominada.

---

## Dicas

- O ATC é um contador monotonicamente crescente. Nunca vai para trás em um cartão legítimo.
- O Unpredictable Number é gerado pelo terminal (não pelo chip) — é um número aleatório fresh para cada transação. Se dois terminais diferentes gerarem o mesmo UN (colisão), não é replay. Mas se o MESMO terminal gerar o mesmo UN para o mesmo cartão, é replay.
- Não bloqueie todos os 89 cartões imediatamente sem confirmar o ataque — alguns podem ser falsos positivos (chip com bug que não incrementa ATC corretamente).
- Em produção, validação de ARQC exige HSM. Para este exercício, foque na validação de ATC e UN (que não exigem HSM).
