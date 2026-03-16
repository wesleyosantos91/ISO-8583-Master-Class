# Semana 2 — Desafio Integrador

## O Cenário

Você recebeu um dump hexadecimal de uma mensagem ISO 8583 capturada na rede. O time de suporte não consegue identificar o que está errado — a transação foi negada com "Format Error" (DE39=30) e ninguém sabe por quê.

```
Dump hex (mensagem completa, incluindo header de 4 bytes):
00 E7 30 32 30 30 F2 3C 46 D1 28 E0 90 00 00 00
00 00 00 00 01 00 31 36 34 35 33 32 30 31 35 31
31 32 38 33 30 33 36 36 30 30 30 30 30 30 30 30
30 30 31 35 30 30 30 30 33 30 31 30 39 31 35 30
30 32 35 31 32 33 34 35 36 30 35 31 30 31 32 30
31 30 30 31 31 30 31 36 33 38 33 37 34 35 33 32
30 31 35 31 31 32 38 33 30 33 36 36 3D 32 36 30
31 31 30 31 30 30 31 30 30 31 30 39 38 33 32 30
30 30 30 30 35 30 30 30 34 30 30 30 30 30 30 30
30 30 30 30 39 38 36
```

## Sua Missão

### Parte 1 — Decodificação Manual (45 min)

Decodifique a mensagem **à mão**, sem usar jPOS nem qualquer parser:

1. **Extraia o MTI** (4 bytes ASCII). Qual é?
2. **Extraia o bitmap** (8 bytes binário). Converta para binário e liste todos os DEs presentes.
3. **Para cada DE presente**, decodifique o valor:
   - Consulte a spec padrão (teoria da Semana 2) para saber tipo e tamanho de cada campo
   - Atenção: DE 2 e DE 35 são LLVAR — o prefixo de comprimento está em ASCII
4. **Monte uma tabela** com todos os campos decodificados:

| DE | Nome | Valor | Observação |
|----|------|-------|------------|
| 0  | MTI  |       |            |
| 2  | PAN  |       |            |
| 3  | Proc Code | | |
| ... | ... | ... | ... |

### Parte 2 — Identifique o Erro (15 min)

Algum campo parece inconsistente? Algum dado não faz sentido? Identifique o provável motivo do Format Error.

Dicas:
- Verifique se o bitmap indica campos que realmente estão presentes
- Verifique se os comprimentos LLVAR/LLLVAR batem com o conteúdo
- Verifique se campos numéricos contêm apenas dígitos

### Parte 3 — Corrija e Reenvie (30 min)

1. Corrija o erro encontrado
2. Reconstrua a mensagem corrigida em hex
3. Prove com código Java que a mensagem corrigida faz unpack corretamente

## Critério de Avaliação

| Item | Peso |
|------|------|
| MTI e bitmap decodificados corretamente | 25% |
| Todos os DEs decodificados corretamente | 25% |
| Erro identificado com justificativa técnica | 25% |
| Mensagem corrigida e validada com código | 25% |

**Tempo total:** 90 minutos
