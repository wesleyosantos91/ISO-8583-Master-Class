# Semana 27 — PCI-DSS: Como Certificar um Sistema de Pagamentos

## Por que PCI-DSS é essencial para o especialista?

Qualquer sistema que armazene, processe ou transmita dados de cartão está sujeito ao PCI-DSS. Como especialista em ISO 8583, você vai desenvolver e arquitetar sistemas que fazem exatamente isso — e precisa saber não apenas o que é obrigatório, mas **como projetar o sistema para ser certificável desde o início**.

---

## 1. O que é PCI-DSS

**PCI-DSS (Payment Card Industry Data Security Standard)** é um conjunto de requisitos de segurança criado pelo **PCI SSC (Security Standards Council)**, formado por Visa, Mastercard, Amex, Discover e JCB.

Versão atual: **PCI-DSS 4.0** (lançada em março/2022, v4.0.1 em junho/2024).

```
Quem deve seguir PCI-DSS:
  - Merchants (lojistas que aceitam cartão)
  - Adquirentes
  - Processadoras
  - Gateways de pagamento
  - Provedores de serviço que armazenam/processam/transmitem dados de cartão
```

---

## 2. Dados que Estão no Escopo

### 2.1 Cardholder Data (CHD) — pode armazenar com proteção

| Dado | Campo ISO | Pode armazenar? | Deve proteger? |
|------|-----------|----------------|----------------|
| PAN (número do cartão) | DE 2 | Sim, mascarado | Sim (criptografia ou tokenização) |
| Cardholder name | DE 45 | Sim | Sim |
| Expiration date | DE 14 | Sim | Sim |
| Service code | - | Sim | Sim |

### 2.2 Sensitive Authentication Data (SAD) — NUNCA armazenar após autorização

| Dado | Campo ISO | Pode armazenar? |
|------|-----------|----------------|
| Full magnetic stripe (track data) | DE 35 / DE 36 | **NUNCA** |
| CAV2/CVC2/CVV2/CID (código de segurança) | DE 48 subelemento | **NUNCA** |
| PIN / PIN block | DE 52 | **NUNCA** |
| ARQC / criptogramas EMV | DE 55 | Somente até autorização |

**Regra de ouro para o switch:** Após receber a resposta de autorização (`0110`/`0210`), qualquer SAD nos logs ou banco de dados deve ser eliminada. Jamais persista DE 35, DE 52 ou CVV2.

```java
// ERRADO — nunca faça isso
log.info("Transaction: PAN={} Track2={} CVV={}", msg.getString(2), msg.getString(35), cvv);

// CORRETO
log.info("Transaction: PAN={} STAN={} RC={}",
    PANMasker.mask(msg.getString(2)), msg.getString(11), msg.getString(39));
```

---

## 3. Os 12 Requisitos PCI-DSS 4.0

### Requisito 1 — Controles de rede
Instalar e manter controles de segurança de rede.

**Para o switch:**
- Segmentação de rede: servidor ISO 8583 em VLAN separada
- Firewall entre zona de pagamentos e outros sistemas
- Whitelist de IPs permitidos para conexões de adquirentes/bandeiras

### Requisito 2 — Configurações seguras
Aplicar configurações seguras em todos os componentes do sistema.

**Para o switch:**
- Desabilitar serviços e portas não utilizadas no servidor jPOS
- Alterar senhas padrão
- Inventário de todos os componentes no CDE (Cardholder Data Environment)

### Requisito 3 — Proteção de dados armazenados
Proteger dados de conta armazenados.

**Para o switch:**
```java
// PAN deve ser armazenado truncado ou mascarado
String maskedPan = pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4);

// Se precisar armazenar o PAN completo: use tokenização ou criptografia AES-256
String encryptedPan = aesEncrypt(pan, dataEncryptionKey);

// NUNCA: armazenar track data, CVV, PIN mesmo após autorização
```

### Requisito 4 — Proteger dados em trânsito
Proteger dados do titular do cartão transmitidos em redes públicas.

**Para o switch:**
- TLS 1.2+ em todas as conexões (adquirente → bandeira, switch → emissor)
- Certificados válidos e atualizados
- Sem SSL 3.0, TLS 1.0, TLS 1.1 (todos inseguros)

### Requisito 5 — Proteção contra malware
Proteger todos os sistemas e redes de softwares maliciosos.

**Para o switch:**
- Antivírus nos servidores (se rodar em SO propósito geral)
- Monitoramento de integridade de arquivos (FIM) em binários críticos

### Requisito 6 — Desenvolvimento seguro
Desenvolver e manter sistemas e softwares seguros.

**Para o switch — o mais técnico para desenvolvedores:**
```
Exige:
  - Treinamento em desenvolvimento seguro para toda a equipe
  - Revisão de código para vulnerabilidades (OWASP Top 10)
  - SAST (Static Analysis) e DAST (Dynamic Analysis) automatizados
  - Processo de patching: críticos em 1 mês, outros em 3 meses
  - Separação de ambientes: DEV != STAGE != PROD
  - Dados de produção (PANs reais) NUNCA em ambientes de teste
```

### Requisito 7 — Controle de acesso
Restringir acesso a componentes e dados por necessidade de negócio.

**Para o switch:**
```
Princípio do menor privilégio:
  - Conta de serviço do jPOS: somente permissões necessárias
  - DBA: somente acesso às tabelas de transação (não às de usuários do sistema)
  - Desenvolvedor: sem acesso à produção com dados reais
  - Acesso à produção: somente via jump server, com MFA
```

### Requisito 8 — Identificação e autenticação
Identificar usuários e autenticar o acesso a componentes do sistema.

**Para o switch:**
- MFA obrigatório para acesso a sistemas no CDE
- Contas individuais (sem contas compartilhadas)
- Senha forte mínima: 12 caracteres, complexidade
- Expiração de sessão após inatividade

### Requisito 9 — Segurança física
Restringir acesso físico a dados do titular do cartão.

Para HSMs e servidores de pagamento:
- Rack em datacenter certificado (ISO 27001 mínimo)
- Controle de acesso físico por biometria/crachá
- Logs de acesso físico

### Requisito 10 — Logs e monitoramento
Registrar e monitorar todo o acesso a recursos e dados do titular.

**Para o switch:**
```java
// Logs de auditoria: WHO did WHAT to WHICH data WHEN from WHERE
AuditLogger.log(AuditEvent.builder()
    .userId(currentUser)
    .action("VIEW_TRANSACTION")
    .resource("TXN:" + rrn)
    .ipAddress(remoteIp)
    .timestamp(Instant.now())
    .maskedPan(PANMasker.mask(pan))
    .build());

// Retenção: mínimo 12 meses (3 meses online, 9 meses arquivo)
```

### Requisito 11 — Testes de segurança
Testar a segurança dos sistemas e redes regularmente.

```
Obrigatório:
  - Scan de vulnerabilidades: trimestral (ASV aprovado pelo PCI SSC)
  - Penetration test: anual + após mudanças significativas
  - Teste de segmentação de rede: anual
  - Monitoramento de IDS/IPS: contínuo
```

### Requisito 12 — Políticas e programas de segurança
Manter uma política de segurança da informação.

```
Documentos obrigatórios:
  - Política de segurança da informação
  - Plano de resposta a incidentes (com simulação anual)
  - Inventário de ativos no CDE
  - Análise de risco anual
  - Programa de conscientização dos funcionários
```

---

## 4. Scoping — O que Entra no CDE

O **CDE (Cardholder Data Environment)** é o ambiente no escopo do PCI. Quanto menor o CDE, menor o custo e esforço da certificação.

### 4.1 O que está no CDE

```
DENTRO do CDE:
  ✓ Servidor jPOS (processa mensagens com PAN)
  ✓ Banco de dados de transações (guarda PAN mascarado ou token)
  ✓ HSM (processa PIN)
  ✓ Rede entre esses componentes
  ✓ Sistemas de log que capturam dados de cartão
  ✓ Servidores de backup desses sistemas

FORA do CDE (se isolado corretamente):
  ✗ Sistema de RH
  ✗ Sistema de BI/Analytics (se recebe apenas dados mascarados)
  ✗ Ambiente de desenvolvimento (se não usa PANs reais)
  ✗ Monitoramento geral (se não tem acesso a dados de cartão)
```

### 4.2 Como reduzir o escopo

```
Estratégias:
  1. Tokenização: substituir PAN por token antes de passar para outros sistemas
     → BI recebe token, não PAN → BI sai do escopo

  2. Truncation: armazenar apenas últimos 4 dígitos
     → Relatórios de fraude usam PAN mascarado → saem do escopo

  3. Segmentação de rede: CDE em VLAN isolada
     → Outros sistemas não têm rota para o CDE

  4. P2PE (Point-to-Point Encryption): terminal criptografa antes de transmitir
     → Gateway de pagamento nunca vê o PAN em claro → sai do escopo
```

---

## 5. SAQ vs ROC — Qual se Aplica?

### 5.1 SAQ (Self-Assessment Questionnaire)

Para organizações menores, o PCI permite auto-avaliação:

| SAQ | Para quem | Premissas |
|-----|-----------|-----------|
| SAQ A | E-commerce que terceiriza tudo | Merchant nunca vê PAN, usa iframe do gateway |
| SAQ A-EP | E-commerce com página própria | Usa JS do gateway, servidor próprio |
| SAQ B | Terminais físicos, sem armazenamento | Transações via linha discada/IP para adquirente |
| SAQ B-IP | Terminais IP | Terminais certificados PTS, sem armazenamento |
| SAQ C | Sistemas com conexão à internet | Não armazena PAN |
| SAQ C-VT | Terminal virtual (browser) | Sem sistema de pagamento local |
| SAQ D | Todos os outros | Armazena, processa ou transmite PAN |

**Para um switch/adquirente/processadora: SAQ D ou ROC.**

### 5.2 ROC (Report on Compliance)

Para grandes volumes ou mandato das bandeiras, um QSA (Qualified Security Assessor) deve fazer auditoria completa e produzir o ROC.

```
Quem DEVE ter ROC (não pode fazer SAQ):
  - Adquirentes
  - Processadoras
  - Gateways que armazenam PANs
  - Merchants nível 1 (> 6 milhões de transações/ano)
  - Qualquer entidade mandada pelas bandeiras
```

---

## 6. Checklist Técnico para um Switch PCI-Compliant

```
Código:
  [ ] PAN nunca logado sem mascaramento
  [ ] Track data (DE 35/36) nunca armazenado após autorização
  [ ] CVV2 nunca armazenado
  [ ] PIN block nunca logado
  [ ] ARQC/DE 55 removido de logs permanentes
  [ ] TLS 1.2+ em todas as conexões
  [ ] Criptografia AES-256 para PANs em repouso

Infraestrutura:
  [ ] Segmentação de rede (VLAN CDE separada)
  [ ] Firewall com whitelist de IPs
  [ ] FIM (File Integrity Monitoring) em binários
  [ ] Logs de auditoria com retenção de 12 meses
  [ ] MFA para acesso a produção

Processo:
  [ ] Pentest anual
  [ ] Scan de vulnerabilidades trimestral
  [ ] Plano de resposta a incidentes
  [ ] Treinamento anual da equipe
  [ ] Inventário de ativos atualizado
```

---

## Resumo da Semana

| Conceito | O que você deve saber |
|----------|-----------------------|
| SAD vs CHD | SAD nunca armazena; CHD protege com criptografia/tokenização |
| 12 Requisitos | Rede, configuração, dados, trânsito, malware, dev, acesso, auth, físico, logs, testes, políticas |
| CDE | Minimizar escopo usando tokenização, truncation e segmentação |
| SAQ vs ROC | Switch/adquirente → ROC com QSA |
| PAN Masking | First 6 + last 4 no mínimo |
| TLS | 1.2+ obrigatório, sem SSL/TLS antigos |
| Pentest | Anual + após mudanças significativas |
