# Semana 19 — Desafio Integrador

## O Cenário

Você é engenheiro no time de pagamentos de uma grande plataforma de e-commerce brasileira (similar ao Mercado Livre). O Head de Produto apresenta um roadmap ambicioso:

> "Queremos implementar 3 funcionalidades até o fim do trimestre:
> 1. **One-click buy:** O portador cadastra o cartão uma vez e compra com 1 clique nas próximas vezes
> 2. **Assinatura Prime:** Cobrança mensal automática de R$ 29,90 sem o portador precisar fazer nada
> 3. **Apple Pay / Google Pay:** Pagamento com carteira digital no app"

Você precisa avaliar a viabilidade técnica, os campos ISO 8583 envolvidos e os riscos de cada funcionalidade.

## Sua Missão

### Parte 1 — Mapeamento Técnico (30 min)

Escreva `mapeamento-tecnico-s19.md` com uma análise técnica ISO 8583 para cada funcionalidade:

**Funcionalidade 1: One-Click Buy (COF)**

1. **Primeira transação (CIT):**
   - Quais campos ISO 8583 devem estar presentes?
   - O que o adquirente precisa guardar após a aprovação?
   - O portador precisa autenticar de alguma forma?

2. **Transações subsequentes (MIT one-click):**
   - Diferença nos campos DE 22, DE 25, DE 48 em relação à CIT
   - O que o merchant envia para identificar que é uma MIT de um COF anterior?
   - Se o cartão expirou: qual é o fluxo?

3. **Riscos:** Quem é responsável por chargeback em caso de fraude na MIT?

**Funcionalidade 2: Assinatura Prime (Recurring)**

1. **Diferenças para One-Click Buy:**
   - Recurring vs on-demand: quais campos mudam?
   - Como indicar que é cobrança recorrente (não pontual)?

2. **Cenário de falha:** Cartão recusado na cobrança de fevereiro. Quais são os passos?
   - Quantas retentativas?
   - Com qual intervalo?
   - O que mostrar ao usuário?

3. **Account Updater:** Como integrar para renovação automática de cartões expirados?

**Funcionalidade 3: Apple Pay / Google Pay (Tokenização)**

1. **FPAN vs DPAN:** O que muda no DE 2 quando o portador paga com Apple Pay?

2. **Fluxo de autorização:**
   - O adquirente recebe o DPAN no DE 2
   - Quem faz a de-tokenização?
   - Em que ponto o emissor vê o FPAN?

3. **Diferenças no DE 55:** Que tags estão presentes em pagamento via Apple Pay que não estão em chip contact?

### Parte 2 — Modelagem do Fluxo Completo (25 min)

Escolha a funcionalidade mais complexa (Assinatura Prime) e modele o ciclo de vida completo de um assinante:

**Mês 1:** Portador assina o Prime
- Quais mensagens ISO 8583 são trocadas?
- O que é persistido no banco do merchant?

**Meses 2-5:** Cobranças automáticas bem-sucedidas
- Mostrar apenas uma cobrança MIT com todos os campos relevantes

**Mês 6:** Cartão expira (31/05)
- Cobrança de junho falha com DE39=14 (Invalid Card)
- Account Updater descobre novo cartão
- Cobrança reprocessada com sucesso
- Quais mensagens foram trocadas?

**Mês 8:** Portador contesta a cobrança (chargeback)
- Alega que não autorizou a assinatura
- Qual é a defesa do merchant?
- Qual documentação (evidências ISO 8583) o merchant precisa apresentar?

Crie um diagrama Mermaid (`assinatura-prime-lifecycle.mermaid`) cobrindo todos os meses acima.

### Parte 3 — Avaliação de Riscos e Recomendação (15 min)

Escreva `risk-assessment-s19.md`:

1. **Matriz de riscos** para as 3 funcionalidades:

| Funcionalidade | Risco de Fraude | Risco de Chargeback | Complexidade Técnica | Recomendação |
|----------------|----------------|---------------------|---------------------|-------------|
| One-Click Buy | | | | |
| Assinatura Prime | | | | |
| Apple Pay | | | | |

2. **Ordem de implementação recomendada:** Qual implementar primeiro e por quê?

3. **Pré-requisitos:** O que precisa estar pronto antes de implementar cada funcionalidade? (Ex: COF precisa de Network Transaction ID → integração com bandeira para obter esse ID)

---

## Critérios de Avaliação

| Critério | Pontos |
|----------|--------|
| Mapeamento técnico correto dos campos ISO 8583 para cada funcionalidade | /30 |
| Fluxo do ciclo de vida da assinatura é completo e tecnicamente preciso | /30 |
| Análise de chargeback e evidências necessárias | /15 |
| Diagrama Mermaid correto e legível | /15 |
| Matriz de riscos e recomendação são justificadas | /10 |

**Meta:** 80+ pontos = Semana 19 dominada.

---

## Dicas

- COF é simples na teoria mas complexo na prática: você precisa guardar o Network Transaction ID retornado pela bandeira na primeira transação e usá-lo nas subsequentes. Se não tiver esse ID, a MIT pode ser recusada.
- Assinatura e One-Click são COF, mas com indicadores diferentes no DE 48. Consulte os guias de integração da Visa/Mastercard para os valores exatos.
- Apple Pay: o DPAN começa com o mesmo BIN do FPAN. O emissor precisa de de-tokenização antes de autorizar — verifique quem na sua integração faz isso.
- Chargeback em MIT sem documentação adequada é quase sempre perdido pelo merchant. A evidência mais forte é: Network Transaction ID + timestamp da CIT assinada + evidência de que o portador autorizou o armazenamento.
- Cronometre. Em produção, o Head de Produto vai pedir uma estimativa de prazo ao final desta análise.
