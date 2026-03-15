# Semana 24 — Desafio Integrador

## O Cenário

Você passou 24 semanas construindo o payment-switch-lab. Agora é hora do test final. A empresa está avaliando se deve usar o seu switch em produção ou contratar uma solução de prateleira.

O CTO agendou uma Architecture Review Board (ARB) com 4 pessoas:
- **Arquiteto de Segurança:** Foco em PCI DSS, gestão de chaves, proteção de dados
- **VP de Engenharia:** Foco em escalabilidade, confiabilidade, débito técnico
- **Head de Operações:** Foco em monitoramento, incident response, runbooks
- **Gerente Financeiro:** Foco em custo, risco financeiro, ROI de construir vs comprar

Você tem 45 minutos para apresentar. Cada pessoa terá 10 minutos para perguntas.

## Sua Missão

### Parte 1 — Documentação Arquitetural (30 min)

Produza os seguintes artefatos em `architecture-review/`:

**1. README-EXECUTIVO.md** (máximo 2 páginas):
- O que o sistema faz (para o Gerente Financeiro)
- Capacidade atual (TPS, latência, disponibilidade)
- O que está implementado vs o que falta
- Riscos conhecidos

**2. ARCHITECTURE.md** (para o Arquiteto):
Inclua:
- Diagrama C4 nível 2 (containers)
- Diagrama C4 nível 3 (components)
- As 5 ADRs principais
- Trade-offs explícitos
- Débito técnico identificado

**3. SECURITY.md** (para o Arquiteto de Segurança):
Inclua:
- Como PAN/PIN são protegidos (logging, transmissão, armazenamento)
- Ciclo de vida de chaves criptográficas (ZPK, DUKPT)
- Quais controles PCI DSS estão implementados
- O que falta para certificação PCI DSS completa
- Como são tratados os dados de portadores em logs e banco de dados

**4. OPERATIONS.md** (para o Head de Operações):
Inclua:
- Como deployar (Docker Compose + variáveis)
- Como monitorar (métricas chave, dashboards)
- Como responder aos 5 incidentes mais comuns (runbook simplificado)
- Como escalar horizontalmente (o que é necessário)
- Como fazer rollback em caso de problema

### Parte 2 — Perguntas Difíceis (20 min)

Escreva `hard-questions.md` com suas respostas para as perguntas mais difíceis que cada pessoa fará:

**Arquiteto de Segurança perguntará:**
1. "Se um servidor de produção for comprometido, quais dados de portadores ficam em risco?"
2. "O sistema passou por pentesting? Qual foi o resultado?"
3. "Como as chaves ZPK são rotacionadas sem downtime?"

**VP de Engenharia perguntará:**
1. "O sistema tem estado em memória (cache de deduplicação). Como escala para 3 instâncias sem cobranças duplicadas?"
2. "Qual é o MTTR (Mean Time To Recovery) estimado para cada tipo de falha?"
3. "O que você faria diferente se começasse hoje?"

**Head de Operações perguntará:**
1. "Às 3h da manhã, o alerta 'reversal_exhausted' dispara. Qual é o primeiro passo?"
2. "Como o time de operações sabe se o sistema está saudável sem você?"
3. "Quanto tempo para treinar um novo membro do time a operar esse sistema?"

**Gerente Financeiro perguntará:**
1. "Uma solução de prateleira custa R$ 50.000/mês. Quanto custa manter esse sistema? (infra + time)"
2. "Qual é o risco financeiro de uma falha crítica? Quanto o negócio pode perder por hora de downtime?"
3. "O sistema é auditável para fins de regulação BACEN?"

Para cada pergunta: responda honestamente, incluindo o que o sistema ainda não faz.

### Parte 3 — Veredicto e Próximos Passos (5 min)

Escreva `next-steps.md`:

1. **O sistema está pronto para produção?** Responda com honestidade: sim/não/parcialmente. Justifique.

2. **O que falta para produção?** Liste os 5 itens mais críticos, em ordem de prioridade.

3. **Construir vs Comprar:** Dado o esforço até aqui, quando faz sentido construir um switch próprio vs usar uma solução de prateleira?

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| README-EXECUTIVO claro e honesto sobre capacidades e gaps | /15 |
| ARCHITECTURE.md com diagramas C4 e ADRs de qualidade | /20 |
| SECURITY.md completo e alinhado com PCI DSS | /20 |
| OPERATIONS.md acionável e suficiente para operação autônoma | /20 |
| Respostas às perguntas difíceis são honestas e bem fundamentadas | /25 |

**Meta:** 80+ pontos = Fase 6 completa. Você está pronto para produção.

---

## Dicas

- O ARB não quer ouvir que o sistema é perfeito. Quer ouvir que você entende as limitações e tem um plano.
- A pergunta mais difícil é a do VP de Engenharia sobre escala horizontal: cache em memória + instâncias múltiplas = duplicatas. Não existe resposta fácil — escolha entre: Redis compartilhado, sticky sessions, ou aceitar janela de duplicata.
- A pergunta do Gerente Financeiro sobre "construir vs comprar" não tem resposta técnica — tem resposta de negócio. Pense em: volume de transações, time-to-market, custo de manutenção, controle sobre o roadmap.
- "O sistema está pronto para produção?" — Se você foi honesto durante as 24 semanas, a resposta provavelmente é "parcialmente". Isso é completamente válido. O objetivo era aprender, não ter um sistema de missão crítica.
- Cronometre. Em um ARB real, você tem 45 minutos para convencer 4 pessoas exigentes. Pratique a apresentação.
