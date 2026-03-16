# Semana 24 — Exercícios de Fixação

## Exercício 1 — Diagrama C4: O Modelo (Nível: Iniciante)

O modelo C4 (Context, Container, Component, Code) organiza a arquitetura em 4 níveis de abstração.

Para o payment-switch-lab, descreva (sem ainda criar os diagramas) o que vai em cada nível:

| Nível | O que mostra | Audiência | Exemplo do switch |
|-------|-------------|-----------|-------------------|
| Context (L1) | | | |
| Container (L2) | | | |
| Component (L3) | | | |
| Code (L4) | | | |

Depois responda:
1. Por que um arquiteto externo precisa ver o diagrama C4 nível 1 (Context) antes de qualquer outra coisa?
2. Qual nível mostra a decisão "usamos jPOS como framework"?
3. Qual nível mostra a decisão "TransactionManager usa o padrão Participant"?

**Critério de sucesso:** Tabela preenchida corretamente, 3 perguntas respondidas.

---

## Exercício 2 — Diagrama C4 Nível 1 e 2 (Nível: Iniciante/Intermediário)

Crie os diagramas C4 Nível 1 (Context) e Nível 2 (Container) para o payment-switch-lab usando Mermaid ou PlantUML.

**Nível 1 (Context):** Deve mostrar:
- O Payment Switch como sistema central
- Atores externos: Terminal POS, Emissor, Bandeira Visa, Bandeira Mastercard, Bandeira Elo
- Relações com setas e descrições

**Nível 2 (Container):** Deve mostrar:
- payment-switch-lab (JVM, Java 17)
- Banco de dados (para deduplicação e reversals pendentes)
- Fila de reversals persistentes
- Canais TCP para cada bandeira
- Tecnologias usadas em cada container

Salve como `c4-context.mermaid` e `c4-container.mermaid`.

**Critério de sucesso:** Diagramas tecnicamente precisos, todos os componentes externos incluídos.

---

## Exercício 3 — Diagrama C4 Nível 3 (Component) (Nível: Intermediário)

Crie o diagrama C4 Nível 3 para o container `payment-switch-lab`, mostrando os componentes internos:

Componentes a incluir:
- `TransactionRouter` — roteia por MTI/BIN
- `ValidateMessage` participant
- `DuplicateChecker` participant
- `ChannelGuard` participant
- `RouteMessage` participant
- `AutoReversalEngine` participant
- `SafQueue`
- `ChannelHealthCheck`
- `SwitchMetrics`
- `SessionManager`

Para cada componente: nome, responsabilidade (1 linha), tecnologia, interações com outros componentes.

Salve como `c4-component.mermaid`.

**Critério de sucesso:** Todos os componentes incluídos, responsabilidades precisas, interações corretas.

---

## Exercício 4 — ADRs: Documente as 5 Decisões Mais Importantes (Nível: Intermediário)

Escreva 5 ADRs (Architecture Decision Records) para o payment-switch-lab.

Formato de cada ADR:
```markdown
# ADR-001: [Título da Decisão]

## Status
[Accepted | Deprecated | Superseded by ADR-XXX]

## Contexto
[Qual problema estava sendo resolvido? Quais forças estavam em jogo?]

## Decisão
[O que foi decidido?]

## Consequências
[Positivas e negativas desta decisão]

## Alternativas Consideradas
[O que mais foi considerado e por que foi rejeitado?]
```

ADRs obrigatórias:
1. **ADR-001:** Por que usar jPOS TransactionManager + Participants em vez de um único serviço monolítico?
2. **ADR-002:** Por que cache em memória (Caffeine) para deduplicação em vez de banco de dados?
3. **ADR-003:** Por que timeout de 30s para autorização?
4. **ADR-004:** Reversal síncrono vs assíncrono — qual foi escolhido e por quê?
5. **ADR-005:** Escalabilidade horizontal — como o switch escala e qual o impacto no estado (deduplicação, reversals)?

**Critério de sucesso:** 5 ADRs com todos os campos preenchidos, trade-offs claramente documentados.

---

## Exercício 5 — Diagrama de Deployment (Nível: Intermediário)

Crie `docker-compose.yml` e um diagrama de deployment (`deployment.mermaid`) para o payment-switch-lab.

O Docker Compose deve incluir:
- `payment-switch`: o serviço principal (Java/jPOS)
- `postgres`: banco de dados para persistência
- `prometheus`: coleta de métricas
- `grafana`: dashboards
- `wiremock` (ou similar): mock dos emissores para testes

O diagrama de deployment deve mostrar:
- Onde cada container roda
- Portas expostas
- Volumes persistentes
- Redes Docker
- Variáveis de ambiente críticas (sem valores sensíveis)

**Critério de sucesso:** Docker Compose funcional (`docker compose up` sobe tudo), diagrama coerente com o YAML.

---

## Exercício 6 — Trade-offs: Documente Explicitamente (Nível: Avançado)

Escreva `trade-offs.md` documentando os trade-offs explícitos do payment-switch-lab:

Para cada decisão, use o formato:

```
## Trade-off: [Nome]
**Escolha feita:** [O que foi feito]
**O que ganhamos:** [Benefícios]
**O que sacrificamos:** [Custos/limitações]
**Quando isso vira um problema:** [Em que cenário a decisão precisa ser revisitada]
```

Trade-offs obrigatórios:
1. Cache em memória para deduplicação (ganho: velocidade; custo: estado local, não escala horizontalmente sem coordenação)
2. Reversal assíncrono (ganho: não bloqueia o portador; custo: complexidade de garantir entrega)
3. jPOS como framework (ganho: protocolo pronto; custo: curva de aprendizado, dependência de framework)
4. Timeout de 30s fixo (ganho: simplicidade; custo: não adaptável a emissores lentos vs rápidos)
5. Persistência local de SAF (ganho: sobrevive restart; custo: não compartilhado entre instâncias)

**Critério de sucesso:** 5 trade-offs documentados com clareza e honestidade sobre os custos.

---

## Exercício 7 — Pitch para 3 Audiências (Nível: Avançado)

Prepare 3 apresentações do payment-switch-lab para audiências diferentes. Cada pitch deve ter no máximo 10 linhas escritas (não é uma apresentação oral, é um texto):

**Pitch 1 — Para o Arquiteto Sênior:**
Foco: decisões técnicas, trade-offs, pontos de melhoria, débito técnico. Use termos técnicos sem restrição.

**Pitch 2 — Para o Head de Produto/Gerente:**
Foco: o que o sistema faz (não como), riscos de negócio, capacidade (TPS), confiabilidade, o que ainda não está pronto. Sem jargão técnico.

**Pitch 3 — Para o Time de Operações:**
Foco: como monitorar, o que fazer quando algo dá errado, quais alertas existem, onde estão os runbooks. Linguagem operacional.

Salve em `architecture-pitches.md`.

**Critério de sucesso:** 3 pitches distintos e adaptados para cada audiência, todos tecnicamente precisos.
