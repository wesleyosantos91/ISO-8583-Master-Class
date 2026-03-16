# Semana 11 — Exercícios de Fixação
## On-Us/Off-Us + Roteamento + Parcelamento

---

## Exercício 1 — BINs, On-Us e Off-Us (Nível: Iniciante)

O switch pertence ao **BancoMix** (processadora própria). Use a tabela de BINs abaixo:

| Prefixo BIN | Emissor | Bandeira | On-Us? |
|-------------|---------|----------|--------|
| `45320151` | BancoMix | Visa | Sim |
| `54127890` | Bradesco | Mastercard | Não |
| `63621234` | Caixa | Elo | Não |
| `51231111` | Nubank | Mastercard | Não |
| `43895555` | BancoMix | Visa | Sim |
| `65049999` | Itaú | Elo | Não |
| `45320152` | BancoMix | Visa | Sim |
| `60041234` | BancoMix | Elo | Sim |

Para cada PAN abaixo, defina:
- Bandeira
- Emissor
- On-Us ou Off-Us?
- Para qual QMUX/destino seria roteado?

PANs para classificar:
1. `4532015112830366`
2. `5412789012345678`
3. `6362123456789012`
4. `4389555544443333`
5. `6504999988887777`
6. `5123111122223333`
7. `4532015299998888`
8. `6004123411112222`

**Critério de sucesso:** Todos os 8 PANs classificados corretamente.

---

## Exercício 2 — BINTable com Longest Prefix Match (Nível: Iniciante)

Explique o conceito de **Longest Prefix Match** com um exemplo:

Dada a tabela com entradas:
- `453201` → Visa genérico
- `45320151` → BancoMix Visa (on-us)
- `45320152` → BancoMix Visa Premium (on-us)

Para cada PAN, qual rota é selecionada e por quê?
1. `4532015112830366` → rota: `453201` ou `45320151`?
2. `4532015299998888` → rota: `453201` ou `45320152`?
3. `4532019999999999` → rota: `453201` ou nenhuma das específicas?

**Por que o longest prefix match é necessário?** O que aconteceria se você usasse sempre o match mais curto?

**Critério de sucesso:** Os 3 PANs com rota correta e justificativa clara.

---

## Exercício 3 — Implementando BINTable (Nível: Intermediário)

Implemente `BINTable.java` completo:

```java
public class BINTable {

    private final TreeMap<String, Route> routes = new TreeMap<>();

    // Adiciona uma rota. Prefixo pode ter 6 ou 8 dígitos.
    public void addRoute(String binPrefix, Route route) { ... }

    // Busca por longest prefix match. Retorna null se não encontrar.
    public Route lookup(String pan) { ... }

    // Carrega rotas de um arquivo de configuração JSON ou properties
    public void loadFromFile(String filePath) throws IOException { ... }

    // Retorna estatísticas: quantas rotas, quantas on-us, quantas por bandeira
    public BINTableStats getStats() { ... }
}

public record Route(
    String binPrefix,
    String issuerName,
    String network,          // VISA, MASTERCARD, ELO
    boolean onUs,
    String muxName,
    String fallbackMux       // null se não houver fallback
) {}
```

**Popule a tabela com pelo menos 20 BINs:** 5 on-us, 15 off-us, cobrindo 3 bandeiras.

Implemente testes para:
- Lookup exato (BIN de 8 dígitos)
- Lookup por fallback (8 dígitos não encontrado → tenta 6)
- Lookup não encontrado → retorna null
- BIN prefix de 6 e 8 para o mesmo BIN — o de 8 deve vencer

**Critério de sucesso:** Todos os testes passando, 20 BINs cadastrados.

---

## Exercício 4 — RouteByBIN Participant Completo (Nível: Intermediário)

Implemente `RouteByBIN.java` completo com base na teoria:

```java
public class RouteByBIN implements TransactionParticipant {

    private final BINTable binTable;

    @Override
    public int prepare(long id, Serializable context) {
        // 1. Extrai PAN do REQUEST
        // 2. Extrai BIN (8 dígitos)
        // 3. Lookup na BINTable (com fallback 8→6)
        // 4. Se não encontrado: RESPONSE_CODE="92", ABORTED
        // 5. Se encontrado: popula Context com ROUTE, IS_ON_US, DESTINATION_MUX, NETWORK
        // 6. Loga decisão de roteamento (sem PAN completo!)
    }
}
```

**Requisitos adicionais:**
- Log de roteamento: `[ROUTE] pan=4532****0366 bin=45320151 → on-us/issuer-local`
- Nunca logar o PAN completo
- Se o MUX destino não estiver disponível (`mux.isConnected() == false`): tenta o `fallbackMux`
- Se fallback também indisponível: RESPONSE_CODE=`"91"` (Issuer unavailable)

Implemente testes para: on-us, off-us Visa, off-us Mastercard, BIN não encontrado, MUX indisponível com fallback.

**Critério de sucesso:** Todos os cenários de roteamento testados e funcionando.

---

## Exercício 5 — Parcelamento no ISO 8583 (Nível: Intermediário)

Implemente `InstallmentData.java` e a integração com ISOMsg:

```java
public class InstallmentData {

    private final int numberOfInstallments;  // 2-99
    private final InstallmentType type;       // LOJISTA, EMISSOR

    public enum InstallmentType { LOJISTA, EMISSOR }

    // Serializa para string no formato do campo privado DE48
    // Formato: "03" + tipo ("1"=lojista, "2"=emissor) + quantidade (2 dígitos)
    // Ex: 3x lojista → "031103", 12x emissor → "032212"
    public String toDE48() { ... }

    // Deserializa de uma string DE48
    public static InstallmentData fromDE48(String de48Value) { ... }

    // Calcula o valor de cada parcela (sem juros para lojista)
    public BigDecimal calculateInstallmentAmount(BigDecimal totalAmount) { ... }
}
```

**Implemente testes para:**
- Serialização: 3x lojista, 6x emissor, 12x lojista
- Deserialização: string → objeto → campos corretos
- Roundtrip: objeto → string → objeto → igual ao original
- Valor de parcela: R$ 300 em 3x = R$ 100,00 (sem centavos perdidos)
- Arredondamento: R$ 100 em 3x = duas parcelas de R$ 33,34 e uma de R$ 33,32

**Critério de sucesso:** Serialização/deserialização correta, arredondamento sem perda de centavos.

---

## Exercício 6 — BIN Expansion: 6 para 8 Dígitos (Nível: Avançado)

Simule o cenário de BIN expansion descrito na teoria:

**Situação:** O cartão `45320151 1283 0366` era roteado antes pelo BIN de 6 dígitos `453201` para a rota "Visa genérico (off-us)". Agora o BIN de 8 dígitos `45320151` deve rotear para "BancoMix on-us".

**Implemente:**

1. `BINTableV1.java` — apenas com BIN de 6 dígitos `453201` → off-us
2. `BINTableV2.java` — com BIN de 6 dígitos `453201` e BIN de 8 dígitos `45320151` → on-us

**Escreva o teste:**

```java
@Test
void binExpansionRoutesCorrectly() {
    String pan = "4532015112830366";

    // Com tabela antiga (6 dígitos): deve rotear como OFF-US
    Route oldRoute = tableV1.lookup(pan);
    assertFalse(oldRoute.isOnUs());

    // Com tabela nova (8 dígitos): deve rotear como ON-US
    Route newRoute = tableV2.lookup(pan);
    assertTrue(newRoute.isOnUs());
}
```

**Documente:** Qual é o impacto financeiro de rotear on-us como off-us? (Use interchange de 1.5% e 50.000 transações/dia de R$ 150 em média.)

**Critério de sucesso:** Teste passando, impacto financeiro calculado.

---

## Exercício 7 — Pipeline Completo com Roteamento (Nível: Avançado)

Integre o `RouteByBIN` no pipeline completo do switch e implemente um teste end-to-end com dois "emissores simulados":

- `IssuerSimulatorOnUs` — representa o próprio banco (on-us), sempre aprova PANs on-us
- `IssuerSimulatorOffUs` — representa o gateway externo (bandeira), aprova PANs off-us com regras diferentes

O teste deve verificar que:
1. Transação com PAN on-us vai para `IssuerSimulatorOnUs`
2. Transação com PAN off-us vai para `IssuerSimulatorOffUs`
3. Transação com BIN desconhecido recebe DE39=`92`
4. Transação com MUX destino offline recebe DE39=`91`

**Critério de sucesso:** 4 cenários testados end-to-end sem mocks de roteamento.
