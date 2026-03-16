# Semana 5 — Desafio Integrador
## "O Switch que Nunca Subiu"

---

## O Cenário

Você acabou de ser contratado como engenheiro sênior na FinTechBR, uma processadora de pagamentos em rápido crescimento. Seu primeiro dia é uma segunda-feira de carnaval — e o switch de pagamentos do ambiente de homologação está fora do ar desde sexta-feira.

O time anterior deixou um repositório com o projeto jPOS, mas **ninguém conseguiu subir o ambiente**. Os testes de integração com o banco parceiro estão parados. Há uma demo para o cliente na quinta-feira.

Você tem acesso ao repositório e a esta mensagem de erro encontrada nos logs:

```
ERROR Q2 - Error deploying deploy/05_txnmgr.xml
  java.lang.ClassNotFoundException: com.fintechbr.participants.ValidateMessage

INFO  Q2 - deploy/00_logger.xml loaded (rotativo, 30 dias)
INFO  Q2 - deploy/20_mux.xml loaded (mux name=issuer-mux, timeout=30000)
ERROR Q2 - deploy/05_txnmgr.xml: FAILED
WARN  Q2 - deploy/30_server.xml: waiting for txnmgr...
INFO  Q2 - Ready (degraded mode — server not accepting connections)
```

O `pom.xml` do projeto tem apenas:

```xml
<dependency>
    <groupId>org.jpos</groupId>
    <artifactId>jpos</artifactId>
    <version>2.1.9</version>
</dependency>
```

---

## Sua Missão

### Parte 1 — Diagnóstico Completo (30 min)

Crie o documento `diagnostico-startup.md` com:

1. **Liste todos os problemas identificados** no log e no `pom.xml`:
   - O que está faltando?
   - Por que o servidor não aceita conexões?
   - O que é "degraded mode"?

2. **Mapa de dependências:** Desenhe (texto ASCII ou Mermaid) como os componentes do Q2 dependem uns dos outros na ordem de inicialização. Por que o `30_server.xml` ficou esperando o `txnmgr`?

3. **Impacto:** Se o switch ficou em degraded mode em **produção** (e não em homologação), o que teria acontecido? Estime o impacto financeiro considerando:
   - 500.000 transações/dia
   - Ticket médio R$ 120
   - MDR médio 2%

### Parte 2 — Recuperação do Ambiente (45 min)

Implemente e entregue um projeto `payment-switch-lab` funcional que:

1. **`pom.xml` corrigido** com todas as dependências necessárias

2. **Estrutura de diretórios correta** com todos os arquivos no lugar certo

3. **`00_logger.xml`** com log rotativo por dia, guardando 30 dias

4. **`Application.java`** que:
   - Inicia o Q2
   - Faz shutdown graceful no `Ctrl+C`
   - Loga "Q2 pronto para receber conexões" quando tudo estiver UP

5. **Pelo menos um participant dummy** (`ValidateMessage.java`) que:
   - Implementa `TransactionParticipant`
   - No `prepare()`: loga "ValidateMessage: processando transação {id}" e retorna `PREPARED`
   - No `commit()`: loga "ValidateMessage: commit {id}"
   - No `abort()`: loga "ValidateMessage: abort {id}"

6. **`05_txnmgr.xml`** que carrega o participant acima

7. **`Dockerfile`** que permite rodar tudo em container

**Evidência de sucesso:** Cole aqui o log de startup sem erros (pode ser o log real ou simulado).

### Parte 3 — Documentação para o Time (15 min)

Escreva `onboarding-guide.md` (máximo 1 página) para que qualquer novo engenheiro possa:

1. Clonar o repositório
2. Entender a arquitetura em 5 minutos
3. Rodar o projeto localmente
4. Adicionar um novo participant sem quebrar nada

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| Diagnóstico identifica todos os problemas corretamente | /20 |
| Entende e explica a ordem de inicialização do Q2 | /15 |
| Projeto sobe sem erros (log limpo) | /25 |
| Participant implementado corretamente (prepare/commit/abort) | /20 |
| Documentação clara e acionável | /20 |

**Meta:** 80+ pontos = Semana 5 dominada.

---

## Dicas

- O número no prefixo do arquivo XML (ex: `05_`) determina a **ordem de deploy**. Pense bem na dependência entre componentes.
- O Q2 não vai iniciar o servidor TCP se o TransactionManager não estiver pronto. Isso é intencional — evita aceitar conexões sem conseguir processar.
- Em produção, "degraded mode" é um incidente P1. O switch aceita a conexão TCP mas não consegue processar — o terminal recebe timeout e o cliente fica na dúvida se a transação foi ou não processada.
- Cronometre. Na entrevista técnica da FinTechBR, você vai ter exatamente 90 minutos para resolver este tipo de problema ao vivo.
