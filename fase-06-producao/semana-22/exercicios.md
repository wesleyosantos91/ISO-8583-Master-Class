# Semana 22 — Exercícios de Fixação

## Exercício 1 — Taxonomia de Falhas: Classificação (Nível: Iniciante)

Para cada sintoma abaixo, classifique a categoria da falha (Rede/TCP, Framing, Encoding, Spec/Packager, Bitmap, Negócio, Roteamento) e indique como investigar:

| Sintoma | Categoria | Como investigar |
|---------|-----------|-----------------|
| DE39=30 (Format Error) em 100% das transações após deploy | | |
| Timeout em transações para emissor específico | | |
| DE39=91 (Issuer Unavailable) em todos os off-us | | |
| Response code inesperado DE39=58 (Transaction Not Permitted) | | |
| Parser lança `ArrayIndexOutOfBoundsException` ao ler DE 55 | | |
| Todos os valores de Amount aparecem com zeros a mais | | |
| Transações Elo sendo roteadas para Mastercard | | |
| 50% das mensagens chegam truncadas ao emissor | | |

**Critério de sucesso:** 8 de 8 classificados corretamente com método de investigação preciso.

---

## Exercício 2 — Playbook de Troubleshooting: Os 4 Passos (Nível: Iniciante/Intermediário)

Para cada cenário, aplique os 4 passos do playbook:

**PASSO 1:** Qual é o sintoma?
**PASSO 2:** Isolar o escopo (todos os terminais? todas as bandeiras? faixa de BIN?)
**PASSO 3:** Analisar a mensagem (hex dump, bitmap, campos)
**PASSO 4:** Reproduzir

**Cenário A:** O emissor BANCO_X retorna DE39=30 para 100% das transações de chip (DE22=051), mas aprova 100% das de tarja (DE22=090).

Aplique os 4 passos e chegue a um diagnóstico. Dica: diferença entre chip e tarja neste caso é o DE 55 (presente no chip, ausente na tarja).

**Cenário B:** Transações de um terminal específico (TERM0099) com valor acima de R$ 200 são sempre negadas com DE39=05. Abaixo de R$ 200 são aprovadas normalmente. O emissor confirma que o portador tem limite disponível.

Aplique os 4 passos e chegue a um diagnóstico.

**Critério de sucesso:** 4 passos aplicados para cada cenário, diagnóstico correto.

---

## Exercício 3 — Resolva o Cenário A: DE39=30 (Nível: Intermediário)

O dump hexadecimal abaixo representa uma mensagem que está gerando DE39=30:

```
0200 B238000108A18000 0019 4532015112830366
0030 000000001500 0031 41600 001234561600000314
```

(Espaços adicionados para legibilidade)

1. Decodifique o MTI: `0200`
2. Decodifique o bitmap: `B238000108A18000` — quais DEs estão presentes?
3. Identifique o problema: existe algum campo com tamanho incorreto, valor fora do esperado ou posição errada?
4. Compare com uma mensagem correta (você define os valores corretos)
5. Qual é a correção?

Documente em `debug-cenario-a.md`.

**Critério de sucesso:** Bitmap decodificado corretamente, problema identificado, correção proposta.

---

## Exercício 4 — Resolva o Cenário B: BIN Roteado Errado (Nível: Intermediário)

**Cenário:** Transações de BIN `6504xxxx` (Elo Itaú) estão sendo roteadas para a Elo (off-us) em vez de para o Itaú (on-us).

Analise e responda:
1. O que seria necessário verificar na tabela de BINs do switch?
2. Como um BIN pode estar mapeado errado? Liste 3 causas possíveis.
3. O BIN `6504xxxx` tem 4 dígitos de prefixo na tabela. Se a Elo também tem BINs começando com `65`, como o switch decide?
4. Escreva o código Java que verifica, para um dado PAN, se deve ser roteado on-us ou off-us:

```java
public class BINRouter {
    private final Map<String, String> binTable; // BIN prefix → route

    public String route(String pan) {
        // Percorrer do BIN mais longo (8 dígitos) ao mais curto (4 dígitos)
        // Primeiro match vence (longest prefix match)
        // TODO
    }
}
```

**Critério de sucesso:** Lógica de longest prefix match implementada, cenário de BIN duplicado tratado.

---

## Exercício 5 — Resolva o Cenário C: Echo OK mas 0200 Timeout (Nível: Intermediário)

**Cenário:** Terminal TERM0099 — echo (0800) funciona, mas 0200 dá timeout.

Liste **5 hipóteses** e para cada uma:
- Qual log/métricas consultaria para confirmar?
- Qual seria o comportamento observado nos logs se essa hipótese fosse correta?

Hipóteses para investigar (você pode propor outras):
1. O canal está logado (sign-on feito) para echo mas não para transações financeiras?
2. O mux tem um bug onde reply key para 0200 não está sendo gerado corretamente?
3. O emissor está processando o 0200 mas a response está sendo roteada para outro terminal?
4. O timeout está configurado muito baixo para 0200 mas adequado para 0800?
5. O switch aceita 0800 de qualquer origem mas só aceita 0200 de terminais cadastrados?

**Critério de sucesso:** 5 hipóteses com método de verificação específico para cada.

---

## Exercício 6 — Resolva o Cenário D: Contactless OK, Chip Negado (Nível: Avançado)

**Cenário:** No mesmo terminal — contactless (DE22=071) aprovado, chip contact (DE22=051) negado com DE39=55 (PIN Incorrect). O portador jura que o PIN está correto.

Analise:
1. DE39=55 significa "PIN errado". Mas o portador nega. Quais outros motivos podem gerar DE39=55?
2. O que é diferente entre chip contact e contactless no fluxo de PIN?
3. **Hipótese de ZPK:** Se o ZPK do adquirente mudou e o terminal não foi atualizado, o que acontece com o PIN block?
4. **Hipótese de DUKPT:** Se o contador DUKPT do terminal chegou no limite (2^21 = 2.097.152), o que acontece?
5. Como você confirmaria cada hipótese sem revelar o PIN do portador?

Implemente `PINDebugger` que analisa um PIN block (sem descriptografar — apenas formato):
```java
public class PINDebugger {
    /**
     * Verifica se o PIN block tem o formato correto (Format 0)
     * sem precisar da chave de descriptografia.
     */
    public PINBlockAnalysis analyzeFormat(byte[] pinBlock, String pan) { /* ... */ }
}
```

**Critério de sucesso:** 5 hipóteses analisadas, `PINDebugger` que verifica formato sem expor o PIN.

---

## Exercício 7 — Catálogo de 15 Falhas (Nível: Avançado)

Crie `fault-catalog.md` com pelo menos 15 falhas comuns em switches de pagamento. Para cada falha:

```markdown
### Falha #N: [Nome]
**Sintoma:** O que se observa (DE39, timeout, exception, etc.)
**Categoria:** Rede/TCP | Framing | Encoding | Spec | Bitmap | Negócio | Roteamento
**Causa raiz:** O que tecnicamente causou
**Como diagnosticar:** Passos específicos com ferramentas
**Correção:** O que mudar no código/configuração
**Prevenção:** Como evitar no futuro
```

Obrigatórias (as 5 do playbook: A, B, C, D, E da teoria). Acrescente mais 10 da sua experiência/pesquisa.

**Critério de sucesso:** 15 falhas documentadas com todos os campos preenchidos.
