# Desafio — Semana 27 — Auditoria PCI: O Switch Está Pronto para Certificação?

## Contexto

Sua empresa recebeu mandato da Visa para obter conformidade PCI-DSS 4.0 até o próximo trimestre. Um QSA (Qualified Security Assessor) externo fará a auditoria formal (ROC — Report on Compliance) em 60 dias.

Você foi designado como responsável técnico pela preparação. O QSA enviou a lista de verificações que vai executar no dia da auditoria — e você tem 60 dias para garantir que o payment-switch-lab passe em cada uma.

## O Problema

Uma análise preliminar identificou os seguintes problemas no sistema atual:

```
[CRÍTICO] Logs de produção contêm PANs em claro em 3 locais:
  - AuthorizationProcessor.java:142: log.debug("Processing auth for PAN={}", msg.getString(2))
  - RouteMessage.java:89: log.info("Routing {}", msg.toString())  // ISOMsg.toString() expõe tudo
  - ErrorHandler.java:67: log.error("Failed message: {}", isoMsg)

[CRÍTICO] DE 35 (Track 2) é logado no modo DEBUG durante troubleshooting

[ALTO] Banco de dados armazena PAN completo na tabela transaction_log (não mascarado)

[MÉDIO] TLS 1.0 ainda está habilitado no canal com o emissor legado (banco X)

[MÉDIO] Não existe inventário formal de ativos no CDE

[BAIXO] Logs de auditoria (quem acessou o quê) não têm retenção de 12 meses
```

## Missão

### Parte 1 — Remediação de Código (Prioridade Crítica)

Corrija todos os pontos críticos no código. Para cada correção:

**Problema 1: PAN exposto nos logs**

```java
// ANTES (violação):
log.debug("Processing auth for PAN={}", msg.getString(2));

// DEPOIS (correto):
// Implemente aqui usando PANMasker
```

**Problema 2: ISOMsg.toString() expõe todos os campos incluindo DE 35, DE 52**

```java
public class SafeISOLogger {
    /**
     * Serializa uma ISOMsg para log de forma segura:
     * - Mascara DE 2 (PAN)
     * - Omite completamente DE 35 (Track 2), DE 36 (Track 3), DE 52 (PIN)
     * - Trunca DE 55 (EMV data) para os primeiros 20 chars + "..."
     * - Todos os outros campos: incluir normalmente
     */
    public static String safeToString(ISOMsg msg) { /* ... */ }
}
```

**Problema 3: Migração do banco de dados**

Escreva a migration SQL que:
1. Cria coluna `pan_masked` (VARCHAR 20)
2. Popula `pan_masked` com `CONCAT(LEFT(pan, 6), '******', RIGHT(pan, 4))`
3. Dropa a coluna `pan` original

Documente: como fazer essa migração sem downtime em produção (com janela de convivência das duas colunas)?

### Parte 2 — Checklist de Conformidade

Avalie o payment-switch-lab contra o checklist PCI-DSS técnico abaixo. Para cada item, marque: CONFORME / NÃO CONFORME / PARCIALMENTE CONFORME e explique o que falta:

```
Código:
[ ] PAN nunca logado sem mascaramento (first 6 + last 4 no mínimo)
[ ] DE 35/36 (track data) nunca persistido após autorização
[ ] CVV2 nunca armazenado (nem em memória além do processamento)
[ ] DE 52 (PIN block) nunca logado
[ ] TLS 1.2+ em todas as conexões externas (sem TLS 1.0/1.1)
[ ] PANs em banco de dados: mascarados ou tokenizados

Infraestrutura:
[ ] Segmentação de rede (switch em VLAN isolada)
[ ] Logs de auditoria com retenção mínima de 12 meses
[ ] Acesso a produção via jump server + MFA

Processo:
[ ] Pentest anual documentado
[ ] Plano de resposta a incidentes existe e foi testado
```

Para cada item NÃO CONFORME, escreva o plano de remediação com prazo estimado.

### Parte 3 — Simulação do QSA: Perguntas Difíceis

O QSA vai fazer estas perguntas no dia da auditoria. Prepare suas respostas por escrito:

1. "Mostre-me onde o PAN é mascarado nos logs. Como você garante que nenhum novo código introduza uma violação?" *(dica: teste automatizado que detecta PANs em claro nos logs)*

2. "Se um desenvolvedor com acesso ao código-fonte de produção quisesse extrair PANs, o que impediria?" *(dica: segmentação de acesso, logs de auditoria, criptografia em repouso)*

3. "Você tem um plano de resposta a incidentes. Quando foi a última vez que vocês simularam uma violação de dados?" *(a resposta honesta vale mais que uma resposta bonita)*

4. "O sistema usa TLS 1.0 com o banco X. Qual o plano de migração e quando será concluído?"

Escreva `qsa-preparation.md` com as respostas para cada pergunta.

## Critérios de Avaliação

- [ ] Todos os logs de PAN corrigidos com `PANMasker` ou `SafeISOLogger`
- [ ] `SafeISOLogger.safeToString()` implementado e testado (DE35, DE52 nunca aparecem)
- [ ] Migration SQL correta com estratégia de zero-downtime documentada
- [ ] Checklist preenchido honestamente com planos de remediação realistas
- [ ] `qsa-preparation.md` com respostas substanciais para as 4 perguntas
- [ ] Teste automatizado que detecta PANs em claro em arquivos de log

## Dicas

- `ISOMsg.toString()` do jPOS expõe todos os campos — **nunca use em produção**. Sempre use um serializer customizado que conhece os campos sensíveis.
- A migração do banco de dados sem downtime segue o padrão expand-contract: adiciona `pan_masked`, popula, aplica código novo que usa `pan_masked`, depois dropa `pan`. Pode levar semanas em produção com alto volume.
- O QSA vai pedir evidências, não apenas afirmações. Leve logs de antes e depois da correção, capturas de tela do pipeline de CI com o teste de auditoria, e o relatório do pentest.
- "Parcialmente conforme" é uma resposta válida e honesta — o QSA prefere honestidade com plano de remediação a conformidade falsa descoberta durante a auditoria.
