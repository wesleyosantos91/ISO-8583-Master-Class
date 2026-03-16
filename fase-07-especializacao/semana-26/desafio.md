# Desafio — Semana 26 — Locadora de Veículos: O Ciclo Completo de Pre-Authorization

## Contexto

Locadoras de veículos são um dos casos de uso mais complexos para pre-authorization no mundo real. O valor final é desconhecido no check-in, pode ser aumentado incrementalmente durante a locação (danos, combustível, extensão de prazo), e pode gerar disputa de chargeback se o portador contestar cobranças adicionais.

Você foi escalado para implementar o fluxo completo de autorização para uma locadora cliente do adquirente. O analista de negócio documentou os requisitos:

> "No check-in, bloqueamos R$ 5.000 de caução no cartão do cliente. Se houver danos ao veículo, cobramos até R$ 2.000 adicionais. No checkout, cobramos o valor real da locação. Se o cliente reservou mas não buscou o carro, cobramos R$ 200 de no-show."

## O Problema

O switch atual não suporta:
1. Incremental authorization (aumentar o valor de uma pre-auth existente)
2. No-show charges (cobrar sem o portador presente, usando credencial armazenada)
3. Partial release (liberar parte do bloqueio no checkout se o valor final for menor)

Sem isso, a locadora é obrigada a fazer novas autorizações para cada cobrança adicional, travando múltiplas vezes o limite do portador.

## Missão

### Parte 1 — Implementar o Ciclo Completo

Implemente os fluxos no `payment-switch-lab`. Para cada mensagem, documente os campos ISO 8583 exatos.

**Cenário A — Locação normal sem danos:**
```
1. Check-in:    Pre-auth R$ 5.000 (DE25=06)
2. Checkout:    Completion R$ 1.800 (DE25=12, valor real da locação)
3. Liberação:   R$ 3.200 do limite deve ser liberado automaticamente
```

**Cenário B — Locação com dano descoberto no checkout:**
```
1. Check-in:    Pre-auth R$ 5.000 (DE25=06)
2. Dano:        Incremental +R$ 1.500 (DE25=10, referenciando a pre-auth)
3. Checkout:    Completion R$ 6.200 (R$ 1.800 locação + R$ 1.500 dano + R$ 2.900 saldo)
   → Valor total: R$ 6.200 > R$ 5.000 original + 15% = R$ 5.750 → requer nova verificação
```

**Cenário C — No-show:**
```
1. Reserva:     CIT R$ 0 para armazenar credencial (network transaction ID gerado)
2. No-show:     MIT R$ 200 (DE25=71, DE22=100, DE48 com tipo 08 e network transaction ID)
```

Implemente `RentalFlowOrchestrator`:

```java
public class RentalFlowOrchestrator {

    public PreAuthResult checkIn(String pan, BigDecimal depositAmount) { /* ... */ }

    public IncrementalAuthResult addDamageCharge(
        String originalAuthCode,
        BigDecimal incrementAmount
    ) { /* ... */ }

    public CompletionResult checkOut(
        String originalAuthCode,
        BigDecimal finalAmount
    ) { /* ... */ }

    public NoShowResult chargeNoShow(
        String storedNetworkTxnId,
        BigDecimal penaltyAmount
    ) { /* ... */ }
}
```

### Parte 2 — Validação dos Limites de Incremento

Visa permite incremental authorization de até 15% acima do valor original para locadoras. Implemente a validação:

```java
public class IncrementalAuthValidator {

    /**
     * Valida se o incremento é permitido.
     * Regras:
     * - Visa hotel/car: até 15% do total acumulado
     * - Se ultrapassar 15%: retornar REQUIRES_NEW_PREAUTH
     * - Pre-auth expirada: retornar EXPIRED
     * - Auth code não encontrado: retornar NOT_FOUND
     */
    public ValidationResult validate(
        String originalAuthCode,
        BigDecimal currentTotal,
        BigDecimal requestedIncrement,
        String merchantCategoryCode
    ) { /* ... */ }
}
```

Teste com:
- Pre-auth R$ 5.000 + incremento R$ 700 → aceito (700/5000 = 14%)
- Pre-auth R$ 5.000 + incremento R$ 800 → rejeitar (800/5000 = 16% > 15%)

### Parte 3 — Documentação do Fluxo

Escreva `rental-flow-diagram.md` com:

1. Diagrama de sequência (texto ASCII ou Mermaid) do Cenário B completo mostrando todas as mensagens ISO 8583 trocadas entre: Terminal POS → Switch → Emissor
2. Tabela com todos os campos ISO 8583 de cada mensagem (MTI, DE3, DE4, DE22, DE25, DE38, DE90 onde aplicável)
3. Análise de risco: em qual ponto do fluxo o portador pode contestar via chargeback e qual é a defesa do merchant?

## Critérios de Avaliação

- [ ] Cenário A implementado com campos corretos (DE25=06, DE25=12)
- [ ] Cenário B com incremental authorization e validação do limite de 15%
- [ ] Cenário C com no-show usando DE25=71 e MIT type 08
- [ ] `IncrementalAuthValidator` com testes para limite dentro e fora do permitido
- [ ] Diagrama de sequência do Cenário B tecnicamente preciso
- [ ] Tratamento correto quando completion > pre-auth original (nova verificação ou rejeição)

## Dicas

- O `DE 90` no completion deve conter: MTI original (0100) + STAN original + DateTime original — é a forma de correlacionar completion com pre-auth.
- Para no-show, o `network transaction ID` da CIT original é obrigatório — sem ele, o emissor pode recusar a MIT como não autorizada pelo portador.
- Partial release (liberar saldo excedente) não é uma mensagem ISO 8583 explícita — o emissor libera automaticamente quando o completion é por valor menor que a pre-auth. Documente isso.
- Visa incrementa por merchant category code: 3351-3441 (locadoras) e 7011-7011 (hotéis) têm regras específicas.
