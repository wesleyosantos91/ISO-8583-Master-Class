# Response Codes — Referência Completa (DE 39)

> Para cada response code: o que significa, causa provável, e ação recomendada.

## Aprovação

| Código | Significado | Ação |
|--------|-------------|------|
| **00** | Approved | Transação bem-sucedida |
| **10** | Partial Approval | Aprovado por valor menor. Terminal deve oferecer complemento. |
| **85** | No reason to decline | Usado em validação de cartão (zero-dollar auth) |

## Referral

| Código | Significado | Causa provável | Ação |
|--------|-------------|----------------|------|
| **01** | Refer to card issuer | Emissor quer falar com portador | Pedir que ligue pro banco |
| **02** | Refer to card issuer (special) | Condição especial | Idem |

## Decline — Problema no cartão

| Código | Significado | Causa provável | Ação |
|--------|-------------|----------------|------|
| **05** | Do not honor | Genérico — emissor não quer dizer o motivo | Tentar outro meio. NÃO retransmitir. |
| **14** | Invalid card number | PAN falhou Luhn ou não existe na base | Verificar digitação ou cartão danificado |
| **41** | Lost card — pick up | Cartão reportado como perdido | **Reter cartão se possível** |
| **43** | Stolen card — pick up | Cartão reportado como roubado | **Reter cartão se possível** |
| **54** | Expired card | Cartão vencido (DE14 < data atual) | Usar cartão novo |
| **57** | Transaction not permitted to cardholder | Tipo de transação bloqueada para este cartão | Verificar se cartão aceita este tipo de compra |
| **62** | Restricted card | Cartão com restrição (internacional, etc.) | Verificar restrições do cartão |

## Decline — Problema financeiro

| Código | Significado | Causa provável | Ação |
|--------|-------------|----------------|------|
| **51** | Insufficient funds | Saldo ou limite insuficiente | Valor menor ou outro meio |
| **61** | Exceeds withdrawal amount limit | Acima do limite por transação | Valor menor |
| **65** | Exceeds withdrawal frequency limit | Muitas transações no período | Aguardar ou contactar banco |

## Decline — Problema de PIN/Segurança

| Código | Significado | Causa provável | Ação |
|--------|-------------|----------------|------|
| **55** | Incorrect PIN | PIN digitado não confere | Tentar novamente (máx 3) |
| **75** | Allowable PIN tries exceeded | 3+ tentativas erradas | Cartão bloqueado — contactar banco |
| **59** | Suspected fraud | Score de fraude alto | Não retransmitir. Contactar banco. |

## Erro técnico — Problema no seu sistema

| Código | Significado | Causa provável | Ação técnica |
|--------|-------------|----------------|------|
| **12** | Invalid transaction | MTI ou Processing Code inválido | Verificar packager e spec |
| **13** | Invalid amount | Valor fora do range (zero, negativo, muito alto) | Verificar DE4 |
| **30** | **Format error** ★ | Campo malformado, encoding errado, bitmap inconsistente | **Debug urgente — hex dump** |
| **58** | Transaction not permitted to terminal | Terminal não autorizado para este tipo de transação | Verificar cadastro do terminal |
| **76** | Unable to locate previous message | Reversal de transação não encontrada | Original pode não ter chegado (OK) |
| **77** | Inconsistent reversal data | Dados do reversal não batem com o original | Verificar DE90 |

## Erro de infraestrutura

| Código | Significado | Causa provável | Ação |
|--------|-------------|----------------|------|
| **68** | Response received too late | Timeout — emissor demorou demais | Auto-reversal + retry |
| **91** | **Issuer unavailable** ★ | Emissor fora do ar | Stand-in ou decline. Monitorar. |
| **92** | Routing error | Destino não encontrado (BIN não roteado) | Verificar tabela de BINs |
| **96** | **System malfunction** ★ | Erro genérico no receptor | Debug no lado receptor |

## Response Codes mais críticos para monitorar

```
DE39=30 em alta → Problema de spec/encoding → Debug urgente
DE39=91 em alta → Emissor fora → Ativar stand-in ou alertar
DE39=96 em alta → Erro sistêmico → Verificar destino
DE39=68 em alta → Timeouts → Verificar rede/latência
DE39=05 em alta → Emissor negando muito → Verificar se BIN mudou de regra
```

---

# Modelo Econômico de Pagamentos

## Fluxo de Dinheiro

```
                    PORTADOR
                    paga R$ 100 na fatura
                        │
                        ▼
                    EMISSOR (Itaú)
                    └── Recebe R$ 100 do portador
                    └── Paga R$ 98,50 para a bandeira (100 - interchange 1,50)
                        │
                        ▼
                    BANDEIRA (Visa)
                    └── Recebe R$ 98,50
                    └── Desconta assessment fee (R$ 0,20)
                    └── Paga R$ 98,30 para o adquirente
                        │
                        ▼
                    ADQUIRENTE (Cielo)
                    └── Recebe R$ 98,30
                    └── Desconta margem própria (R$ 0,80)
                    └── Paga R$ 97,50 para o merchant
                        │
                        ▼
                    MERCHANT
                    └── Recebe R$ 97,50 (MDR = 2,50%)
```

## Receita por ator

| Ator | Fontes de receita | Receita neste exemplo |
|------|-------------------|----------------------|
| Emissor | Interchange + anuidade + juros rotativo + parcelas | R$ 1,50 |
| Bandeira | Assessment fee + licenciamento | R$ 0,20 |
| Adquirente | MDR - interchange - assessment + antecipação | R$ 0,80 |
| Processadora | Fee por mensagem processada (se terceirizada) | R$ 0,02-0,10 |

## Interchange no Brasil

| Tipo | Interchange típico | Regulação BACEN |
|------|-------------------|-----------------|
| Débito | 0,5% | Teto regulado (Circular 4.739) |
| Crédito à vista | 1,2 - 1,8% | Em discussão |
| Crédito parcelado | 1,5 - 2,5%+ | Em discussão |
| CNP (e-commerce) | +0,2-0,5% adicional | — |

## Impacto técnico na receita

| Decisão técnica | Impacto financeiro |
|-----------------|-------------------|
| Roteamento on-us quando deveria ser off-us | Paga interchange desnecessário |
| Tabela de BINs desatualizada | Transações vão pra rota errada |
| Timeout alto demais | Mais reversals, menos aprovações |
| Timeout baixo demais | Reversals prematuros, inconsistências |
| Switch lento (latência alta) | Mais timeouts na cadeia, menos aprovações |
| Deduplicação falha | Cobranças duplas, chargebacks, multas |
| Clearing com mismatch | Exceções financeiras, investigação manual |
