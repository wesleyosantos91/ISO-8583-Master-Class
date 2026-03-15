# Desafio — Semana 35 — Da Expertise à Contribuição Pública

## Contexto

Você passou 35 semanas construindo expertise técnica em ISO 8583 e sistemas de pagamento.
Agora chegou o momento de transformar esse conhecimento em contribuição pública que
constrói sua reputação no mercado.

Este desafio é diferente dos anteriores: não há um incidente em produção para resolver.
O desafio é você mesmo — criar artefatos públicos que demonstrem sua competência
de forma que o mercado possa descobrir e validar.

## O Problema

Atualmente, há pouquíssimo conteúdo técnico em português sobre ISO 8583, jPOS e o
ecossistema de pagamentos brasileiro. Desenvolvedores que entram na área precisam ler
documentação em inglês, specs proprietárias de difícil acesso e aprender por tentativa
e erro. Você pode mudar isso.

## Missão

### Parte 1 — Contribuição para o jPOS

Identifique e documente uma contribuição concreta que você faria ao jPOS.
Você tem três opções — escolha uma e execute:

**Opção A — Issue de bug bem documentada:**
Encontre ou simule um comportamento inesperado no jPOS que você já encontrou
durante o curso. Escreva a issue como se fosse abri-la no GitHub:

```markdown
## Título: [Descreva o problema em uma linha]

**Versão do jPOS:** x.x.x
**Java:** xx

## Comportamento esperado
[O que deveria acontecer]

## Comportamento atual
[O que está acontecendo de fato]

## Como reproduzir
[Passos mínimos para reproduzir o problema]

## Código de reprodução
```java
// Snippet mínimo que demonstra o problema
```

## Logs relevantes
[Stack trace ou log relevante]
```

**Opção B — Melhoria de documentação:**
Identifique uma seção da documentação do jPOS (README, wiki, Javadoc) que está
desatualizada, incompleta ou inexistente. Escreva o texto da melhoria como se
fosse um PR description + o novo conteúdo proposto.

**Opção C — Teste unitário para código não coberto:**
Escreva um teste unitário para a classe `VelocityEngine` implementada durante o curso,
cobrindo pelo menos 3 cenários de borda:

```java
@ExtendWith(MockitoExtension.class)
class VelocityEngineTest {

    @Mock
    private RedisTemplate<String, String> redis;

    @InjectMocks
    private VelocityEngine velocityEngine;

    @Test
    void deveDetectarVelocidadeExcessivaDoMesmoCartao() {
        // TODO: simular 6 transações do mesmo PAN em 10 minutos
        // Verificar que VelocityViolation "CARD_VELOCITY_10MIN" é gerada
    }

    @Test
    void devePermanecerDentroDaJanelaSeContadorEstaNolimite() {
        // TODO: simular exatamente 5 transações (no limite, não deve violar)
    }

    @Test
    void deveIgnorarIpNuloSemLancarExcecao() {
        // TODO: chamar check() com ip=null
        // Verificar que não lança NullPointerException
    }
}
```

### Parte 2 — Post Técnico Publicável

Escreva um post técnico completo e publicável (mínimo 600 palavras) sobre um dos
temas de alto impacto da teoria. O post deve seguir a estrutura da teoria:

- Título forte com promessa específica
- Hook: situação familiar para developers de pagamentos
- Seção "O problema" (conceito explicado de forma simples)
- Seção "Como funciona por dentro" (protocolo, campos, fluxo — com código)
- Seção "O caso brasileiro" (o que é específico do Brasil)
- Seção "Na prática" (exemplo concreto com código Java/jPOS)
- Resumo com 3-5 bullets
- Call to action

O post deve ser escrito em português, para desenvolvedores brasileiros de nível
pleno a sênior que ainda não dominam o ecossistema de pagamentos.

### Parte 3 — Plano de Networking dos Próximos 30 Dias

Crie um plano concreto de networking para os próximos 30 dias:

1. **5 pessoas específicas para conectar:** Pesquise e liste 5 profissionais reais
   (LinkedIn público) que trabalham com pagamentos no Brasil em empresas como Cielo,
   Stone, Nubank, PagSeguro, Rede, Elo, Visa Brasil, Mastercard Brasil.
   Para cada um, escreva a mensagem personalizada que você enviaria.

2. **1 comunidade para participar ativamente:** Escolha uma das comunidades da teoria
   e planeje 3 contribuições concretas que você faria na primeira semana
   (perguntas respondidas, conteúdo compartilhado, ou issue aberta).

3. **1 evento para participar:** Identifique um evento de pagamentos ou fintech
   nos próximos 90 dias (Febraban Tech, meetup, conferência) e descreva:
   - O que você levaria para compartilhar (projeto, artigo, ideia)
   - Quem você tentaria conhecer e por quê

## Critérios de Avaliação

- [ ] Contribuição para o jPOS bem estruturada e tecnicamente precisa (Parte 1)
- [ ] Issue/PR/teste segue as convenções do projeto open source (Parte 1)
- [ ] Post técnico com no mínimo 600 palavras e todas as seções (Parte 2)
- [ ] Post inclui código Java/jPOS funcional e relevante (Parte 2)
- [ ] Linguagem do post é acessível para o público-alvo definido (Parte 2)
- [ ] 5 mensagens de networking personalizadas, não genéricas (Parte 3.1)
- [ ] Plano de comunidade com contribuições concretas e datas (Parte 3.2)
- [ ] Evento identificado com plano de participação ativo, não passivo (Parte 3.3)

## Dicas

- O melhor post técnico começa com um problema que você mesmo já enfrentou durante o curso
- Mensagens de networking que referenciam algo específico da pessoa têm 3x mais resposta
- Para a contribuição ao jPOS, documentação bem escrita tem tanto valor quanto código
- Contribuições consistentes ao longo do tempo valem mais do que uma contribuição grande
- Publicar no LinkedIn como artigo tem mais alcance no Brasil do que um blog próprio inicial
