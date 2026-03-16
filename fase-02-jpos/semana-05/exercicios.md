# Semana 5 — Exercícios de Fixação
## Setup jPOS + Q2 Runtime

---

## Exercício 1 — Vocabulário do jPOS (Nível: Iniciante)

Preencha a tabela abaixo sem consultar a teoria:

| Conceito | O que é | Para que serve | Analogia com outro framework |
|----------|---------|----------------|------------------------------|
| Q2 | | | |
| deploy/ | | | |
| TransactionManager | | | |
| TransactionParticipant | | | |
| Space | | | |
| QMUX | | | |

**Critério de sucesso:** Conseguiu explicar os 6 conceitos com precisão técnica.

---

## Exercício 2 — Estrutura de Projeto (Nível: Iniciante)

Dado o repositório `payment-switch-lab`, crie a estrutura de diretórios completa conforme aprendido:

```
payment-switch-lab/
├── src/main/java/com/lab/
│   ├── ???/   ← participants ficam aqui
│   ├── ???/   ← canais customizados
│   └── ???/   ← utilitários
├── src/main/resources/
│   └── ???/   ← packagers XML
├── src/dist/
│   └── deploy/
│       ├── ???_logger.xml
│       ├── ???_txnmgr.xml
│       ├── ???_channel.xml
│       ├── ???_mux.xml
│       └── ???_server.xml
└── src/test/java/
```

1. Preencha os `???` com os nomes corretos.
2. Explique por que os arquivos XML do deploy usam **prefixo numérico** (ex: `00_`, `05_`, `10_`).
3. O que acontece se dois arquivos tiverem o mesmo prefixo?

**Critério de sucesso:** Estrutura correta, explicação coerente sobre a ordem de inicialização.

---

## Exercício 3 — pom.xml Mínimo (Nível: Iniciante)

Escreva o trecho do `pom.xml` necessário para rodar o jPOS 2.1.9 com suporte ao TransactionManager:

1. Qual é o `groupId` e `artifactId` da dependência principal do jPOS?
2. Qual dependência adicional é necessária para o TransactionManager?
3. Qual plugin Maven é recomendado para criar o JAR executável com o Q2?
4. O que acontece se você esquecer a dependência `jposee-txn`?

Escreva o XML completo com as dependências e justifique cada uma.

**Critério de sucesso:** XML compilável e justificativas corretas.

---

## Exercício 4 — Ciclo de Vida do Q2 (Nível: Intermediário)

Responda com base no comportamento do Q2:

1. O Q2 monitora mudanças em runtime no diretório `deploy/`. O que acontece se você:
   a. Adicionar um novo arquivo XML enquanto o Q2 está rodando?
   b. Remover um arquivo XML existente?
   c. Modificar um arquivo XML existente?

2. Dado o log abaixo, explique o que está acontecendo e se há problema:
   ```
   INFO  Q2 - deploy/00_logger.xml loaded
   INFO  Q2 - deploy/05_txnmgr.xml loaded
   WARN  Q2 - deploy/10_channel.xml: class not found: com.lab.channel.CustomChannel
   INFO  Q2 - deploy/20_mux.xml loaded
   ```

3. Qual a diferença entre `q2.start()` e `q2.run()`?

**Critério de sucesso:** Explicou o hot deploy corretamente e identificou o problema no log.

---

## Exercício 5 — Logger XML (Nível: Intermediário)

Crie o arquivo `00_logger.xml` que satisfaça todos os requisitos:

1. Log rotativo **por dia** (arquivo novo a cada dia)
2. Formato de arquivo: `log-YYYY-MM-DD.txt`
3. Guarda logs dos últimos **30 dias** (deleta os mais antigos)
4. Nível de log configurável via propriedade do sistema (`-Dlog.level=...`)
5. Log de console para desenvolvimento (pode desligar em produção)

Documente cada atributo XML com um comentário explicando o que faz.

**Critério de sucesso:** XML válido para o jPOS, todos os 5 requisitos atendidos.

---

## Exercício 6 — Application.java (Nível: Intermediário)

Implemente a classe `Application.java` que:

1. Instancia e inicia o Q2
2. Registra um shutdown hook para fechar o Q2 graciosamente quando `Ctrl+C` for pressionado
3. Aguarda o Q2 terminar antes de encerrar a JVM
4. Loga no console: "Q2 iniciado" após startup e "Q2 encerrado" após shutdown
5. Aceita argumento de linha de comando `-d <diretório>` para usar um diretório `deploy/` alternativo

**Critério de sucesso:** Classe compilável e funcional, shutdown graceful funcionando.

---

## Exercício 7 — Diagnóstico de Startup (Nível: Avançado)

O Q2 foi iniciado mas não está processando mensagens. O log mostra:

```
INFO  Q2 - Starting jPOS 2.1.9
INFO  Q2 - deploy/00_logger.xml loaded
INFO  Q2 - deploy/05_txnmgr.xml loaded
INFO  Q2 - deploy/20_mux.xml loaded
WARN  Q2 - deploy/30_server.xml: port 8583 already in use
INFO  Q2 - Ready.
```

1. Qual componente está faltando no startup? Por que causa problema?
2. O que significa "port 8583 already in use"? Como investigar e resolver?
3. Como você verificaria se o Q2 está de fato pronto para receber conexões?
4. Escreva um script shell simples que verifica se o processo jPOS está rodando e se a porta está ouvindo.

**Critério de sucesso:** Diagnóstico correto, script funcional.

---

## Exercício 8 — Docker + jPOS (Nível: Avançado)

Crie um `Dockerfile` e um `docker-compose.yml` para o `payment-switch-lab`:

**Dockerfile deve:**
- Usar imagem base `eclipse-temurin:17-jdk-alpine`
- Fazer build Maven dentro do container (multi-stage build)
- Copiar apenas os artefatos necessários para a imagem final
- Expor a porta `8583`
- Definir o comando de startup do Q2

**docker-compose.yml deve:**
- Definir o serviço `payment-switch`
- Mapear a porta `8583`
- Montar o volume `./logs:/app/logs` para persistir logs
- Definir health check que verifica se a porta `8583` está ouvindo
- Reiniciar automaticamente em caso de falha (`restart: unless-stopped`)

**Critério de sucesso:** `docker-compose up` sobe o container, Q2 inicia, health check passa em menos de 60 segundos.

---

## Exercício 9 — README Técnico (Nível: Iniciante)

Escreva o `README.md` do projeto `payment-switch-lab` com:

1. **O que é** este projeto (2-3 linhas)
2. **Pré-requisitos** (Java, Maven, Docker)
3. **Como rodar localmente** (passo a passo)
4. **Como rodar com Docker**
5. **Estrutura do projeto** (mapa de diretórios comentado)
6. **Como funciona o Q2** (explicação em linguagem simples)
7. **Como adicionar um novo participant** (passo a passo)

**Critério de sucesso:** Um desenvolvedor novo consegue rodar o projeto seguindo apenas o README.
