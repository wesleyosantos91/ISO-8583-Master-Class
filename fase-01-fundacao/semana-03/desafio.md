# Semana 3 — Desafio Integrador

## O Cenário

Você é o engenheiro de plantão e recebe um alerta às 3h da manhã:

> "Taxa de Format Error (DE39=30) subiu de 0.1% para 12% nos últimos 30 minutos. Afetando apenas transações de um adquirente específico (Terminal IDs começando com 'PX'). Todos os outros adquirentes operando normalmente."

O time de suporte capturou duas mensagens — uma que **funciona** e uma que **falha**:

### Mensagem OK (outro adquirente):
```
MTI: 0200
DE 2  (PAN):          4532015112830366   (LLVAR, prefixo ASCII)
DE 3  (Proc Code):    000000
DE 4  (Amount):       000000015000
DE 11 (STAN):         123456
DE 22 (Entry Mode):   051
DE 41 (Terminal):     TERM0001
DE 42 (Merchant):     MERCHANT0000001
DE 49 (Currency):     986
```

### Mensagem com erro (adquirente PX):
```
Raw hex do campo DE 2 conforme chega no switch:
0x10 0x34 0x35 0x33 0x32 0x30 0x31 0x35 0x31 0x31 0x32 0x38 0x33 0x30 0x33 0x36 0x36
```

## Sua Missão

### Parte 1 — Diagnóstico (30 min)

1. Analise o raw hex do DE 2 da mensagem com erro
2. Identifique a **causa raiz**: por que o prefixo LLVAR está diferente?
3. Explique tecnicamente o que está acontecendo (encoding mismatch)
4. Por que afeta apenas o adquirente PX e não os outros?

### Parte 2 — Implementação do Fix (45 min)

Implemente um mini-parser ISO 8583 **sem usar jPOS** que:

1. Recebe um `byte[]` raw (mensagem completa)
2. Extrai o MTI (4 bytes ASCII)
3. Extrai o bitmap (8 bytes binário) e identifica quais DEs estão presentes
4. Para cada DE presente, extrai o valor baseado em uma spec simplificada:

```java
record FieldSpec(int de, String name, FieldType type, int length, Encoding encoding) {}

enum FieldType { FIXED, LLVAR, LLLVAR }
enum Encoding { ASCII, BCD, BINARY }
```

5. Retorna um `Map<Integer, String>` com os campos decodificados

### Parte 3 — Proposta de Solução (15 min)

Escreva um documento `solucao.md` com:

1. **Root cause**: Descrição técnica precisa do problema
2. **Fix imediato**: O que fazer para resolver agora (< 1 hora)
3. **Fix definitivo**: O que implementar para evitar recorrência
4. **Teste de validação**: Como provar que o fix funciona

## Critério de Avaliação

| Item | Peso |
|------|------|
| Diagnóstico correto da causa raiz | 30% |
| Parser funcional com testes | 40% |
| Proposta de solução clara e implementável | 20% |
| Código limpo e bem testado | 10% |

**Tempo total:** 90 minutos
