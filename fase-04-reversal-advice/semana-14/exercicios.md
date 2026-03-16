# Semana 14 — Exercícios de Fixação

## Exercício 1 — Request vs Advice vs Notification (Nível: Iniciante)

Preencha a tabela abaixo sem consultar a teoria:

| Aspecto | Request (xx0x) | Advice (xx2x) | Notification |
|---------|---------------|---------------|--------------|
| Quem decide? | | | |
| O receptor pode negar? | | | |
| Exemplo de MTI | | | |
| Quando usar? | | | |
| Requer resposta? | | | |

Depois responda:
1. Por que um 0220 (Financial Advice) não pode ser recusado pelo receptor?
2. Em quais situações o adquirente usa 0120 (Auth Advice) em vez de 0100 (Auth Request)?
3. O que significa "offline authorization" e qual MTI é usado para avisar o host?

**Critério de sucesso:** Tabela preenchida corretamente, 3 perguntas respondidas com precisão técnica.

---

## Exercício 2 — Identificar o MTI Correto (Nível: Iniciante)

Para cada situação, indique o MTI correto e justifique:

1. Terminal de metrô: aprova a passagem offline (sem conexão) e depois avisa o host
2. Switch recebeu 0200, teve timeout, quer informar o emissor que reverteu localmente
3. Terminal de pedágio: processa a travessia e envia confirmação em batch
4. Terminal offline de companhia aérea: autoriza check-in de bagagem sem conexão
5. Switch precisa confirmar que capturou uma transação para o emissor
6. Terminal envia segunda tentativa de uma financial request que não recebeu resposta

**Para cada um:** MTI, nome, direção (terminal→host ou host→emissor), pode ser recusado?

**Critério de sucesso:** 6 de 6 corretos, com justificativa coerente.

---

## Exercício 3 — Implemente SafQueue (Nível: Iniciante/Intermediário)

Implemente `SafQueue` em Java com as seguintes operações:

```java
public class SafQueue {
    public void enqueue(ISOMsg advice);
    public void drain(MUX mux);
    public int size();
    public boolean isEmpty();
}
```

Requisitos:
- Persistência em arquivo (use JSON simples ou serialização Java)
- Ao inicializar: carrega itens do arquivo (sobrevive a restart)
- `drain`: processa um item por vez, espera confirmação (0x30 no MTI de resposta) antes de prosseguir
- Descarta itens com mais de 24 horas sem confirmação (chama `alertOperations`)
- Backoff exponencial: 1s, 2s, 4s entre retentativas (máximo 3)

Escreva testes para:
1. Enqueue 3 advices → drain bem-sucedido → fila vazia
2. Drain falha → retry com backoff → sucesso na segunda tentativa
3. Item com 25 horas: deve ser descartado com alerta
4. Simulação de restart: enqueue 2 itens, recria `SafQueue`, verifica que os 2 itens ainda estão na fila

**Critério de sucesso:** Todos os 4 testes passam, persistência funciona.

---

## Exercício 4 — Detecção de Retransmissão (Nível: Intermediário)

O dígito 4 do MTI indica se é uma retransmissão:
- `0` = original
- `1` = retransmissão (repeat)

Exemplo: `0201` é retransmissão de `0200`.

Implemente `RetransmissionHandler` como `TransactionParticipant`:

```java
public class RetransmissionHandler implements TransactionParticipant {
    // Cache: chave = STAN + TerminalID + Amount
    // Valor = ISOMsg de resposta anterior

    @Override
    public int prepare(long id, Serializable context) {
        // Se MTI[3] == '1': busca resposta anterior no cache
        // Se encontrada: retorna ABORTED com a resposta cacheada
        // Se não encontrada: processa normalmente (pode ser retry após cache expirar)
        // Se MTI[3] == '0': processa normalmente
    }
}
```

Regras:
- Cache TTL: 10 minutos
- Chave: STAN + TerminalID + ProcessingCode
- Se retransmissão e cache hit: retorna exatamente a mesma resposta (mesmo DE39, mesmo DE38)
- Se retransmissão e cache miss: processa normalmente (não rejeita — pode ser retry legítimo após restart)

Escreva testes:
1. 0200 original → aprovado → 0201 retransmitido → retorna mesma resposta (DE39=00, DE38=igual)
2. 0200 original → reprovado → 0201 retransmitido → retorna mesma resposta (DE39=05)
3. 0200 original → aprovado → espera TTL expirar → 0201 → processa como nova
4. 0200 com STAN diferente → não é retransmissão → processa normalmente

**Critério de sucesso:** Todos os 4 testes passam.

---

## Exercício 5 — Diagnóstico: Terminal Repetindo Transações (Nível: Intermediário)

**Cenário:** O terminal TERM0088 enviou a mesma transação 47 vezes (0201 repetido). O switch tem o `RetransmissionHandler` implementado. O emissor processou todas as 47 como novas.

Analise e responda:

1. Se o `RetransmissionHandler` está no switch, por que o emissor processou 47 vezes?
2. Liste 3 possíveis causas do bug:
   - Uma no terminal
   - Uma no switch
   - Uma no emissor
3. Qual é a causa mais provável e por quê?
4. Como você replicaria o bug em ambiente de teste para confirmar?
5. Qual é a correção para cada camada?

Escreva `diagnostico-term0088.md` com sua análise.

**Critério de sucesso:** Causa raiz identificada corretamente, correção proposta é técnica e específica.

---

## Exercício 6 — Riscos e Mitigações do SAF (Nível: Intermediário/Avançado)

Para cada risco abaixo, descreva: como o risco se concretiza, qual o impacto financeiro/operacional e como mitigar.

| Risco | Como acontece | Impacto | Mitigação |
|-------|--------------|---------|-----------|
| SAF duplicado após reconexão | | | |
| SAF expirado (> 24h) recebido pelo host | | | |
| Fila SAF crescendo sem drenar (host ainda fora) | | | |
| SAF chega para clearing já fechado | | | |
| SAF de reversal chega depois que o clearing já incluiu a transação | | | |

**Critério de sucesso:** Todos os 5 riscos analisados com impacto e mitigação concretos.

---

## Exercício 7 — Diagrama: SAF Flow Completo (Nível: Avançado)

Crie um diagrama Mermaid (`saf-flow.mermaid`) para o seguinte cenário:

**Contexto:** Loja em shopping com 1 terminal. A conexão com o host cai às 14:30. Durante 20 minutos (14:30–14:50), o terminal processa 4 compras offline. A conexão volta às 14:50.

Diagrama deve mostrar:
- Terminal, Switch, Host (emissor simulado)
- As 4 transações offline acumuladas
- O processo de drenagem da fila SAF ao reconectar
- Respostas de confirmação do host
- O que acontece se a 3ª transação é recusada pelo host ao drenar

**Critério de sucesso:** Diagrama correto, drenagem sequencial representada, cenário de falha durante drenagem incluído.

---

## Exercício 8 — Análise Comparativa: SAF vs Tempo Real (Nível: Avançado)

Uma empresa de estacionamento está decidindo entre:
- **Opção A:** Processamento em tempo real (online) — cada transação enviada imediatamente
- **Opção B:** SAF (Store-and-Forward) — transações acumuladas e enviadas em batch de 5 minutos

Analise para cada opção:
1. Qual é o risco de fraude? (ex: cartão bloqueado durante os 5 minutos do SAF)
2. Qual é o impacto de uma queda de conexão?
3. Qual é a complexidade de implementação?
4. Qual é o impacto na experiência do portador?
5. Qual a recomendação e por quê?

Documente em `saf-vs-realtime-analise.md`.

**Critério de sucesso:** Análise equilibrada, recomendação com justificativa técnica e de negócio.
