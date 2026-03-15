# Desafio — Semana 25 — Parcelamento sem Juros: Infraestrutura por Trás de uma Peculiaridade Brasileira

## Contexto

O Brasil é o único país do mundo onde parcelamento sem juros é amplamente oferecido pelo lojista no crédito. Em outros países (EUA, Europa), o portador paga juros ao emissor se não quitar a fatura. No Brasil, o lojista absorve o custo do parcelamento via MDR diferenciado — e toda a infraestrutura técnica foi construída para suportar isso.

Você foi contratado como engenheiro sênior de uma fintech que quer lançar um produto de antecipação de recebíveis para pequenos lojistas. O produto precisa ser construído sobre o switch existente e integrado às registradoras.

## O Problema

O sistema atual do adquirente não:
1. Rastreia as parcelas individuais de cada transação parcelada
2. Registra os recebíveis nas registradoras (CIP/CERC/TAG)
3. Calcula o custo real do parcelamento por BIN/bandeira
4. Permite que o merchant consulte sua agenda de recebíveis futuros

Com 40% do volume em parcelado, a ausência desses dados impossibilita o produto de antecipação.

## Missão

### Parte 1 — Modelagem dos Recebíveis

Projete o modelo de dados para rastrear parcelas de transações parceladas. A partir de uma transação ISO 8583 com `DE 60` (parcelamento Elo) ou `DE 48` subelemento de parcelamento (Visa/Master), o sistema deve gerar os registros de recebíveis correspondentes.

Implemente `InstallmentSplitter`:

```java
public class InstallmentSplitter {

    public record Receivable(
        String transactionId,    // RRN da transação original
        String merchantId,       // DE 42
        String pan4,             // últimos 4 dígitos do PAN
        int installmentNumber,   // 1 de N
        int totalInstallments,   // N
        BigDecimal grossAmount,  // valor bruto da parcela
        BigDecimal mdrAmount,    // desconto MDR
        BigDecimal netAmount,    // valor líquido a receber
        LocalDate settlementDate,// D+30, D+60, ...
        String brand,            // VISA, MASTER, ELO
        String registrarId       // null até registrar na CIP/CERC
    ) {}

    /**
     * Dada uma transação aprovada de R$ 1.200,00 em 12x,
     * gera os 12 Receivable com datas D+30, D+60, ... D+360.
     * MDR de 2,2% distribuído por parcela.
     */
    public List<Receivable> split(
        ApprovedTransaction txn,
        BigDecimal mdrRate,
        LocalDate baseDate
    ) { /* ... */ }
}
```

### Parte 2 — Simulação da Integração com Registradoras

Implemente `ReceivableRegistrar` que simula o registro de recebíveis na CIP:

```java
public class ReceivableRegistrar {

    /**
     * Registra uma lista de recebíveis na registradora.
     * Em produção, isso seria uma chamada HTTP à API da CIP/CERC.
     * Para o exercício, persiste localmente e gera ID de registro.
     *
     * Regra: deve registrar em até 1 dia útil após a transação.
     */
    public RegistrationResult register(List<Receivable> receivables) { /* ... */ }

    /**
     * Consulta agenda de recebíveis de um merchant em um período.
     * Retorna total por data de liquidação e situação (livre, antecipado, garantia).
     */
    public AgendaReport queryAgenda(
        String merchantId,
        LocalDate from,
        LocalDate to
    ) { /* ... */ }
}
```

### Parte 3 — Cálculo da Antecipação e Documentação

Com a agenda de recebíveis implementada, calcule o valor de uma antecipação:

Dado que um merchant tem R$ 120.000 em recebíveis distribuídos nos próximos 12 meses (D+30 a D+360), com taxa de antecipação de 1,8% a.m., calcule:

1. Valor presente de cada parcela futura
2. Total antecipado líquido
3. Custo total da antecipação (quanto o merchant paga)

Escreva `anticipation-analysis.md` explicando:
- Como o parcelamento sem juros cria a infraestrutura de recebíveis
- Por que emissores investiram mais em crédito após o teto de interchange de 0,5% em débito
- Como o Open Finance muda a disputa pelo produto de antecipação (banco vs adquirente)

## Critérios de Avaliação

- [ ] `InstallmentSplitter` gera as parcelas corretas com datas e valores precisos
- [ ] MDR distribuído proporcionalmente entre as parcelas
- [ ] `ReceivableRegistrar` persiste e consulta a agenda com totais corretos
- [ ] Cálculo de antecipação matematicamente correto (valor presente)
- [ ] `anticipation-analysis.md` com pelo menos 300 palavras, tecnicamente preciso
- [ ] Testes cobrindo o `InstallmentSplitter` com casos de parcelamento 2x, 6x e 12x

## Dicas

- O parcelamento em 12x com MDR de 2,2% custa ao lojista ~R$ 26,40 por R$ 1.200 — mas o custo real é maior porque o dinheiro demora 12 meses para chegar.
- As datas de liquidação seguem calendário bancário: use `BusinessDayCalculator` com feriados ANBIMA.
- Em produção, o registro nas registradoras é por API REST com autenticação OAuth2 — o timing importa para que o recebível fique disponível para portabilidade.
- A antecipação é um produto financeiro, não apenas tecnológico — o spread que o adquirente cobra é a diferença entre o custo de capital dele e a taxa cobrada do merchant.
