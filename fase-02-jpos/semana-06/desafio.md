# Semana 6 — Desafio Integrador
## "A Integração Quebrada"

---

## O Cenário

A FinTechBR acabou de fechar uma parceria com a BancoMais, um banco regional que vai enviar autorizações para o seu switch. O time de integração da BancoMais entregou a especificação do packager deles — e ela é diferente do seu.

O gerente de projetos diz:

> "Produção começa na segunda-feira. Temos apenas este fim de semana para fazer a integração funcionar. O BancoMais já testou do lado deles e diz que está tudo certo. Mas os nossos testes estão falhando — as mensagens chegam corrompidas."

Você recebeu a seguinte mensagem em hex de um teste real do BancoMais:

```
Mensagem recebida (hex):
30323030F020000000000000041532015112830366003000000000015000
0314143025000042051000TERM00010MERCHANT000012  986
```

E a spec que eles enviaram (incompleta):

```
MTI:  4 bytes, ASCII numérico
DE1:  Bitmap binário (8 bytes, BCD)
DE2:  LLVAR numérico, length em BCD, dados em BCD
DE3:  6 bytes, BCD
DE4:  12 bytes, ASCII numérico
DE7:  10 bytes, ASCII numérico
DE11: 6 bytes, ASCII numérico
DE22: 3 bytes, ASCII numérico
DE25: 2 bytes, ASCII numérico
DE41: 8 bytes, ASCII (fixo, pad direita com espaços)
DE42: 15 bytes, ASCII (fixo, pad direita com espaços)
DE49: 3 bytes, ASCII numérico
```

O seu packager atual é 100% ASCII para todos os campos.

---

## Sua Missão

### Parte 1 — Análise da Incompatibilidade (25 min)

Crie o documento `analise-incompatibilidade.md` respondendo:

1. **Identifique os campos com formato diferente** entre o packager da BancoMais e o seu:
   - DE1: por que bitmap binário vs ASCII importa?
   - DE2: o que muda quando o length indicator é BCD?
   - DE3: o impacto de BCD numérico vs ASCII numérico

2. **Decodifique manualmente** os primeiros 30 bytes da mensagem hex acima:
   - Bytes 0-3: MTI
   - Bytes 4-11: Bitmap (interprete os bits)
   - A partir do byte 12: decodifique DE2 considerando que o length é BCD

3. **Qual seria o comportamento** se você tentasse fazer unpack da mensagem BancoMais com o seu packager ASCII? O que quebraria primeiro?

### Parte 2 — Packager Customizado (35 min)

Implemente `cfg/bancomais.xml`, o packager que decodifica exatamente o formato da BancoMais.

**Requisitos:**
- Usar as classes `IFB_*` onde o BancoMais usa BCD/binário
- Usar as classes `IFA_*` onde o BancoMais usa ASCII
- Adicionar comentários em cada campo indicando `<!-- BancoMais: BCD -->` ou `<!-- BancoMais: ASCII -->`

### Parte 3 — Adaptador de Mensagem (25 min)

Implemente `MessageAdapter.java` que converte mensagens entre os dois formatos:

```java
public class MessageAdapter {

    /**
     * Recebe bytes no formato BancoMais, retorna ISOMsg no formato interno
     */
    public ISOMsg fromBancoMais(byte[] bancoMaisBytes) throws ISOException { ... }

    /**
     * Recebe ISOMsg interna, serializa no formato BancoMais
     */
    public byte[] toBancoMais(ISOMsg msg) throws ISOException { ... }
}
```

**Implemente também o teste:**

```java
@Test
void roundtripBancoMaisToInterno() throws Exception {
    // 1. Decode mensagem no formato BancoMais
    // 2. Verifica que todos os campos foram decodificados corretamente
    // 3. Re-encode no formato BancoMais
    // 4. Verifica que os bytes são idênticos ao original
}
```

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| Identifica corretamente os campos com formato diferente | /20 |
| Decodificação manual dos primeiros 30 bytes correta | /15 |
| Packager BancoMais funcional (todos os campos corretos) | /30 |
| Adaptador converte sem perda de dados | /25 |
| Teste de roundtrip passando | /10 |

**Meta:** 80+ pontos = Semana 6 dominada.

---

## Dicas

- Em switches de produção, cada parceiro tem seu próprio packager. É comum ter dezenas de arquivos XML diferentes num projeto real.
- O erro mais comum em integrações de packager é misturar `IFA_BITMAP` (hex ASCII) com `IFB_BITMAP` (binário puro). Um bit errado no bitmap torna toda a mensagem ilegível.
- Quando uma integração está quebrando e o parceiro diz "está funcionando do nosso lado", o próximo passo é **comparar byte a byte**. Salve a mensagem recebida em hex, decodifique manualmente, identifique onde a decodificação diverge.
- O BCD (Binary Coded Decimal) empacota dois dígitos em um byte. `15` em ASCII = `0x3135` (2 bytes). `15` em BCD = `0x15` (1 byte). Economiza espaço, mas complica a integração.
