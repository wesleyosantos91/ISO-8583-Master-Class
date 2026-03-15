# Desafio — Semana 32 — Vazamento de Dados e Resposta a Breach

## Contexto

Você lidera o time de engenharia de risco de uma processadora que atende 8 emissores
de pequeno e médio porte. São 11h de uma segunda-feira quando você recebe uma ligação
do time de segurança:

> "Encontramos evidências de acesso não autorizado ao banco de dados de transações.
> O intruso teve acesso entre sexta 22h e sábado 03h. Estimamos exposição de
> 180.000 registros de transações, incluindo PANs truncados 6+4 e dados de merchant."

Ao mesmo tempo, o dashboard de fraude mostra:

```
[ALERTA] Taxa de fraude nas últimas 24h: 0.87% (baseline: 0.09%)
[INFO]   Volume de chargebacks 10.4 (CNP sem autenticação): +340% vs semana anterior
[ALERTA] 3 emissores reportando pico de contestações de portadores
[INFO]   Origem geográfica das fraudes: 67% internacionais (US, UK, UA)
```

## O Problema

O breach expôs PANs truncados (6+4). Apesar de truncados, combinados com dados de
merchant e data/hora, esses registros podem ser usados para:

1. **Inferência de PAN completo** por atacantes com acesso a outras fontes (dark web, phishing anterior)
2. **Fraude de e-commerce** com PANs completos obtidos previamente, usando os dados
   do breach para parecer transação legítima (mesmo merchant, mesmo perfil)

Os 180.000 registros abrangem transações de 45 dias dos 8 emissores clientes.

## Missão

### Parte 1 — Triagem e Contenção

Com base nos dados disponíveis, implemente o alerta de breach no sistema de monitoramento:

```java
public class BreachAlertHandler {

    // Dado que o breach cobriu transações de 45 dias,
    // implemente a lógica para:
    // 1. Identificar todos os PANs únicos expostos (6+4 format) no período
    // 2. Para cada emissor afetado, gerar um relatório com:
    //    - quantidade de PANs expostos
    //    - merchants mais frequentes no período (top 10)
    //    - faixa de valores das transações
    // 3. Gerar lista de PANs para comunicação emergencial aos emissores
    public BreachReport analyzeExposure(LocalDate breachStart, LocalDate breachEnd,
                                         List<String> affectedIssuers) {
        // Implemente aqui
        // Assuma acesso a transactionRepository.findByDateRangeAndIssuers(...)
        return null;
    }
}
```

### Parte 2 — Playbook de Resposta

Execute o playbook de resposta a incidente respondendo cada passo:

**Passo 1 — Contenção (primeiros 15 minutos):**
- Qual regra emergencial você aplica imediatamente no switch para reduzir a fraude ativa?
- Como você notifica os 8 emissores de forma eficiente sem causar pânico?
- Quem mais precisa ser notificado internamente?

**Passo 2 — Obrigações Regulatórias:**
Complete a tabela abaixo com os prazos e destinatários corretos:

| Regulador | Prazo para notificação | O que reportar |
|-----------|------------------------|----------------|
| Visa      | ?                      | ?              |
| Mastercard| ?                      | ?              |
| BACEN     | ?                      | ?              |
| ANPD      | ?                      | ?              |

**Passo 3 — Comunicação com Portadores:**
- Os portadores afetados devem ser notificados? Por qual lei?
- Qual é o critério para determinar se o risco é "relevante" para o portador?
- Quem é responsável pela notificação: a processadora ou o emissor?

### Parte 3 — Ajuste do Modelo e Post-Mortem

1. Com os novos dados de fraude coletados após o breach, como você usaria esses casos
   confirmados para melhorar o modelo de ML? Descreva o processo em 4 etapas.

2. Escreva a seção "Lições Aprendidas" do post-mortem com pelo menos 3 pontos técnicos
   concretos que poderiam ter prevenido ou detectado o breach mais cedo.

3. Após o incidente, a diretoria quer saber se a processadora deve implementar
   tokenização completa de PANs (não apenas truncamento). Argumente a favor ou contra,
   considerando impacto técnico, custo e conformidade com PCI-DSS.

## Critérios de Avaliação

- [ ] Implementação do `BreachAlertHandler` que identifica PANs por emissor (Parte 1)
- [ ] Contenção emergencial identificada e comunicação interna estruturada (Parte 2, P1)
- [ ] Prazos regulatórios corretos para todos os quatro destinatários (Parte 2, P2)
- [ ] Responsabilidade de notificação ao portador corretamente atribuída (Parte 2, P3)
- [ ] Processo de melhoria do modelo com dados do breach descrito (Parte 3, Q1)
- [ ] Lições aprendidas técnicas e acionáveis, não genéricas (Parte 3, Q2)
- [ ] Argumento sobre tokenização com embasamento técnico e regulatório (Parte 3, Q3)

## Dicas

- PANs truncados no formato 6+4 não são considerados dados sensíveis pelo PCI-DSS, mas
  combinados com outros dados podem constituir risco relevante segundo a LGPD
- Mastercard exige notificação em até 24h após **suspeita** — não após confirmação
- A ANPD interpreta "prazo razoável" do Art. 48 da LGPD como aproximadamente 72 horas
- Tokenização (ex: Network Tokenization da Visa/Mastercard) substitui o PAN por um token
  de uso único ou restrito — considere o impacto nos fluxos de clearing existentes
