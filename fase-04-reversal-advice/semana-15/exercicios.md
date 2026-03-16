# Semana 15 — Exercícios de Fixação

## Exercício 1 — Mapeamento de Network Management Codes (Nível: Iniciante)

Preencha a tabela sem consultar a teoria:

| DE 70 | Nome | Quando usar | Quem inicia? | Resposta esperada |
|-------|------|-------------|--------------|-------------------|
| 001 | | | | |
| 002 | | | | |
| 101 | | | | |
| 102 | | | | |
| 201 | | | | |
| 301 | | | | |

Depois responda:
1. O que acontece se um terminal tenta enviar um 0200 antes de fazer sign-on (DE70=001)?
2. Por que o echo test (DE70=301) usa MTI 0800 e não 0200?
3. Qual é a diferença entre sign-off (002) e simplesmente fechar a conexão TCP?

**Critério de sucesso:** Tabela preenchida corretamente, 3 perguntas respondidas.

---

## Exercício 2 — Implemente Sign-On Obrigatório (Nível: Iniciante/Intermediário)

Implemente um `SessionManager` que:

```java
public class SessionManager {
    private volatile boolean signedOn = false;
    private Instant lastSignOnTime;

    public boolean performSignOn(QMUX mux) throws ISOException { /* ... */ }
    public boolean isSignedOn() { /* ... */ }
    public void blockIfNotSignedOn() throws SessionException { /* ... */ }
}
```

Regras:
- `performSignOn`: envia 0800/DE70=001, aguarda 0810/DE39=00; retorna true se bem-sucedido
- `blockIfNotSignedOn`: lança `SessionException` se `!signedOn`
- Todo `TransactionParticipant` deve chamar `blockIfNotSignedOn` no início do `prepare`
- Se o sign-on falhar: tenta novamente até 3 vezes com intervalo de 5s
- Após reconexão do canal: reexecuta sign-on automaticamente

Escreva testes:
1. Sign-on bem-sucedido → `isSignedOn()` = true → 0200 processado normalmente
2. Sign-on não feito → 0200 lança `SessionException`
3. Sign-on falha 2x, sucesso na 3ª → `isSignedOn()` = true
4. Sign-on falha 3x → `isSignedOn()` = false, alerta gerado

**Critério de sucesso:** Todos os 4 testes passam.

---

## Exercício 3 — Implemente Echo Test Periódico (Nível: Intermediário)

Implemente `ChannelHealthCheck` usando `ScheduledExecutorService`:

```java
public class ChannelHealthCheck {
    enum ChannelStatus { UP, DOWN, DEGRADED }

    public ChannelStatus getStatus() { /* ... */ }
    public long getLastLatencyMs() { /* ... */ }
    public Instant getLastEchoTime() { /* ... */ }
    public void start();
    public void stop();
}
```

Regras:
- Echo a cada 30 segundos
- Latência > 500ms: status = DEGRADED (mas ainda UP)
- Sem resposta em 10s: status = DOWN
- 3 echos consecutivos bem-sucedidos após DOWN: status = UP e trigger sign-on
- Ao voltar para UP: chamar `SessionManager.performSignOn()`

Escreva testes:
1. Canal saudável → status UP, latência registrada
2. Canal responde lento (700ms) → status DEGRADED
3. Canal não responde → status DOWN após timeout
4. Canal volta após DOWN → sign-on reexecutado, status UP

**Critério de sucesso:** Todos os 4 testes passam, latência medida corretamente.

---

## Exercício 4 — Implemente Cutover (DE70=201) (Nível: Intermediário)

O cutover (virada de dia) é um processo de sincronização entre o switch e o host ao final de cada dia. Implemente:

```java
public class CutoverManager {
    public CutoverResult performCutover(QMUX mux) throws ISOException { /* ... */ }
}

public record CutoverResult(
    boolean success,
    Instant cutoverTime,
    String newBusinessDay,
    String previousBusinessDay
) {}
```

Regras:
- Enviar 0800/DE70=201 com a data atual no DE 7
- Aguardar 0810/DE39=00 com a nova data de negócio
- Se aprovado: atualizar a data de negócio interna do switch
- Todas as transações após o cutover usam a nova data de negócio
- Se cutover falhar: não mudar a data, alertar, tentar novamente em 5 minutos

Escreva um teste de integração que:
1. Executa cutover com data `20260315` → sucesso → nova data `20260316`
2. Verifica que um 0200 enviado após o cutover usa a data `20260316`
3. Simula falha no cutover → data permanece `20260315`

**Critério de sucesso:** Cutover funcional, data atualizada corretamente, falha tratada.

---

## Exercício 5 — Bloqueio de Transações por Canal Down (Nível: Intermediário)

Implemente `ChannelGuard` como `TransactionParticipant`:

```java
public class ChannelGuard implements TransactionParticipant {
    // Usa ChannelHealthCheck para verificar status antes de processar
    // Se DOWN: retorna DE39=91 (Issuer Unavailable) imediatamente
    // Se DEGRADED: permite passar mas loga warning
    // Se UP: permite passar normalmente
}
```

Implemente também um `CircuitBreaker` integrado ao `ChannelGuard`:
- Após 10 falhas consecutivas em 60 segundos: abre o circuito (rejeita todas por 30s)
- Após 30s: tenta 1 transação de probe
- Se probe bem-sucedida: fecha o circuito (volta ao normal)

Escreva testes:
1. Canal DOWN → 0200 retorna DE39=91 imediatamente (sem esperar timeout)
2. Canal UP → 0200 processado normalmente
3. 10 falhas consecutivas → circuito abre → próxima transação rejeitada sem tentar
4. Após 30s com circuito aberto → probe enviada → sucesso → circuito fecha

**Critério de sucesso:** Todos os 4 testes passam, circuito breaker funcional.

---

## Exercício 6 — Dashboard de Status de Canais (Nível: Avançado)

Implemente um `ChannelDashboard` que gera um relatório estruturado a cada 60 segundos:

```java
public class ChannelDashboard {
    public ChannelReport generateReport() { /* ... */ }
    public void printToLog() { /* ... */ }
}

public record ChannelReport(
    Map<String, ChannelStatus> channelStatuses,
    Map<String, Long> lastLatencyByChannel,
    Map<String, Instant> lastSignOnByChannel,
    Map<String, Long> blockedTransactionsByChannel,
    Instant reportTime
) {}
```

O log deve ter o seguinte formato:
```
=== CHANNEL STATUS REPORT 2026-03-15T14:30:00Z ===
VISA-PRIMARY    : UP       | Latency: 45ms  | Last Sign-On: 14:25:00 | Blocked: 0
VISA-SECONDARY  : DEGRADED | Latency: 620ms | Last Sign-On: 14:25:00 | Blocked: 0
MASTERCARD      : DOWN     | Latency: N/A   | Last Sign-On: 13:50:00 | Blocked: 47
ELO             : UP       | Latency: 38ms  | Last Sign-On: 14:25:00 | Blocked: 0
```

**Critério de sucesso:** Relatório gerado a cada 60s, formato correto, contagem de transações bloqueadas correta.

---

## Exercício 7 — Análise: O que Acontece Sem Sign-On? (Nível: Avançado)

**Cenário:** O sign-on não foi implementado. O switch reinicia às 03h00. Às 03h01, um terminal envia 0200. O host (emissor) ainda não sabe que houve restart — do ponto de vista do host, a sessão estava ativa.

Analise e responda:
1. O host pode processar a transação normalmente sem sign-on explícito?
2. Quais são os riscos se o switch processar transações sem ter feito sign-on?
3. Por que o sign-on é especialmente importante após troca de chaves (ZPK)?
4. Se a troca de chave ZPK aconteceu durante o downtime do switch, e o switch não fez sign-on/key-change ao voltar, o que acontece com os PIN blocks?

Documente em `analise-signoff-risks.md`.

**Critério de sucesso:** Análise técnica correta, risco criptográfico (ZPK) identificado e explicado.
