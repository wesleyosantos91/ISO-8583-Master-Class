# Semana 10 — Desafio Integrador
## "A Taxa de Aprovação Despencou"

---

## O Cenário

Segunda-feira, 9h05. O gerente de contas da FinTechBR liga com urgência:

> "O BancoNordeste, nosso maior cliente, está ameaçando sair. A taxa de aprovação deles caiu de 89% para 61% durante o fim de semana. Perdemos R$ 4,2 milhões em transações negadas que deveriam ser aprovadas. O CTO do BancoNordeste quer uma explicação em 2 horas."

Você puxa as métricas do fim de semana:

```
Sexta-feira 23:59:
  Total: 82.450 transações
  Aprovadas: 73.380 (89.0%)
  Top declines: 51 (4.2%), 05 (3.1%), 54 (2.8%), outros

Sábado 00:15 (15 minutos depois do deploy de emergência):
  Total: 8.924 transações
  Aprovadas: 5.439 (61.0%)
  Top declines: 05 (28.4%), 51 (4.1%), 54 (2.9%)

Domingo 23:59:
  Total: 79.211 transações
  Aprovadas: 48.318 (61.0%)
  Top declines: 05 (28.3%), 51 (4.1%), 54 (2.8%)
```

Você nota: o DE39=`05` (Do not honor) subiu de quase zero para **28%** exatamente às 00:15 de sábado — no mesmo momento do deploy de emergência.

O deploy de sábado foi um "hotfix de roteamento" feito pelo engenheiro de plantão para corrigir um problema de latência. O código que mudou:

```java
// ANTES (código original):
public class RouteByBIN implements TransactionParticipant {
    public int prepare(long id, Serializable context) {
        // ...
        String bin = pan.substring(0, 8); // 8-digit BIN
        Route route = binTable.lookup(bin);
        ctx.put("IS_ON_US", route.isOnUs());
        ctx.put("DESTINATION_MUX", route.getMuxName());
        return PREPARED;
    }
}

// DEPOIS (hotfix de sábado):
public class RouteByBIN implements TransactionParticipant {
    public int prepare(long id, Serializable context) {
        // ...
        String bin = pan.substring(0, 8); // 8-digit BIN
        Route route = binTable.lookup(bin);
        ctx.put("IS_ON_US", false); // ← linha "temporária" adicionada para debug
        ctx.put("DESTINATION_MUX", route.getMuxName());
        return PREPARED;
    }
}
```

---

## Sua Missão

### Parte 1 — Diagnóstico (20 min)

Crie `diagnostico-aprovacao.md`:

1. **Identifique o bug:** O que exatamente a linha `ctx.put("IS_ON_US", false)` causou?

2. **Por que gerou DE39=05?**
   - Trace o caminho de uma transação on-us com `IS_ON_US=false`.
   - O que o participant `ForwardToIssuer` faz diferente quando `IS_ON_US=false`?
   - Por que o emissor devolveu `05` em vez de `00`?

3. **Calcule o impacto financeiro do fim de semana:**
   - Quantas transações afetadas? (total aprovadas previstas × 89% − aprovadas reais)
   - Ticket médio: R$ 112
   - Valor total em transações negadas indevidamente?
   - Se o MDR médio é 2.2%, quanto de receita a FinTechBR perdeu?

4. **Por que o DE39=51 e DE39=54 não mudaram?** Isso é esperado?

### Parte 2 — Correção e Prevenção (40 min)

**Parte 2a: Corrija o bug imediatamente**

Reimplemente `RouteByBIN.java` com a lógica correta e adicione:

1. Um **teste de regressão** que teria capturado este bug antes do deploy:
   ```java
   @Test
   void onUsFlagMustReflectRouteTable() {
       // Se a tabela de BINs diz on-us, IS_ON_US deve ser true
       // NUNCA hardcoded como false
   }
   ```

2. Uma **asserção defensiva** no próprio código:
   ```java
   // Se route.isOnUs() retorna true mas IS_ON_US foi sobreescrito para false,
   // logar um WARN crítico e usar o valor da route table
   ```

**Parte 2b: IssuerSimulator com comportamento on-us vs off-us**

Atualize o `IssuerSimulator` para simular o comportamento real:

- Quando recebe uma mensagem **diretamente** (on-us): processa com regras normais
- Quando recebe via **bandeira** (off-us → Mastercard → emissor): o DE3 pode ser diferente, e o emissor pode ser mais restritivo

Demonstre com um teste que a mesma transação on-us aprova (DE39=`00`) mas off-us com IS_ON_US=false vai para o lugar errado.

**Parte 2c: Checklist de deploy**

Escreva `checklist-deploy.md` com mínimo 10 verificações obrigatórias antes de qualquer deploy no switch de produção.

### Parte 3 — Comunicação com o Cliente (20 min)

Escreva dois documentos:

1. **`comunicado-bancnordeste.md`** (para o CTO do BancoNordeste):
   - O que aconteceu (em linguagem não técnica)
   - Por quanto tempo durou
   - Impacto estimado para o BancoNordeste
   - O que foi feito para corrigir
   - O que foi feito para prevenir recorrência
   - Proposta de compensação (ex: desconto em taxas do próximo mês)

2. **`retrospectiva-interna.md`** (para o time de engenharia):
   - Root cause real
   - Por que o código de debug foi para produção?
   - Quais controles falharam?
   - Ações de melhoria de processo (não de punição)

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| Diagnóstico correto: identifica a linha exata e o efeito cascata | /20 |
| Cálculo de impacto financeiro correto | /15 |
| RouteByBIN corrigido com teste de regressão | /25 |
| Comunicado ao BancoNordeste: claro, honesto, profissional | /20 |
| Retrospectiva interna: foca em processo, não em culpa | /20 |

**Meta:** 80+ pontos = Semana 10 dominada.

---

## Dicas

- O `IS_ON_US` sendo sempre `false` significa que todas as transações on-us foram tratadas como off-us e roteadas para a bandeira. A bandeira, ao receber uma transação que deveria ser processada diretamente pelo emissor, pode negar com `05` por regras de roteamento incorreto.
- Código de debug em produção é um dos erros mais comuns em sistemas de pagamento. A prevenção é processo: feature flags, não hardcode; teste obrigatório antes de merge; review de código mesmo em hotfixes de madrugada.
- Ao comunicar um incidente para um cliente corporativo, **nunca minimize e nunca culpe infraestrutura**. Seja específico sobre o que aconteceu e concreto sobre a prevenção.
- 28% de declines é um sinal de alarme que deveria ter disparado um alerta automático em < 5 minutos. Se não disparou, o sistema de monitoramento também precisa de correção.
