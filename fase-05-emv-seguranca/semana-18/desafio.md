# Semana 18 — Desafio Integrador

## O Cenário

Você é engenheiro de segurança em uma fintech que acabou de receber a notificação mais temida:

> **NOTIFICAÇÃO PCI DSS — AUDITORIA DE EMERGÊNCIA**
>
> "Durante o processo de certificação PCI DSS Nível 1, o QSA (Qualified Security Assessor) identificou que os logs de transação do payment-switch-lab contêm PANs completos e PIN blocks em texto. O certificado PCI DSS está sendo suspenso. Todas as transações internacionais (Visa/Mastercard) serão bloqueadas em 72 horas caso o problema não seja corrigido."

A suspensão do certificado PCI DSS significa:
- Bloqueio imediato de transações internacionais
- Multa potencial de USD 10.000 a USD 100.000 por mês
- Possível encerramento do contrato com as bandeiras

A empresa processa R$ 2M/dia. 72 horas para corrigir.

## Sua Missão

### Parte 1 — Auditoria e Mapeamento (25 min)

Escreva `auditoria-s18.md` com:

1. **Faça um inventário completo** de todos os locais no código (ou em uma aplicação fictícia) onde dados sensíveis podem vazar:
   - Logs de aplicação (SLF4J, Log4j)
   - Arquivos de trace do jPOS
   - Dumps de debug
   - Banco de dados (tabelas de auditoria)
   - Mensagens de erro retornadas ao cliente
   - Headers HTTP (se houver API REST em cima do switch)

2. **Classifique cada localização por risco:**
   - P1 (crítico): dado sensível exposto em produção
   - P2 (alto): dado sensível em ambiente não-prod que pode ser acessado
   - P3 (médio): risco existe mas com mitigações

3. **Estime o esforço de correção** para cada item (horas de desenvolvimento + teste).

4. **Crie o plano de 72 horas:** Com sua equipe de 3 engenheiros, como você prioriza e divide as correções para cumprir o prazo?

### Parte 2 — Implementação das Correções (25 min)

Implemente `SecureLoggingFilter` para jPOS:

```java
/**
 * Intercepta todos os logs do jPOS (ISOMsg dump, channel logs)
 * e remove/mascara dados sensíveis antes de escrever no log.
 */
public class SecureLoggingFilter implements LogListener {

    private static final Set<Integer> FORBIDDEN_FIELDS = Set.of(
        2,   // PAN
        35,  // Track 2
        36,  // Track 3
        45,  // Track 1
        52,  // PIN Block
        55   // EMV Data (contém dados criptográficos)
    );

    @Override
    public ISOMsg log(ISOMsg msg, String direction) { /* ... */ }
}
```

Requisitos:
- DE 2 (PAN): substituir por versão mascarada (first6****last4)
- DE 35 (Track 2): substituir por `[TRACK2_REDACTED]`
- DE 52 (PIN): substituir por `[PIN_REDACTED]`
- DE 55 (EMV): substituir por `[EMV_REDACTED_LENGTH=XXX]` (manter tamanho para debugging de framing)
- Todos os outros campos: logar normalmente
- Adicionar campo `security_filter=applied` ao log

Escreva testes que verificam que:
1. PAN completo NÃO aparece nos logs após o filtro
2. PIN block NÃO aparece nos logs
3. MTI, Amount, STAN, TerminalID SIM aparecem nos logs
4. Tamanho do DE 55 SIM aparece nos logs (para debug de framing)

### Parte 3 — Evidências para o QSA (20 min)

O QSA precisa de evidências documentadas de que o problema foi corrigido.

Escreva `qsa-evidence.md` com:

1. **Root Cause Analysis:** O que causou o vazamento de PAN nos logs? (Seja específico sobre o código/configuração)

2. **Lista de arquivos modificados:** Com uma linha descrevendo o que mudou em cada arquivo.

3. **Como verificar a correção:** Instruções passo-a-passo para o QSA verificar que PANs não aparecem mais nos logs:
   - Que comando rodar?
   - Que arquivo verificar?
   - Qual output esperar?

4. **Controles preventivos:** O que foi adicionado para garantir que isso não aconteça novamente?
   - Code review checklist
   - Testes automatizados de segurança
   - Ferramenta de scan de secrets em CI/CD

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| Inventário completo e realista dos pontos de vazamento | /20 |
| Priorização e plano de 72 horas viável | /15 |
| `SecureLoggingFilter` implementado corretamente | /25 |
| Testes verificando ausência de dados sensíveis | /20 |
| Documentação para o QSA é técnica e auditável | /20 |

**Meta:** 80+ pontos = Semana 18 dominada.

---

## Dicas

- PCI DSS não é apenas sobre código — é sobre processos, people e tecnologia. A auditoria cobre tudo.
- O `SecureLoggingFilter` precisa ser aplicado mesmo nos logs de trace do jPOS (que por padrão dumpam a mensagem completa).
- jPOS tem `Logger` e `LogListener` — entenda essa hierarquia para interceptar no lugar certo.
- 72 horas parece muito, mas incluem: desenvolvimento, code review, deploy em staging, validação, deploy em produção e validação final. É corrido.
- Em produção real, você também precisaria fazer uma rotação de segredos (chaves que podem ter sido expostas nos logs) — não apenas corrigir o código.
