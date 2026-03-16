# Semana 8 — Exercícios de Fixação
## QMUX, Correlação e Late Response

---

## Exercício 1 — Conceitos do QMUX (Nível: Iniciante)

Responda sem consultar a teoria:

1. O que é o QMUX e qual problema ele resolve?
2. Por que uma conexão TCP persistente precisa de correlação? O que aconteceria sem ela?
3. Explique a diferença entre **chave de correlação** e **timeout**. São conceitos independentes?
4. O que é uma **late response**? Por que ela é um problema?
5. Complete a tabela:

| Atributo XML | Para que serve | Exemplo de valor |
|--------------|----------------|------------------|
| `<in>` | | |
| `<out>` | | |
| `<ready>` | | |
| `<key>` | | |
| `timeout` | | |

**Critério de sucesso:** Todas as perguntas respondidas corretamente.

---

## Exercício 2 — Escolha da Chave de Correlação (Nível: Iniciante)

Para cada cenário, indique **qual chave de correlação** você usaria e **por quê**:

**Cenário 1:** Switch conecta a um único emissor. Volume: 500 transações/segundo. Timeout: 30s.
- Opções: `11` (STAN), `11 41` (STAN+Terminal), `37` (RRN)

**Cenário 2:** Switch conecta a Visa. Volume: 10.000 transações/segundo. Múltiplos terminais por merchant.

**Cenário 3:** Switch de banco para banco. Todas as transações têm DE37 único garantido pelo parceiro.

**Cenário 4:** Switch para ATM. Volume baixo (10 transações/min). STAN reinicia a cada sessão.

Para cada cenário:
- Qual chave você escolhe?
- Qual o risco de colisão com essa escolha?
- Há algum trade-off?

**Critério de sucesso:** Justificativas técnicas coerentes para cada cenário.

---

## Exercício 3 — Configuração do QMUX (Nível: Intermediário)

Escreva o arquivo `deploy/20_mux.xml` para os seguintes requisitos:

1. Um QMUX chamado `issuer-mux` que:
   - Recebe mensagens da queue `issuer-receive`
   - Envia mensagens pela queue `issuer-send`
   - Usa chave de correlação STAN + Terminal ID
   - Timeout de 30 segundos

2. Um segundo QMUX chamado `visa-mux` para conexão com a Visa:
   - Timeout de 15 segundos (bandeira exige resposta mais rápida)
   - Chave de correlação: RRN (DE37)

Documente com comentários XML o motivo de cada configuração.

**Critério de sucesso:** XML válido para jPOS, dois QMUX configurados corretamente.

---

## Exercício 4 — Happy Path com QMUX (Nível: Intermediário)

Implemente um teste de integração `QMUXHappyPathTest.java` que:

1. Configura um QMUX em memória (sem conexão TCP real)
2. Sobe um `ISOServer` simulado que responde em 200ms com DE39=`"00"`
3. Faz uma requisição via `mux.request(authRequest, 30000)`
4. Verifica:
   - Response não é null
   - DE39 = `"00"`
   - DE11 da response = DE11 do request (STAN preservado)
   - DE41 da response = DE41 do request (Terminal preservado)
5. Mede o tempo de resposta e verifica que é < 500ms

**Critério de sucesso:** Teste passando em menos de 1 segundo.

---

## Exercício 5 — Teste de Timeout (Nível: Intermediário)

Implemente `QMUXTimeoutTest.java` que:

1. Configura o QMUX com timeout de **2 segundos** (para não demorar o teste)
2. Sobe um servidor simulado que **nunca responde** (thread dorme para sempre)
3. Faz uma requisição via `mux.request(authRequest, 2000)`
4. Verifica que:
   - O retorno é `null`
   - O tempo total foi de aproximadamente 2 segundos (±200ms)
   - Nenhuma exceção não esperada foi lançada

5. Verifica o que acontece se você chamar `mux.request()` **duas vezes com o mesmo STAN** antes de receber a resposta da primeira. O que o QMUX faz?

**Critério de sucesso:** Timeout detectado corretamente, comportamento de STAN duplicado documentado.

---

## Exercício 6 — ForwardToIssuer Participant (Nível: Intermediário)

Implemente `ForwardToIssuer.java` completo:

```java
public class ForwardToIssuer implements TransactionParticipant {

    private MUX mux;          // injetado via configuração Q2
    private long timeout;     // injetado via property

    @Override
    public int prepare(long id, Serializable context) {
        // 1. Lê REQUEST e DESTINATION_MUX do Context
        // 2. Envia via mux.request() com timeout configurado
        // 3. Se response null: coloca TIMEOUT=true e RESPONSE_CODE="68", retorna ABORTED
        // 4. Se exceção: coloca RESPONSE_CODE="96", retorna ABORTED
        // 5. Se OK: coloca RESPONSE no Context, retorna PREPARED
    }

    @Override
    public void abort(long id, Serializable context) {
        // Se TIMEOUT=true no Context: loga alerta de timeout com STAN e terminal
    }
}
```

**Requisitos:**
- Timeout configurável via property `timeout` no XML do participant
- Log diferenciado para timeout vs erro de sistema
- Em caso de timeout, coloca `NEEDS_REVERSAL=true` no Context

Implemente testes para: happy path, timeout, exceção de I/O.

**Critério de sucesso:** Todos os cenários tratados, testes passando.

---

## Exercício 7 — Late Response Handler (Nível: Avançado)

Este é o problema mais difícil da semana. Implemente `LateResponseHandler.java`:

**O cenário:**
1. Switch envia 0200 para emissor (STAN=123456, terminal=TERM0001)
2. QMUX atinge timeout em 30s → retorna null
3. Switch envia 0400 (reversal) automaticamente
4. Emissor responde o 0110 original com DE39=`"00"` (aprovado!) após 32s

**Comportamento esperado do handler:**
```java
public class LateResponseHandler implements ISORequestListener {

    // Chamado quando uma mensagem chega no canal sem correlação pendente
    // (porque o QMUX já fez timeout e descartou a chave)
    @Override
    public boolean process(ISOSource source, ISOMsg lateResponse) {
        // 1. Identifica que é uma late response (correlação não existe mais)
        // 2. Se DE39="00" (aprovado): PERIGO! O cliente foi aprovado mas recebeu negado
        //    → Gerar 0400 imediato para desfazer a aprovação tardia
        //    → Logar como incidente para reconciliação manual
        // 3. Se DE39≠"00": apenas logar
    }
}
```

Implemente o handler e um teste que simula o cenário completo.

**Critério de sucesso:** Handler identifica e trata a aprovação tardia corretamente.

---

## Exercício 8 — Análise de Risco (Nível: Avançado)

Escreva `late-response-analysis.md` analisando o problema de late response:

1. **Mapeie todos os cenários possíveis** quando uma late response chega:
   - Late response negada após timeout: qual o risco?
   - Late response aprovada após timeout (e o switch já enviou reversal): qual o risco?
   - Late response aprovada após timeout (e o switch ainda não enviou reversal): qual o risco?

2. **Por que o reversal automático não é sempre a solução?**
   Considere: e se o reversal também der timeout?

3. **Janela de inconsistência:** Entre o timeout e o processamento do reversal, existe uma janela onde o saldo do cliente pode estar inconsistente. Qual é a duração máxima dessa janela?

4. **Proposta de solução robusta:** Como você desenharia um sistema que zera o risco de inconsistência financeira por late response? (Dica: pense em idempotência, reconciliação, e alertas operacionais)

**Critério de sucesso:** Análise cobre todos os cenários, solução proposta é realista.
