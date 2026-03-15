# Semana 2 — Exercícios de Fixação

## Exercício 1 — Decompor MTIs (Nível: Iniciante)

Decomponha cada MTI em versão, classe, função e origem:

| MTI | Versão | Classe | Função | Origem | Significado |
|-----|--------|--------|--------|--------|-------------|
| 0100 | | | | | |
| 0110 | | | | | |
| 0200 | | | | | |
| 0210 | | | | | |
| 0220 | | | | | |
| 0400 | | | | | |
| 0410 | | | | | |
| 0420 | | | | | |
| 0800 | | | | | |
| 0810 | | | | | |
| 0201 | | | | | |
| 0120 | | | | | |
| 0500 | | | | | |

**Atenção especial:** O que muda entre 0200 e 0201? Qual a implicação prática?

---

## Exercício 2 — Construir MTI a partir de requisitos (Nível: Intermediário)

Monte o MTI correto para cada cenário:

1. Emissor respondendo a um pedido de autorização (versão 1987)
2. Adquirente retransmitindo uma transação financeira (versão 1987)
3. Notificação de que uma transação offline foi processada (advice do adquirente)
4. Pedido de echo test para verificar se o link está vivo
5. Adquirente pedindo cancelamento de transação anterior
6. Emissor confirmando que recebeu o aviso de cancelamento
7. Pedido de fechamento de lote
8. Resposta do emissor ao fechamento de lote

---

## Exercício 3 — Bitmap Manual (Nível: Intermediário)

### 3a. Ler bitmap hex → listar DEs

Dado o bitmap: `B238000108A180000000000004000000`

1. Converta para binário (bit a bit)
2. Liste todos os DEs presentes
3. A secondary bitmap está presente? Como sabe?
4. Quantos bytes o bitmap ocupa?

### 3b. Montar bitmap a partir de DEs

Você precisa enviar uma mensagem com estes campos: DE 2, 3, 4, 7, 11, 12, 13, 22, 25, 37, 38, 39, 41, 42, 49

1. Monte a primary bitmap em binário
2. A secondary bitmap é necessária? Por quê?
3. Converta para hexadecimal
4. Quantos bytes o bitmap ocupa?

### 3c. Bitmap com secondary

Agora adicione DE 55 e DE 70 à lista anterior.

1. O que muda na primary bitmap?
2. Monte a secondary bitmap
3. Qual o bitmap final em hex?

---

## Exercício 4 — Serializar Mensagem Manual (Nível: Avançado)

Monte a mensagem serializada completa (em texto) para este cenário:

**Cenário:** Echo test do adquirente

| Campo | Valor |
|-------|-------|
| MTI | (você define) |
| DE 7 | 0314150000 |
| DE 11 | 000001 |
| DE 70 | 301 |

Escreva a mensagem byte a byte:
1. MTI
2. Bitmap (em hex)
3. Cada DE na ordem correta
4. Mensagem completa concatenada

---

## Exercício 5 — Desserializar Mensagem (Nível: Avançado)

Dada a mensagem raw abaixo, faça o parse manual:

```
0200723805412880080016453201511283036600300000000001500003141430251234561430250314051004532010000TERM0001MERCHANT00001  986045[EMV_PLACEHOLDER]
```

Identifique:
1. MTI
2. Bitmap (hex)
3. Quais DEs estão presentes (decomponha o bitmap)
4. Valor de cada DE (respeitando tamanho fixo/variável)
5. O que cada campo significa em linguagem de negócio

---

## Exercício 6 — Implementar Parser de Bitmap em Java (Nível: Avançado)

Implemente a classe `BitmapParser.java` que:

```java
public class BitmapParser {
    // Recebe bitmap hex string, retorna lista de DEs presentes
    public static List<Integer> parse(String hexBitmap) { }

    // Recebe lista de DEs, retorna bitmap hex string
    public static String build(List<Integer> dataElements) { }

    // Recebe bitmap hex, retorna representação binária legível
    public static String toBinaryString(String hexBitmap) { }
}
```

Escreva testes JUnit 5 + AssertJ para:
- Parse do bitmap `7234054128C08001` retorna [2,3,4,7,11,12,13,22,25,41,42,49,55,64]
- Build de [2,3,4,39,41] retorna bitmap hex correto
- Secondary bitmap é adicionada automaticamente se qualquer DE > 64

---

# Semana 2 — Desafio Integrador

## Cenário: Debug de mensagem corrompida

Você recebe um ticket da operação:

> "O terminal TERM0042 está enviando transações mas todas voltam com DE39=30 (Format Error). O terminal funcionava normalmente até ontem."

Você recebe o dump hex da mensagem que o terminal está enviando:

```
0200F23804012880080019453201511283036651003000000000015000031415302512345615302503140510045320100001TERM0042MERCHANT00042  986
```

E o dump de uma mensagem que funcionava antes (de outro terminal):

```
0200F2380401288008001645320151128303660030000000000150000314153025123456153025031405100TERM0001MERCHANT00001  986
```

**Sua missão:**

1. Faça o parse manual de ambas as mensagens
2. Compare campo a campo
3. Identifique a diferença que causa o Format Error
4. Explique por que essa diferença gera DE39=30
5. Proponha a correção
6. Escreva um teste automatizado que detectaria esse problema antes de ir pra produção

**Dica:** O erro está na forma como um campo variável está sendo serializado. Preste atenção ao comprimento.

**Tempo sugerido:** 45 minutos (parse manual: 20 min, diagnóstico: 15 min, correção + teste: 10 min)
