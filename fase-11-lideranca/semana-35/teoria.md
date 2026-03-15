# Semana 35 — Contribuição para o Mercado: Open Source, Blog e Comunidade

## Por que contribuir é parte de ser especialista?

Referências de mercado não apenas acumulam conhecimento — elas **distribuem** conhecimento. Especialistas que compartilham o que sabem constroem reputação que abre portas: convites para palestras, consultorias, liderança técnica, oportunidades internacionais. Esta semana é sobre como transformar seu conhecimento em impacto público.

---

## 1. Contribuindo para o jPOS (Open Source)

### 1.1 Por que contribuir para o jPOS

O jPOS é o framework mais usado para ISO 8583 em Java. Contribuir:
- Demonstra competência técnica publicamente
- Você tem acesso direto aos maintainers (Alejandro Revilla e core team)
- Seu nome aparece em um projeto que empresas reais usam em produção
- Você aprende o código de um switch real ao ler o jPOS source

### 1.2 Como começar

```
1. Fork e setup:
   git clone https://github.com/jpos/jPOS.git
   cd jPOS && mvn install

2. Identifique uma contribuição adequada:
   - Issues marcadas com "good first issue"
   - Documentação desatualizada
   - Testes faltando
   - Bug que você encontrou usando jPOS

3. Antes de codar, discuta:
   - Abra uma issue descrevendo o que você quer fazer
   - Aguarde feedback do maintainer
   - Evite trabalhar semanas em algo que será rejeitado

4. Processo de contribuição:
   - Branch com nome descritivo: fix/qmux-null-pointer-on-timeout
   - Testes unitários são obrigatórios
   - Seguir o estilo de código do projeto
   - PR com descrição clara do problema e solução
```

### 1.3 Tipos de contribuição valorizadas

```
Alta visibilidade:
  - Corrigir bug crítico (NullPointerException em produção)
  - Adicionar suporte a novo protocolo/bandeira
  - Melhorar performance mensurável

Média visibilidade:
  - Adicionar exemplos de uso
  - Melhorar mensagens de erro
  - Corrigir documentação desatualizada

Boa para começar:
  - Tradução de documentação
  - Adicionar testes para código não coberto
  - Reportar bugs com reproducible case
```

---

## 2. Escrevendo Conteúdo Técnico

### 2.1 Tipos de conteúdo e suas audiências

| Formato | Audiência | Objetivo | Tempo para criar |
|---------|-----------|----------|-----------------|
| Blog post | Desenvolvedores | Ensinar conceito específico | 4-8h |
| Thread (X/LinkedIn) | Mixed | Visibilidade, networking | 1-2h |
| Video/YouTube | Desenvolvedores | Tutorial prático | 8-20h |
| Palestra (meetup) | Desenvolvedores locais | Networking, reputação | 10-20h |
| Palestra (conferência) | Desenvolvedores nacionais | Autoridade no tema | 20-40h |
| Artigo técnico | Especialistas | Contribuição ao conhecimento | 20-60h |

### 2.2 Temas de alto impacto para escrever sobre ISO 8583

```
Temas que performam bem (pouca concorrência, alta procura):
  - "Por que sua transação foi negada: guia técnico dos response codes"
  - "Como funciona o chargeback tecnicamente — o que o developer precisa saber"
  - "jPOS em 2024: ainda vale a pena?"
  - "ISO 8583 vs ISO 20022: o que muda para o developer"
  - "Como o PIX é diferente do cartão por dentro"
  - "O que acontece nos 3 segundos entre passar o cartão e ver 'aprovado'"
  - "PCI-DSS para developers: o mínimo que você precisa saber"
  - "Parcelamento brasileiro: como funciona tecnicamente e por que o mundo não tem"
```

### 2.3 Estrutura de um bom post técnico sobre pagamentos

```markdown
# [Título: promessa clara e específica]

[Hook: situação familiar que todo developer de pagamentos já enfrentou]

## O problema
[Explique o problema ou conceito de forma simples primeiro]

## Como funciona por dentro
[Mergulhe no protocolo, campos, fluxo — com diagramas e código]

## O caso brasileiro
[O que é diferente no Brasil? Sempre tem algo — parcelamento, PIX, etc.]

## Na prática
[Exemplo concreto, snippet de código que o leitor pode usar]

## Resumo
[3-5 bullets com os pontos principais]

[Call to action: "Se gostou, veja também...", "Estou errado em algo?"]
```

---

## 3. Participando de Comunidades

### 3.1 Comunidades relevantes no Brasil

```
Online:
  - jPOS Users Mailing List (lista oficial do jPOS)
  - Grupos no LinkedIn: "Pagamentos Digitais Brasil", "Tecnologia em Pagamentos"
  - Discord/Slack de fintech: FinTech Brasil (Discord), ABFintechs
  - GitHub: Issues e Discussions do jPOS

Eventos presenciais:
  - Money20/20 (maior evento de fintech global, edição Brasil)
  - Febraban Tech (maior evento de tecnologia bancária do Brasil)
  - Meetups de pagamentos em São Paulo, Rio, outros estados
  - Campus Party (público geral mas tem trilha fintech)
```

### 3.2 Como participar de forma que agrega

```
❌ "Alguém sabe como fazer X?" (sem mostrar que pesquisou)

✓ "Tentei implementar X no jPOS seguindo a doc Y, mas estou
   recebendo o erro Z. Aqui está o meu código [gist link] e
   o log completo. O que estou fazendo errado?"

❌ "Nossa empresa usa Y, é muito melhor que X"

✓ "Temos experiência com Y. O que nos levou a escolher foi A e B.
   Em comparação com X, encontramos vantagem em C mas desvantagem em D.
   Depende muito do contexto."
```

### 3.3 Construindo reputação online

```
LinkedIn — estratégia de 90 dias:
  Semanas 1-4:
    - Publicar uma reflexão por semana sobre o que está aprendendo
    - Comentar em posts de outros especialistas com valor (não "ótimo post!")

  Semanas 5-8:
    - Publicar um mini-tutorial ou dica técnica (com imagem ou código)
    - Conectar com pessoas dos eventos e comunidades

  Semanas 9-12:
    - Publicar seu primeiro artigo longo (~1.000 palavras)
    - Responder perguntas na sua área com respostas detalhadas

GitHub — sinal de reputação técnica:
  - Manter o payment-switch-lab público e bem documentado
  - Contribuição consistente (não precisa ser todo dia, mas regular)
  - README que conta uma história, não só lista comandos
  - Issues respondidas (mesmo no seu próprio projeto)
```

---

## 4. Networking Técnico Estratégico

### 4.1 Com quem se conectar

```
Dentro das empresas de pagamento:
  - Arquitetos e engenheiros sêniores de Visa, Mastercard, Elo
  - Tech leads de Cielo, Rede, Stone, PagSeguro, Nubank
  - Times de certificação das bandeiras

Em consultorias e fintechs:
  - Especialistas em PCI-DSS (QSAs)
  - Consultores de pagamentos internacionais

Academicamente:
  - Pesquisadores de segurança em pagamentos (UNICAMP, USP têm grupos)
  - Professores de fintech
```

### 4.2 Como se conectar de forma autêntica

```
Abordagem 1 — Após palestra/meetup:
  "Sua apresentação sobre X foi muito boa. Tenho uma dúvida sobre Y
   que você mencionou — você teria 5 minutos para um café?"

Abordagem 2 — LinkedIn:
  "Olá [nome], vi seu artigo sobre [tema específico] e gostei muito
   do ponto sobre [especificidade]. Estou trabalhando em algo similar
   e tive uma experiência diferente em [aspecto]. Adoraria trocar ideias."

Abordagem 3 — Contribuição:
  Ajude a resolver um problema público da pessoa (bug no projeto delas,
  responder uma dúvida no LinkedIn) antes de pedir qualquer coisa.
```

---

## Resumo da Semana

| Ação | Meta dos próximos 90 dias |
|------|--------------------------|
| Open source | 1 contribuição no jPOS (pode ser documentação) |
| Conteúdo | 3 posts técnicos publicados |
| Comunidade | Participar ativamente em 1 comunidade |
| Networking | Conectar com 5 especialistas de forma autêntica |
| Evento | Participar de 1 meetup ou conferência |
