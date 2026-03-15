# Semana 25 — Mercado Brasileiro Profundo

## Por que esta semana é diferente das anteriores?

Nas semanas 1-24 você aprendeu o protocolo. Agora começa a especialização real. O mercado brasileiro de pagamentos tem particularidades que não existem em nenhum outro país do mundo — e dominar essas particularidades é o que separa um implementador de um **especialista referência**.

---

## 1. A Regulação que Moldou Tudo: Lei 12.865/2013

A Lei 12.865 criou o marco regulatório dos arranjos de pagamento no Brasil. Entender essa lei é entender por que o mercado funciona como funciona.

### 1.1 O que a lei fez

Antes de 2013: Visa e Mastercard operavam sem regulação específica. Cielo e Rede tinham exclusividade de bandeiras — Cielo só aceitava Visa, Rede só aceitava Mastercard. O lojista não tinha escolha.

Depois de 2013:
- BACEN passou a regular e supervisionar arranjos de pagamento
- Exclusividade entre adquirentes e bandeiras foi proibida
- Credenciamento cruzado se tornou obrigatório
- Novos entrantes (Stone, PagSeguro, Getnet) puderam competir
- Emissores foram separados regulatoriamente de adquirentes

**Impacto técnico direto:** O mesmo terminal passou a precisar suportar múltiplas bandeiras. O switch do adquirente precisa rotear por bandeira, não apenas por BIN isolado.

### 1.2 Arranjos de Pagamento — O que são

Um **arranjo de pagamento** é o conjunto de regras, procedimentos e infraestrutura que permite transferência de recursos entre pagadores e recebedores. O BACEN autoriza e supervisiona cada arranjo.

```
Arranjos autorizados no Brasil (principais):
  Visa (Visa do Brasil Arranjos de Pagamento Ltda)
  Mastercard (Mastercard Brasil Soluções de Pagamento Ltda)
  Elo (Elo Serviços S.A.)
  American Express (Amex do Brasil)
  Hipercard (Hipercard Administradora de Cartões)
  PIX (arranjo do próprio BACEN — Banco Central)
```

### 1.3 Interoperabilidade obrigatória

O BACEN exige interoperabilidade. Na prática:
- Todo credenciador deve aceitar todos os arranjos autorizados
- Todo emissor deve participar de pelo menos um arranjo
- Portabilidade de domicílio bancário é obrigatória

---

## 2. Elo — A Bandeira Brasileira

### 2.1 História e estrutura

Elo foi criada em 2011 por Bradesco, Caixa e Banco do Brasil para ter uma bandeira nacional, sem dependência de Visa e Mastercard. Processa hoje mais de 2 bilhões de transações/ano.

```
Estrutura societária simplificada:
  Bradesco → participa via Elopar
  Caixa Econômica Federal → participa via Elopar
  Banco do Brasil → participa via Elopar
  Elopar controla Elo Serviços S.A.
```

### 2.2 Como o Elo difere no ISO 8583

O Elo usa especificação própria baseada em ISO 8583, com extensões relevantes:

| Campo | Visa/Master | Elo | Diferença |
|-------|-------------|-----|-----------|
| DE 48 | Subelementos proprietários | Subelementos próprios Elo | Layout diferente |
| DE 60 | Pouco usado | Central para parcelamento | Campos de parcelas Elo |
| DE 62 | Visa-specific | Elo-specific | Layout diferente |
| Response codes | Padrão ISO | Maioria igual + 8xx próprios | Alguns códigos específicos |
| Parcelamento | DE 48 principalmente | DE 60 principalmente | Especificação Elo |

### 2.3 BINs Elo e roteamento

```
Faixas BIN Elo (exemplos representativos):
  506699 - 506778  (Bradesco)
  509000 - 509099  (Caixa)
  636368           (BB)
  627780           (Hiper/Elo co-branded)

Roteamento no switch:
  BinRange elo = loadEloRanges(); // tabela mantida pela Elo Serviços
  if (elo.contains(bin)) {
      route = resolveEloRoute(merchantAcquirer); // ELO_CIELO ou ELO_REDE etc.
  }
```

### 2.4 Elo e o on-us

Elo tem a maior taxa de transações on-us do mercado porque:
- Bradesco emite Elo → Cielo (controlada pelo Bradesco) adquire → rota on-us
- Caixa emite Elo → terminal próprio Caixa → rota on-us
- BB emite Elo → terminal BB → rota on-us

Menor custo de interchange para essas instituições e menor latência.

---

## 3. Teto de Interchange — A Decisão do BACEN

### 3.1 A regulação

O BACEN impôs teto de interchange em débito:
- **Débito:** máximo 0,5% (Resolução BCB nº 150/2021)
- **Pré-pago:** máximo 0,5%
- **Crédito à vista:** sem teto definido regulatório ainda (mercado pratica ~1,5%)
- **Crédito parcelado:** sem teto (mercado pratica 1,8–2,2% dependendo das parcelas)

### 3.2 Impacto no ecossistema

Antes do teto, o emissor recebia ~1,5–2% de interchange em débito. Com o teto de 0,5%:
- Emissores reduziram benefícios e cashback de cartões de débito
- Acelerou a adoção do PIX (sem custo de interchange para o lojista)
- MDR de débito caiu junto (~1,0–1,5% hoje)
- Emissores investiram mais em crédito (sem teto)

**Impacto técnico:** O switch precisa manter tabelas de interchange atualizadas por bandeira, produto e tipo de transação para cálculo correto no settlement. Tabelas mudam periodicamente.

---

## 4. Antecipação de Recebíveis — O Produto Financeiro Mais Importante

### 4.1 O problema que resolve

Lojista vende R$ 100.000 em 12x sem juros em outubro. Receberia:
- Novembro: R$ 8.333
- Dezembro: R$ 8.333
- ... outubro do ano seguinte: R$ 8.333

Com antecipação, recebe tudo hoje com desconto (taxa de antecipação).

### 4.2 Como funciona tecnicamente

```
1. Registro no adquirente:
   Transação: R$ 100.000, 12x, MerchantID=42, RRN=123456789012
   Parcelas: 12 registros de R$ 8.333, datas D+30 a D+360

2. Registro na Registradora (CIP/CERC/TAG):
   Registra o recebível: MerchantID + Valor + Data + AdquirenteID
   Permite portabilidade: Merchant designa banco de domicílio

3. Antecipação:
   Merchant solicita ao adquirente (ou banco)
   Taxa exemplo: 1,8% a.m.
   R$ 100.000 em 12x ≈ R$ 89.000 antecipado hoje
   Adquirente passa a ter direito sobre as parcelas futuras
```

### 4.3 Registradoras e portabilidade

```
Registradoras autorizadas pelo BACEN:
  CIP (Câmara Interbancária de Pagamentos) — operada pela B3
  TAG Tecnologia em Pagamentos
  CERC (Central de Recebíveis)

Portabilidade de domicílio bancário:
  Merchant pode designar banco diferente do adquirente para receber
  Adquirente DEVE respeitar a designação
  Adquirente NÃO pode reter recebíveis como garantia sem autorização expressa
  Resolução BCB 96/2021 regulamenta o processo
```

**Impacto técnico no switch:** O sistema de clearing precisa registrar cada parcela individualmente nas registradoras via API. Integração com CIP/CERC/TAG é obrigatória para adquirentes de grande porte.

---

## 5. Open Finance — O Mercado Convergindo

### 5.1 O que é Open Finance no Brasil

Open Finance é a regulação do BACEN (Resolução Conjunta BCB/CMN nº 1/2020) que obriga instituições financeiras a compartilharem dados de clientes (com consentimento) e a oferecerem APIs padronizadas.

```
Fases de implementação:
  Fase 1 (fev/2021): Dados públicos (produtos, tarifas)
  Fase 2 (ago/2021): Dados do cliente (contas, cartões, crédito)
  Fase 3 (out/2021): Iniciação de pagamento (PIX via Open Finance)
  Fase 4 (dez/2021): Expansão (câmbio, investimentos, seguros)
```

### 5.2 Impacto em pagamentos com cartão

Open Finance não substitui o ISO 8583, mas cria nova camada sobre ele:

```
Antes:
  App do banco → ISO 8583 → Bandeira → Emissor

Com Open Finance:
  App de terceiro (TPP) → API BACEN padronizada → ISO 8583 → Emissor
                               ↑
                    Iniciador de Pagamento (PISP)
```

Novas entidades:
- **TPP (Third Party Provider):** empresa que acessa dados/pagamentos via Open Finance
- **PISP (Payment Initiation Service Provider):** inicia transações PIX
- **AISP (Account Information Service Provider):** acessa dados de conta

### 5.3 Por que o especialista em ISO 8583 precisa saber disso

Porque cartões e Open Finance convergem:
- Cartão virtual disparado via API Open Finance
- Iniciação de débito em conta que passa pelo mesmo switch de autenticação
- Reconciliação que mistura PIX, Open Finance e cartão no mesmo arquivo de settlement

---

## 6. DREX — O Real Digital

### 6.1 O que é

DREX é a CBDC (Central Bank Digital Currency) do Brasil, desenvolvida pelo BACEN em blockchain privada (Hyperledger Besu). Fase piloto em 2024-2025 com instituições selecionadas.

### 6.2 Diferença técnica fundamental

| Aspecto | Cartão + ISO 8583 | DREX |
|---------|-------------------|------|
| Protocolo | ISO 8583 | Smart contracts (EVM) |
| Liquidação | D+1/D+2 | Imediata (on-chain) |
| Intermediários | Adquirente, bandeira, emissor | Muito reduzidos |
| Clearing | Arquivo batch | Automático via smart contract |
| Reversibilidade | Reversal ISO 8583 | Smart contract cancelamento |

### 6.3 O que isso não muda no médio prazo

DREX não vai eliminar cartões nos próximos anos porque:
- Bandeiras internacionais (Visa, Master) não migram para DREX
- Cartões físicos continuam sendo ISO 8583
- Infraestrutura de terminais não muda do dia para a noite
- Portadores internacionais usam cartão, não DREX

O especialista em ISO 8583 continua sendo necessário por mais de uma década.

---

## 7. Sub-adquirência Profunda

### 7.1 Regulação específica

O BACEN publicou a Resolução BCB nº 80/2021 regulando sub-adquirentes. Pontos técnicos:

```
Sub-adquirente deve:
  - Ter autorização BACEN se processar > R$ 500 milhões/ano
  - Monitorar chargeback ratio de cada merchant sub-credenciado
  - Ser responsável por falhas do merchant perante o adquirente master
  - Registrar recebíveis de seus merchants nas registradoras

Adquirente master deve:
  - Monitorar sub-adquirentes como se fossem merchants diretos
  - Reportar dados do sub-adquirente ao BACEN
  - Responder por fraudes do sub-adquirente
```

### 7.2 Impacto no ISO 8583

```
Transação via sub-adquirente (ex: iFood Pagamentos):
  DE 42 (Merchant ID): ID do sub-adquirente (não do restaurante real)
  DE 43 (Merchant Name): "IFOOD*RESTAURANTE XYZ"
  DE 48: Pode carregar dados do merchant real para fins de relatório

Problema: Adquirente master vê DE 42 como sub-adquirente.
  Para fins de chargeback e fraude, quem responde é o sub-adquirente,
  que por sua vez cobra do merchant real.
```

---

## Resumo da Semana

| Tema | O que você deve dominar |
|------|------------------------|
| Lei 12.865 | Marco regulatório, proibição de exclusividade, abertura do mercado |
| Arranjos | O BACEN autoriza cada arranjo, todos devem ser interoperáveis |
| Elo | Bandeira nacional, ISO 8583 com extensões próprias, alta taxa on-us |
| Interchange | Teto 0,5% débito, impacto em emissores e MDR, tabelas no switch |
| Recebíveis | Parcelas registradas em CIP/CERC/TAG, portabilidade obrigatória |
| Open Finance | TPP, PISP, nova camada sobre ISO 8583, convergência com PIX |
| DREX | CBDC em piloto, não usa ISO 8583, não substitui cartões no médio prazo |
| Sub-adquirência | Regulação BCB 80/2021, responsabilidade do adquirente master |
