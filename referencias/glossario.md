# Glossário Completo — Especialista em Pagamentos

> Termos ordenados por tema. Domine TODOS para se posicionar como especialista.

---

## Protocolo ISO 8583

| Termo | Definição |
|-------|-----------|
| **ISO 8583** | Padrão internacional de mensageria para transações financeiras com cartões. Define estrutura de mensagens entre terminais, adquirentes, bandeiras e emissores. |
| **MTI** | Message Type Indicator. 4 dígitos que identificam versão, classe, função e origem da mensagem. |
| **Bitmap** | Sequência de 64 ou 128 bits indicando quais Data Elements estão presentes na mensagem. |
| **Data Element (DE)** | Campo individual da mensagem. Cada DE tem número (1-128), tipo, tamanho e formato definidos. |
| **Primary Bitmap** | Primeiros 64 bits. Cobre DE 1 a DE 64. Sempre presente. |
| **Secondary Bitmap** | Bits 65-128. Presente quando bit 1 da primary = 1. |
| **PAN** | Primary Account Number (DE 2). Número do cartão, 13-19 dígitos. |
| **STAN** | Systems Trace Audit Number (DE 11). 6 dígitos, identifica a transação para correlação. |
| **RRN** | Retrieval Reference Number (DE 37). 12 caracteres, referência para rastreio de negócio. |
| **Processing Code** | DE 3. 6 dígitos: tipo de transação + conta origem + conta destino. |
| **Response Code** | DE 39. 2 caracteres indicando resultado (00=aprovado, 05=negado, etc.). |
| **Authorization ID** | DE 38. Código de 6 caracteres gerado pelo emissor quando aprova. |
| **POS Entry Mode** | DE 22. Como o cartão foi lido (chip, tarja, contactless, manual, e-commerce). |
| **LLVAR** | Campo de tamanho variável com prefixo de 2 dígitos (máx 99 chars). |
| **LLLVAR** | Campo de tamanho variável com prefixo de 3 dígitos (máx 999 chars). |
| **BCD** | Binary Coded Decimal. 2 dígitos por byte. Compacta campos numéricos. |
| **Packager** | Componente que define como cada campo é serializado/desserializado (encoding, tamanho, tipo). |

## Atores do Ecossistema

| Termo | Definição |
|-------|-----------|
| **Portador (Cardholder)** | Pessoa que possui e usa o cartão. Relação contratual com o emissor. |
| **Merchant (Estabelecimento)** | Empresa que aceita pagamento com cartão. Relação contratual com o adquirente. |
| **Adquirente (Acquirer)** | Instituição que credencia merchants e captura transações. Exemplos: Cielo, Rede, Stone. |
| **Emissor (Issuer)** | Instituição que emite o cartão e aprova/nega transações. Exemplos: Itaú, Nubank. |
| **Bandeira (Card Network/Scheme)** | Define regras, opera o switch central e processa clearing. Exemplos: Visa, Mastercard, Elo. |
| **Processadora (Processor)** | Empresa que efetivamente processa as mensagens ISO 8583. Pode ser terceirizada. |
| **Sub-adquirente (Facilitador)** | Opera sob um adquirente, facilita acesso a pequenos merchants. Ex: PagSeguro, Mercado Pago. |

## Tipos de Transação e Fluxo

| Termo | Definição |
|-------|-----------|
| **Authorization** | Processo real-time de solicitar aprovação ao emissor. Reserva limite sem mover dinheiro. |
| **Capture** | Confirmação de que a venda foi efetivada. Em SMS é automática, em DMS é separada. |
| **Clearing** | Troca de informações financeiras em batch (D+1) entre adquirente, bandeira e emissor. |
| **Settlement** | Movimentação real de dinheiro entre as partes. Emissor paga bandeira, bandeira paga adquirente. |
| **Reversal** | Desfazimento de transação. Usado após timeout, erro, ou cancelamento. MTI 0400. |
| **Advice** | Notificação. O remetente já decidiu e está informando. MTI xx2x. |
| **Stand-In (STIP)** | Bandeira decide quando emissor está indisponível, usando parâmetros pré-definidos. |
| **Chargeback (Contestação)** | Portador contesta uma transação. Emissor solicita estorno ao adquirente. |
| **Force Post** | Clearing enviado sem autorização prévia. Exceção, pode gerar multas. |
| **Auth Adjustment** | Alteração do valor autorizado (ex: gorjeta em restaurante). |
| **Pre-authorization** | Reserva de valor sem captura imediata. Comum em hotéis/locadoras. DE25=06. |
| **Incremental Authorization** | Aumento do valor pré-autorizado. Nova auth referenciando a original. |
| **Partial Approval** | Emissor aprova valor menor que o solicitado. DE39=10. |
| **Balance Inquiry** | Consulta de saldo via ATM/POS. Processing Code 30xxxx. |

## Topologias

| Termo | Definição |
|-------|-----------|
| **On-Us** | Adquirente e emissor da mesma instituição. Mensagem não precisa ir à bandeira. |
| **Off-Us** | Adquirente e emissor de instituições diferentes. Mensagem passa pela bandeira. |
| **Not-On-Us** | Switch identifica que NÃO é on-us e roteia para a bandeira. |
| **BIN/IIN** | Bank/Issuer Identification Number. Primeiros 6-8 dígitos do PAN identificam emissor/bandeira. |
| **BIN Expansion** | Migração de 6 para 8 dígitos no BIN. Permite identificar mais emissores/produtos. |
| **Dual Message System (DMS)** | Auth e captura em mensagens separadas. Usado para crédito. |
| **Single Message System (SMS)** | Auth e captura na mesma mensagem. Usado para débito. |

## Segurança

| Termo | Definição |
|-------|-----------|
| **HSM** | Hardware Security Module. Equipamento dedicado para operações criptográficas. |
| **PIN Block** | PIN criptografado para transmissão. Formato 0: PIN XOR PAN, depois 3DES. |
| **DUKPT** | Derived Unique Key Per Transaction. Cada transação usa chave diferente. |
| **LMK** | Local Master Key. Dentro do HSM, nunca sai. |
| **ZMK** | Zone Master Key. Compartilhada entre instituições para troca de session keys. |
| **ZPK** | Zone PIN Key. Criptografa PIN blocks em trânsito. |
| **PIN Translation** | Re-criptografia do PIN de uma ZPK para outra, dentro do HSM. |
| **Key Ceremony** | Processo formal de injeção de chaves no HSM com custódios. |
| **ARQC** | Application Request Cryptogram. Gerado pelo chip, validado pelo emissor. |
| **ARPC** | Application Response Cryptogram. Gerado pelo emissor, validado pelo chip. |
| **TC** | Transaction Certificate. Prova final de transação aprovada pelo chip. |
| **CVV/CVC** | Card Verification Value/Code. 3-4 dígitos para validação CNP. |
| **3DS** | 3-D Secure. Protocolo de autenticação para e-commerce. |
| **PCI-DSS** | Payment Card Industry Data Security Standard. Requisitos de segurança. |

## Tokenização e Digital

| Termo | Definição |
|-------|-----------|
| **FPAN** | Funding PAN. Número real do cartão. |
| **DPAN** | Digital PAN. Token que substitui o FPAN. |
| **VTS** | Visa Token Service. Serviço de tokenização network-level da Visa. |
| **MDES** | Mastercard Digital Enablement Service. Tokenização da Mastercard. |
| **COF** | Credential on File. Framework para merchant que armazena dados do cartão. |
| **CIT** | Cardholder Initiated Transaction. Transação com portador presente/ativo. |
| **MIT** | Merchant Initiated Transaction. Cobrança recorrente sem portador. |
| **Network Transaction ID** | Identificador da bandeira vinculando transações COF. |

## Economia de Pagamentos

| Termo | Definição |
|-------|-----------|
| **Interchange Fee** | Taxa que o adquirente paga ao emissor por cada transação. Definida pela bandeira. |
| **MDR** | Merchant Discount Rate. Taxa total cobrada do lojista. MDR = interchange + network fee + margem. |
| **Assessment Fee** | Taxa cobrada pela bandeira por transação. |
| **Antecipação de Recebíveis** | Adiantamento ao lojista de vendas parceladas futuras, com desconto. |
| **Domicílio Bancário** | Conta bancária onde o lojista recebe os pagamentos. |
| **Registradora** | Entidade que registra recebíveis. CIP, TAG, CERC. |
| **MCC** | Merchant Category Code. Classifica o tipo de negócio (DE 18). |
| **Parcelamento Lojista** | Sem juros. Lojista absorve custo financeiro. Parcelas em D+30/60/90. |
| **Parcelamento Emissor** | Com juros. Emissor financia. Portador paga mais. |
| **Arranjo de Pagamento** | Regras definidas pela bandeira e reguladas pelo BACEN. |

## Infraestrutura e Redes

| Termo | Definição |
|-------|-----------|
| **Base I** | Rede Visa para autorização (real-time). |
| **Base II** | Rede Visa para clearing (batch). |
| **VisaNet** | Infraestrutura de processamento da Visa. |
| **Banknet** | Rede de switch da Mastercard. |
| **MIP** | Mastercard Interface Processor. Ponto de conexão. |
| **TC File** | Transaction Clearing file. Formato Visa para clearing. |
| **IPM** | Integrated Products Message. Formato Mastercard para clearing. |
| **ISO 20022** | Padrão moderno XML/JSON para mensageria financeira. Futuro de clearing. |

## jPOS

| Termo | Definição |
|-------|-----------|
| **Q2** | Container/runtime do jPOS. Gerencia lifecycle dos componentes. |
| **TransactionManager** | Orquestrador que processa transações via pipeline de participants. |
| **Participant** | Componente que executa uma etapa do processamento (prepare/commit/abort). |
| **GroupSelector** | Participant que decide qual grupo de participants executar. |
| **Context** | Objeto compartilhado entre participants para passar dados. |
| **QMUX** | Multiplexador de mensagens. Gerencia correlação request/response. |
| **QServer** | Servidor TCP que recebe conexões ISO 8583. |
| **GenericPackager** | Packager configurável via XML para serializar/desserializar mensagens. |
| **ISOMsg** | Objeto Java que representa uma mensagem ISO 8583. |
| **ISOChannel** | Abstração de canal TCP com framing (header de tamanho + payload). |
