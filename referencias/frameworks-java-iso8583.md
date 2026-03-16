# Frameworks Java para ISO 8583 — Concorrentes e Alternativas ao jPOS

> Guia comparativo dos principais frameworks Java para processamento de mensagens ISO 8583.
> Inclui arquitetura, exemplos de código e quando usar cada um.

---

## Visão Geral

O ecossistema Java para ISO 8583 tem **7 frameworks principais**, cada um com uma filosofia diferente:

```
┌──────────────────────────────────────────────────────────────────────────┐
│                      Ecossistema Java ISO 8583                           │
│                                                                          │
│  ┌──────────┐  ┌───────────┐  ┌───────────────┐  ┌──────────────┐      │
│  │  jPOS     │  │  j8583    │  │ jreactive-8583│  │ imohsenb     │      │
│  │ (full     │  │ (parsing  │  │ (Netty +      │  │ ISO8583      │      │
│  │  stack)   │  │  only)    │  │  j8583)       │  │ (builder)    │      │
│  └──────────┘  └───────────┘  └───────────────┘  └──────────────┘      │
│                                                                          │
│  ┌─────────────────────┐  ┌───────────────┐  ┌──────────────────────┐  │
│  │ Apache Camel ISO-8583│  │ jBSBE         │  │ nucleus8583          │  │
│  │ (enterprise via j8583)│  │ (annotations) │  │ (ultra-performance)  │  │
│  └─────────────────────┘  └───────────────┘  └──────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## Matriz Comparativa

| Característica | jPOS | j8583 | jreactive-8583 | imohsenb ISO8583 | Camel ISO-8583 | jBSBE | nucleus8583 |
|---|---|---|---|---|---|---|---|
| **Licença** | AGPL v3 (ou comercial) | Apache 2.0 | Apache 2.0 | MIT | Apache 2.0 | Apache 2.0 | Apache 2.0 |
| **Versão atual** | 2.1.9 | 3.0.1 | 1.5.1 | 1.0.5 | 4.14.x | 0.0.5 | (inativo) |
| **Parse/Pack** | Sim | Sim | Sim (via j8583) | Sim | Sim (via j8583) | Sim (via j8583) | Sim |
| **Networking TCP** | Sim (Channel) | Não | Sim (Netty) | Sim (NIO/SSL) | Sim (Camel routes) | Não | Não |
| **TransactionManager** | Sim (2-phase) | Não | Não | Não | Não (usa Camel EIP) | Não | Não |
| **Correlação (MUX)** | Sim (QMUX) | Não | Sim (auto) | Não | Sim (Camel) | Não | Não |
| **Auto-reconnect** | Sim | N/A | Sim | Sim | Sim | N/A | N/A |
| **SSL/TLS** | Sim | N/A | Sim | Sim | Sim | N/A | N/A |
| **Mascaramento PAN** | Manual | Manual | Automático | Manual | Manual | Automático | Manual |
| **Runtime autônomo** | Sim (Q2) | Não | Não | Não | Sim (Spring Boot) | Não | Não |
| **Annotations** | Não | Não | Não | Não | Não | **Sim** | Não |
| **Peso (JAR)** | ~5 MB | ~100 KB | ~200 KB | ~50 KB | ~100 KB + Camel | ~80 KB | ~60 KB |
| **Curva de aprendizado** | Alta | Baixa | Média | Muito baixa | Média-alta | Baixa | Baixa |
| **Comunidade** | Grande | Média | Pequena-média | Pequena | Grande (Camel) | Mínima | Mínima |
| **Ideal para** | Switches completos | Parsing simples | Microsserviços async | Apps Android/simples | Integração enterprise | Microservices | Ultra-perf (legacy) |

---

## 1. jPOS — O Padrão da Indústria

### Quando usar
- Switches de produção em bancos e processadoras
- Sistemas que precisam de TransactionManager com 2-phase commit
- Quando você precisa de um runtime completo (Q2)

### Licença
**AGPL v3** — Código derivado deve ser open source. Para uso comercial fechado, é necessária licença paga (~USD 1.500/ano). Este é o maior ponto de atenção para empresas.

### Maven
```xml
<dependency>
    <groupId>org.jpos</groupId>
    <artifactId>jpos</artifactId>
    <version>2.1.9</version>
</dependency>
```

### Exemplo: Montar e enviar uma autorização (0200)

```java
import org.jpos.iso.*;
import org.jpos.iso.channel.NACChannel;
import org.jpos.iso.packager.GenericPackager;

public class JPosExample {

    public static void main(String[] args) throws Exception {
        // 1. Configurar packager a partir de XML
        GenericPackager packager = new GenericPackager("cfg/iso87ascii.xml");

        // 2. Montar mensagem 0200 (autorização)
        ISOMsg msg = new ISOMsg();
        msg.setPackager(packager);
        msg.setMTI("0200");
        msg.set(2,  "4532015112830366");        // PAN
        msg.set(3,  "003000");                   // Processing Code (compra crédito)
        msg.set(4,  "000000015000");             // Amount R$ 150,00
        msg.set(7,  "0314143025");               // Transmission Date/Time
        msg.set(11, "123456");                   // STAN
        msg.set(22, "051");                      // POS Entry Mode (chip)
        msg.set(41, "TERM0001");                 // Terminal ID
        msg.set(42, "MERCHANT0000001");          // Merchant ID
        msg.set(49, "986");                      // Currency (BRL)

        // 3. Serializar (pack)
        byte[] packed = msg.pack();
        System.out.println("Packed (" + packed.length + " bytes): " + ISOUtil.hexString(packed));

        // 4. Conectar e enviar via NACChannel
        NACChannel channel = new NACChannel("issuer-host", 8583, packager);
        channel.connect();
        channel.send(msg);

        // 5. Receber resposta (0210)
        ISOMsg response = channel.receive();
        System.out.println("Response MTI: " + response.getMTI());
        System.out.println("Response Code (DE39): " + response.getString(39));
        System.out.println("Auth Code (DE38): " + response.getString(38));

        channel.disconnect();
    }
}
```

### Exemplo: TransactionManager Participant

```java
import org.jpos.transaction.TransactionParticipant;
import org.jpos.transaction.Context;
import java.io.Serializable;

public class ValidateAmount implements TransactionParticipant {

    @Override
    public int prepare(long id, Serializable ctx) {
        Context context = (Context) ctx;
        ISOMsg msg = context.get("REQUEST");

        try {
            long amount = Long.parseLong(msg.getString(4));
            if (amount <= 0 || amount > 99999999999L) {
                context.put("RESPONSE_CODE", "13"); // Invalid amount
                return ABORTED;
            }
            return PREPARED;
        } catch (Exception e) {
            context.put("RESPONSE_CODE", "30"); // Format error
            return ABORTED;
        }
    }

    @Override
    public void commit(long id, Serializable ctx) { }

    @Override
    public void abort(long id, Serializable ctx) { }
}
```

### Pros
- Framework mais completo e maduro do mercado (20+ anos)
- Runtime Q2 gerencia ciclo de vida completo
- TransactionManager com prepare/commit/abort
- Correlação de mensagens via QMUX
- Usado em produção por bancos e processadoras reais
- Comunidade ativa e documentação extensa

### Contras
- **Licença AGPL** — restritiva para uso comercial
- Curva de aprendizado alta (Q2, Space, deploy XML)
- JAR pesado (~5 MB com dependências)
- API verbosa para casos simples
- Configuração via XML pode ser complexa

---

## 2. j8583 — Leve e Focado em Parsing

### Quando usar
- Parse e geração de mensagens ISO 8583 sem networking
- Integração com seu próprio framework de transporte (Spring, Vert.x, etc.)
- Quando a licença Apache 2.0 é requisito

### Licença
**Apache 2.0** — Livre para uso comercial, sem restrições de código fonte.

### Maven
```xml
<dependency>
    <groupId>net.sf.j8583</groupId>
    <artifactId>j8583</artifactId>
    <version>3.0.1</version>
</dependency>
```

### Exemplo: Montar e fazer parse de uma autorização (0200)

```java
import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.IsoType;
import com.solab.iso8583.IsoValue;
import com.solab.iso8583.MessageFactory;
import com.solab.iso8583.parse.ConfigParser;

import java.io.InputStream;

public class J8583Example {

    public static void main(String[] args) throws Exception {
        // 1. Configurar MessageFactory via XML
        MessageFactory<IsoMessage> factory = new MessageFactory<>();
        InputStream config = J8583Example.class.getResourceAsStream("/j8583-config.xml");
        ConfigParser.configureFromClasspathConfig(factory, "j8583-config.xml");
        factory.setUseBinaryMessages(false);   // ASCII encoding
        factory.setAssignDate(true);           // Auto DE7

        // 2. Criar mensagem 0200
        IsoMessage msg = factory.newMessage(0x200);
        msg.setValue(2,  "4532015112830366", IsoType.LLVAR, 19);     // PAN
        msg.setValue(3,  "003000",           IsoType.NUMERIC, 6);    // Processing Code
        msg.setValue(4,  "000000015000",     IsoType.NUMERIC, 12);   // Amount
        msg.setValue(11, "123456",           IsoType.NUMERIC, 6);    // STAN
        msg.setValue(22, "051",              IsoType.NUMERIC, 3);    // POS Entry Mode
        msg.setValue(41, "TERM0001",         IsoType.ALPHA, 8);      // Terminal ID
        msg.setValue(42, "MERCHANT0000001",  IsoType.ALPHA, 15);     // Merchant ID
        msg.setValue(49, "986",              IsoType.NUMERIC, 3);    // Currency BRL

        // 3. Serializar para bytes
        byte[] packed = msg.writeData();
        System.out.println("Packed (" + packed.length + " bytes)");

        // 4. Parse de volta (simula receber do socket)
        IsoMessage parsed = factory.parseMessage(packed, 0);
        System.out.println("MTI: " + Integer.toHexString(parsed.getType()));
        System.out.println("PAN (DE2): " + parsed.getObjectValue(2));
        System.out.println("Amount (DE4): " + parsed.getObjectValue(4));
        System.out.println("STAN (DE11): " + parsed.getObjectValue(11));

        // 5. Criar response (0210) a partir do request
        IsoMessage response = factory.createResponse(parsed);
        response.setValue(38, "ABC123", IsoType.ALPHA, 6);   // Auth Code
        response.setValue(39, "00",     IsoType.ALPHA, 2);   // Approved
        byte[] responsePacked = response.writeData();
        System.out.println("Response packed (" + responsePacked.length + " bytes)");
    }
}
```

### Exemplo: Arquivo de configuração j8583-config.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE j8583-config PUBLIC "-//j8583//DTD//EN" "http://j8583.sourceforge.net/j8583.dtd">
<j8583-config>
    <!-- Template para 0200 (autorização) -->
    <template type="0200">
        <field num="3"  type="NUMERIC"  length="6"/>
        <field num="4"  type="NUMERIC"  length="12"/>
        <field num="7"  type="DATE10"   length="10"/>
        <field num="11" type="NUMERIC"  length="6"/>
        <field num="22" type="NUMERIC"  length="3"/>
        <field num="41" type="ALPHA"    length="8"/>
        <field num="42" type="ALPHA"    length="15"/>
        <field num="49" type="NUMERIC"  length="3"/>
    </template>

    <!-- Template para 0210 (response) -->
    <template type="0210">
        <field num="38" type="ALPHA"    length="6"/>
        <field num="39" type="ALPHA"    length="2"/>
    </template>

    <!-- Parsing guide para 0200 -->
    <parse type="0200">
        <field num="2"  type="LLVAR"    length="19"/>
        <field num="3"  type="NUMERIC"  length="6"/>
        <field num="4"  type="NUMERIC"  length="12"/>
        <field num="7"  type="DATE10"   length="10"/>
        <field num="11" type="NUMERIC"  length="6"/>
        <field num="22" type="NUMERIC"  length="3"/>
        <field num="41" type="ALPHA"    length="8"/>
        <field num="42" type="ALPHA"    length="15"/>
        <field num="49" type="NUMERIC"  length="3"/>
    </parse>
</j8583-config>
```

### Pros
- **Licença Apache 2.0** — sem restrições comerciais
- JAR muito leve (~100 KB), zero dependências
- API simples e direta
- MessageFactory com templates reutilizáveis
- Criação automática de response a partir de request
- Estável e maduro (15+ anos)

### Contras
- **Não tem networking** — você precisa implementar TCP/IP
- Não tem TransactionManager ou pipeline
- Não tem correlação de mensagens (MUX)
- Não tem runtime autônomo
- Menos features que jPOS para cenários complexos

---

## 3. jreactive-8583 — Netty + j8583 (Assíncrono)

### Quando usar
- Microsserviços que precisam de client/server ISO 8583 assíncrono
- Quando performance e non-blocking I/O são prioridade
- Quando quer j8583 para parsing MAS também precisa de networking

### Licença
**Apache 2.0** — Livre para uso comercial.

### Maven
```xml
<dependency>
    <groupId>com.github.kpavlov.jreactive8583</groupId>
    <artifactId>netty-iso8583</artifactId>
    <version>1.5.1</version>
</dependency>
```

### Exemplo: Server ISO 8583 com Netty

```java
import com.github.kpavlov.jreactive8583.server.Iso8583Server;
import com.github.kpavlov.jreactive8583.server.ServerConfiguration;
import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.IsoType;
import com.solab.iso8583.MessageFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class Jreactive8583ServerExample {

    public static void main(String[] args) throws Exception {
        // 1. Configurar MessageFactory do j8583
        MessageFactory<IsoMessage> messageFactory = new MessageFactory<>();
        messageFactory.setUseBinaryMessages(false);
        messageFactory.setAssignDate(true);

        // 2. Configurar e criar server
        ServerConfiguration config = ServerConfiguration.newBuilder()
                .port(8583)
                .addLoggingHandler(true)      // Log automático mascarando PAN
                .replyOnError(true)           // Responde automaticamente em caso de erro
                .build();

        Iso8583Server<IsoMessage> server = new Iso8583Server<>(config, messageFactory);

        // 3. Adicionar handler para processar mensagens
        server.addMessageListener(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                IsoMessage isoMsg = (IsoMessage) msg;
                int mti = isoMsg.getType();

                System.out.println("Received MTI: 0x" + Integer.toHexString(mti));

                if (mti == 0x200) {
                    // Processar autorização
                    IsoMessage response = messageFactory.createResponse(isoMsg);
                    response.setValue(38, "ABC123", IsoType.ALPHA, 6);
                    response.setValue(39, "00",     IsoType.ALPHA, 2);
                    ctx.writeAndFlush(response);
                    System.out.println("Sent 0210 response: approved");
                }

                if (mti == 0x800) {
                    // Echo — respondido automaticamente pelo framework
                    System.out.println("Echo request received (auto-reply)");
                }
            }
        });

        // 4. Iniciar server
        server.init();
        server.start();
        System.out.println("ISO 8583 Server listening on port 8583...");

        // Mantém o server rodando
        Thread.currentThread().join();
    }
}
```

### Exemplo: Client ISO 8583 com Netty

```java
import com.github.kpavlov.jreactive8583.client.Iso8583Client;
import com.github.kpavlov.jreactive8583.client.ClientConfiguration;
import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.IsoType;
import com.solab.iso8583.MessageFactory;

public class Jreactive8583ClientExample {

    public static void main(String[] args) throws Exception {
        // 1. Configurar MessageFactory
        MessageFactory<IsoMessage> messageFactory = new MessageFactory<>();
        messageFactory.setUseBinaryMessages(false);

        // 2. Configurar client com auto-reconnect
        ClientConfiguration config = ClientConfiguration.newBuilder()
                .host("localhost")
                .port(8583)
                .reconnectInterval(5)         // Reconecta a cada 5 segundos se cair
                .addLoggingHandler(true)       // Mascaramento automático de PAN nos logs
                .build();

        Iso8583Client<IsoMessage> client = new Iso8583Client<>(config, messageFactory);

        // 3. Conectar
        client.init();
        client.connect();

        // 4. Montar e enviar 0200
        IsoMessage authRequest = messageFactory.newMessage(0x200);
        authRequest.setValue(2,  "4532015112830366", IsoType.LLVAR, 19);
        authRequest.setValue(3,  "003000",           IsoType.NUMERIC, 6);
        authRequest.setValue(4,  "000000015000",     IsoType.NUMERIC, 12);
        authRequest.setValue(11, "123456",           IsoType.NUMERIC, 6);
        authRequest.setValue(22, "051",              IsoType.NUMERIC, 3);
        authRequest.setValue(41, "TERM0001",         IsoType.ALPHA, 8);
        authRequest.setValue(42, "MERCHANT0000001",  IsoType.ALPHA, 15);
        authRequest.setValue(49, "986",              IsoType.NUMERIC, 3);

        // 5. Enviar (assíncrono via Netty)
        client.send(authRequest);

        // 6. Adicionar listener para response
        client.addMessageListener(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                IsoMessage response = (IsoMessage) msg;
                System.out.println("Response DE39: " + response.getObjectValue(39));
                System.out.println("Auth Code DE38: " + response.getObjectValue(38));
            }
        });

        Thread.sleep(5000);
        client.shutdown();
    }
}
```

### Pros
- **Netty** — alta performance, non-blocking, escalável
- **Auto-reconnect** — reconecta automaticamente em caso de queda
- **Mascaramento PAN automático** nos logs (PCI-friendly)
- **Auto-reply Echo** — responde 0800/0810 automaticamente
- Kotlin-friendly (escrito em Kotlin)
- Licença Apache 2.0

### Contras
- Não tem TransactionManager (pipeline de negócio)
- Dependência do j8583 para parsing
- Comunidade menor que jPOS
- Menos documentação e exemplos
- Sem runtime autônomo

---

## 4. imohsenb ISO8583 — Builder Pattern + Android

### Quando usar
- Aplicações Android que precisam falar ISO 8583
- Prototipagem rápida com API fluente (builder pattern)
- Quando você quer NIO e SSL com configuração mínima

### Licença
**MIT** — A mais permissiva de todas.

### Maven
```xml
<dependency>
    <groupId>com.imohsenb</groupId>
    <artifactId>ISO8583</artifactId>
    <version>1.0.5</version>
</dependency>
```

### Exemplo: Montar e enviar mensagem com Builder

```java
import com.imohsenb.ISO8583.builders.ISOClientBuilder;
import com.imohsenb.ISO8583.builders.ISOMessageBuilder;
import com.imohsenb.ISO8583.entities.ISOMessage;

public class ImohsenbExample {

    public static void main(String[] args) throws Exception {
        // 1. Montar mensagem 0200 com Builder Pattern (API fluente)
        byte[] packed = ISOMessageBuilder.Packer(ISOMessage.Version.V1987)
                .networkManagement()                             // Ou .authorization()
                .mti(ISOMessage.MessageType.AUTHORIZATION_REQUEST) // 0200
                .processCode("003000")
                .setField(2,  "4532015112830366")                // PAN
                .setField(4,  "000000015000")                    // Amount
                .setField(11, "123456")                          // STAN
                .setField(22, "051")                             // POS Entry Mode
                .setField(41, "TERM0001")                        // Terminal ID
                .setField(42, "MERCHANT0000001")                 // Merchant ID
                .setField(49, "986")                             // Currency BRL
                .build();

        System.out.println("Packed message: " + packed.length + " bytes");

        // 2. Conectar e enviar via ISOClient (NIO)
        ISOClientBuilder.createSocket("issuer-host", 8583)
                .configureBlocking(true)       // Ou false para NIO
                .build()
                .connect();

        // 3. Parse de mensagem recebida
        ISOMessage parsed = ISOMessageBuilder.Unpacker()
                .setMessage(packed)
                .build();

        System.out.println("MTI: " + parsed.getMTI());
        System.out.println("PAN (DE2): " + parsed.getStringField(2));
        System.out.println("Amount (DE4): " + parsed.getStringField(4));
    }
}
```

### Exemplo com SSL

```java
import com.imohsenb.ISO8583.builders.ISOClientBuilder;

// Conexão SSL — uma linha
ISOClientBuilder.createSocket("secure-host", 8583)
        .enableSSL()                // Habilita SSL/TLS
        .configureBlocking(false)   // NIO non-blocking
        .build()
        .connect();
```

### Pros
- **API builder fluente** — código muito legível
- **MIT License** — sem restrição alguma
- **Android compatível** — funciona em apps mobile
- **SSL e NIO** com configuração mínima
- JAR minúsculo (~50 KB), zero dependências
- Menor curva de aprendizado de todos

### Contras
- Projeto com pouca atividade recente
- Sem TransactionManager
- Sem auto-reconnect
- Sem mascaramento de PAN
- Sem correlação de mensagens
- Comunidade muito pequena
- Não recomendado para switches de produção

---

## 5. Apache Camel ISO-8583 — Integração Enterprise

### Quando usar
- Integração de sistemas legados via Camel routes
- Quando já usa Apache Camel na arquitetura
- Orquestração de fluxos de pagamento com EIPs (Enterprise Integration Patterns)
- Spring Boot + Camel stack

### Licença
**Apache 2.0** — Livre para uso comercial.

### Maven
```xml
<!-- Camel core + ISO 8583 data format -->
<dependency>
    <groupId>org.apache.camel</groupId>
    <artifactId>camel-iso8583</artifactId>
    <version>4.14.0</version>
</dependency>

<!-- Para Spring Boot -->
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-iso8583-starter</artifactId>
    <version>4.14.0</version>
</dependency>
```

### Exemplo: Rota Camel para processar ISO 8583

```java
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.j8583.J8583DataFormat;

public class CamelIso8583Route extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // Configurar data format
        J8583DataFormat iso8583 = new J8583DataFormat();
        iso8583.setDefaultIsoType(0x200);  // Default MTI para parsing

        // Rota: recebe TCP na 8583, faz parse, processa, responde
        from("netty:tcp://0.0.0.0:8583?sync=true&decoders=#isoDecoder&encoders=#isoEncoder")
            .unmarshal(iso8583)                          // Parse ISO 8583 → IsoMessage
            .log("Received MTI: ${body.type}")
            .choice()
                .when(simple("${body.type} == 512"))     // 0x200 = autorização
                    .to("direct:authorization")
                .when(simple("${body.type} == 2048"))    // 0x800 = echo
                    .to("direct:echo")
                .otherwise()
                    .log("Unknown MTI: ${body.type}")
            .end();

        // Sub-rota: autorização
        from("direct:authorization")
            .log("Processing auth for PAN: ${body.getObjectValue(2)}")
            .process(exchange -> {
                IsoMessage request = exchange.getIn().getBody(IsoMessage.class);
                IsoMessage response = messageFactory.createResponse(request);
                response.setValue(38, "ABC123", IsoType.ALPHA, 6);
                response.setValue(39, "00",     IsoType.ALPHA, 2);
                exchange.getMessage().setBody(response);
            })
            .marshal(iso8583)                            // IsoMessage → bytes
            .log("Sent 0210 response");

        // Sub-rota: echo
        from("direct:echo")
            .process(exchange -> {
                IsoMessage request = exchange.getIn().getBody(IsoMessage.class);
                IsoMessage response = messageFactory.createResponse(request);
                response.setValue(39, "00", IsoType.ALPHA, 2);
                exchange.getMessage().setBody(response);
            })
            .marshal(iso8583);
    }
}
```

### Exemplo: Spring Boot + Camel ISO 8583

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class PaymentGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentGatewayApplication.class, args);
    }
}

@Component
class PaymentRoute extends RouteBuilder {
    @Override
    public void configure() {
        // Receber via TCP, processar, rotear para emissor, responder
        from("netty:tcp://0.0.0.0:8583?sync=true")
            .unmarshal().custom("iso8583")
            .routeId("iso8583-gateway")
            .to("bean:authorizationService?method=process")
            .marshal().custom("iso8583");
    }
}
```

### Pros
- **Enterprise Integration Patterns** — routing, filtering, transformation nativo
- **Spring Boot starter** — setup rápido
- Composição de fluxos complexos via DSL
- Conecta com qualquer sistema via 300+ componentes Camel
- Monitoramento, retry, circuit breaker via Camel
- Comunidade Apache enorme

### Contras
- Overhead do Camel para cenários simples
- Curva de aprendizado do Camel + ISO 8583
- Parsing é delegado ao j8583 (mesmas limitações)
- Não tem TransactionManager estilo jPOS
- Menos otimizado para ultra-low latency que jPOS/Netty

---

## 6. jBSBE — Annotations para Microservices (j8583 on Steroids)

### Quando usar
- Microservices Java que precisam mapear POJOs para ISO 8583
- Quando quer annotations (`@Iso8583`, `@IsoField`) em vez de XML
- Stack moderna com Java Time API (LocalDateTime, YearMonth)

### Licença
**Apache 2.0** — Livre para uso comercial.

### Maven
```xml
<!-- Clone e build local recomendado (Maven Central desatualizado) -->
<dependency>
    <groupId>com.github.keyhan</groupId>
    <artifactId>jbsbe</artifactId>
    <version>0.0.5</version>
</dependency>
```

### Exemplo: POJO anotado como mensagem ISO 8583

```java
import com.github.keyhan.jbsbe.annotations.Iso8583;
import com.github.keyhan.jbsbe.annotations.IsoField;
import com.github.keyhan.jbsbe.annotations.AutoStan;

import java.time.LocalDateTime;
import java.time.YearMonth;

// POJO anotado — zero boilerplate
@Iso8583(type = 0x200)
public class AuthorizationRequest {

    @IsoField(no = 2, type = IsoType.LLVAR, length = 19)
    private String pan;

    @IsoField(no = 3, type = IsoType.NUMERIC, length = 6)
    private String processingCode;

    @IsoField(no = 4, type = IsoType.AMOUNT, length = 12)
    private long amount;

    @IsoField(no = 7, type = IsoType.DATE10)
    private LocalDateTime transmissionDateTime;   // Java Time API nativo!

    @AutoStan                                      // STAN gerado automaticamente
    @IsoField(no = 11, type = IsoType.NUMERIC, length = 6)
    private String stan;

    @IsoField(no = 14, type = IsoType.DATE_EXP)
    private YearMonth expirationDate;              // YearMonth nativo!

    @IsoField(no = 41, type = IsoType.ALPHA, length = 8)
    private String terminalId;

    @IsoField(no = 49, type = IsoType.NUMERIC, length = 3)
    private String currencyCode;

    // getters/setters...
}
```

### Exemplo: Usar o POJO anotado

```java
import com.github.keyhan.jbsbe.I50Factory;
import com.github.keyhan.jbsbe.I50Message;

public class JbsbeExample {

    public static void main(String[] args) throws Exception {
        // 1. Criar factory
        I50Factory factory = new I50Factory();

        // 2. Criar request a partir do POJO anotado
        AuthorizationRequest auth = new AuthorizationRequest();
        auth.setPan("4532015112830366");
        auth.setProcessingCode("003000");
        auth.setAmount(15000L);
        auth.setTransmissionDateTime(LocalDateTime.now());
        auth.setExpirationDate(YearMonth.of(2027, 12));
        auth.setTerminalId("TERM0001");
        auth.setCurrencyCode("986");

        // 3. Converter POJO → IsoMessage → bytes
        I50Message msg = factory.fromPojo(auth);
        byte[] packed = msg.writeData();

        // 4. Parse: bytes → POJO
        I50Message parsed = factory.parseMessage(packed, 0);
        AuthorizationRequest received = factory.toPojo(parsed, AuthorizationRequest.class);

        System.out.println("PAN: " + received.getPan());
        System.out.println("Amount: " + received.getAmount());
        System.out.println("STAN: " + received.getStan());  // Gerado automaticamente

        // 5. Pretty-print (mascaramento automático)
        System.out.println(parsed.prettyPrint());
    }
}
```

### Pros
- **Annotations Java** — estilo moderno, sem XML
- **@AutoStan** — geração automática de STAN
- **Java Time API** — LocalDateTime, YearMonth nativos
- **Pretty-print** com mascaramento automático
- Ideal para microservices com DTOs tipados
- Baseado em j8583 (compatível com todo seu ecossistema)

### Contras
- Projeto com baixa atividade
- Maven Central desatualizado (clone + build)
- Versão 0.0.5 — maturidade limitada
- Sem networking próprio
- Comunidade muito pequena
- Documentação escassa

---

## 7. nucleus8583 — Ultra-Performance e OSGi

### Quando usar
- Quando performance é prioridade absoluta (claims 4x vs outros)
- Ambientes OSGi (bundles)
- Quando footprint mínimo de memória é requisito

### Licença
**Apache 2.0** — Livre para uso comercial.

### Exemplo: Configuração com notação ISO padrão

```xml
<!-- nucleus8583 usa notação ISO padrão em vez de classes Java -->
<message mti="0200">
    <field no="2"  type="n.."  maxlen="19"/>  <!-- "n.." = numérico variável -->
    <field no="3"  type="n"    length="6"/>    <!-- "n" = numérico fixo -->
    <field no="4"  type="n"    length="12"/>
    <field no="7"  type="n"    length="10"/>
    <field no="11" type="n"    length="6"/>
    <field no="22" type="n"    length="3"/>
    <field no="39" type="an"   length="2"/>    <!-- "an" = alfanumérico -->
    <field no="41" type="ans"  length="8"/>    <!-- "ans" = alfanumérico + especial -->
    <field no="48" type="ans.." maxlen="999"/> <!-- "ans.." = variável -->
</message>
```

### Exemplo: Parse e criação de mensagens

```java
import org.nucleus8583.core.Iso8583MessageFactory;
import org.nucleus8583.core.Iso8583Message;

public class Nucleus8583Example {

    public static void main(String[] args) throws Exception {
        // 1. Factory a partir de XML com notação ISO
        Iso8583MessageFactory factory = new Iso8583MessageFactory("iso8583.xml");

        // 2. Criar mensagem
        Iso8583Message msg = factory.createMessage();
        msg.setMti(0x200);
        msg.set(2,  "4532015112830366");
        msg.set(3,  "003000");
        msg.set(4,  "000000015000");
        msg.set(11, "123456");
        msg.set(41, "TERM0001");

        // 3. Pack (reutiliza buffers internos — zero-alloc)
        byte[] packed = msg.pack();

        // 4. Unpack
        Iso8583Message parsed = factory.createMessage();
        parsed.unpack(packed);
        System.out.println("MTI: " + parsed.getMti());
        System.out.println("PAN: " + parsed.getString(2));
    }
}
```

### Pros
- **Performance 4x superior** (claim do autor vs outros parsers)
- **Footprint mínimo** — reutilização de objetos, zero-alloc
- **OSGi bundle** nativo
- Notação ISO padrão no XML (`n`, `an`, `ans`, `n..`)
- Apache 2.0

### Contras
- **Projeto abandonado** — sem atualizações recentes
- Sem networking
- Documentação mínima
- Comunidade inexistente
- Não recomendado para novos projetos
- Claims de performance não verificados independentemente

---

## Decisão: Qual Framework Usar?

```
                        Precisa de switch completo
                         com TransactionManager?
                               │
                    ┌──── SIM ─┤── NÃO ────┐
                    │                        │
                  jPOS                  Já usa Camel?
                (AGPL!)                      │
                               ┌──── SIM ─┤── NÃO ────┐
                               │                        │
                        Camel ISO-8583          Precisa de
                                              networking TCP?
                                                     │
                                          ┌── SIM ──┤── NÃO ──┐
                                          │                     │
                                   jreactive-8583            j8583
                                   (Netty async)         (parsing puro)
                                          │
                                    É para Android
                                     ou protótipo?
                                          │
                                   imohsenb ISO8583
                                   (builder pattern)
```

### Recomendação por cenário

| Cenário | Framework | Justificativa |
|---|---|---|
| Switch de produção (banco/processadora) | **jPOS** | Único com TransactionManager completo |
| Microsserviço de autorização (fintech) | **jreactive-8583** | Netty + auto-reconnect + Apache 2.0 |
| Gateway de integração com sistemas legados | **Camel ISO-8583** | EIPs + 300 conectores + Spring Boot |
| Parsing simples / biblioteca standalone | **j8583** | Leve, sem dependências, Apache 2.0 |
| Microservices com DTOs tipados | **jBSBE** | Annotations @Iso8583 + Java Time API |
| App Android / protótipo rápido | **imohsenb** | Builder pattern, MIT, Android-ready |
| Licença Apache 2.0 é obrigatória | **j8583** ou **jreactive-8583** | jPOS é AGPL |
| Ultra-low latency / OSGi | **nucleus8583** | Zero-alloc, OSGi bundle (porém inativo) |

---

## Mapeamento de Conceitos: jPOS → Outros Frameworks

Para quem já domina jPOS, este mapa mental ajuda a fazer a transição:

```
Conceito jPOS              j8583                 jreactive-8583         Camel ISO-8583
────────────────────────────────────────────────────────────────────────────────────────
GenericPackager XML        j8583-config.xml      j8583-config.xml       j8583-config.xml
ISOMsg                     IsoMessage            IsoMessage             IsoMessage (body)
ISOMsg.pack()              IsoMessage.writeData  (automático Netty)     marshal(iso8583)
ISOMsg.unpack()            factory.parseMessage  (automático Netty)     unmarshal(iso8583)
ISOMsg.setMTI("0200")      factory.newMessage    factory.newMessage     factory.newMessage
                            (0x200)               (0x200)                (0x200)
NACChannel                 (você implementa)     Netty Channel          Netty TCP component
QMUX                       (você implementa)     (auto-correlação)      Camel correlator
TransactionManager         (não tem)             (não tem)              Camel routes + EIPs
Q2 Runtime                 (não tem)             (não tem)              Spring Boot
Space                      (não tem)             (não tem)              Camel Exchange
Participant.prepare()      (não tem)             handler.channelRead    Processor.process()
Participant.ABORTED        (não tem)             throw exception        exchange.setException
```

---

## Dependências Maven — Copie e Cole

### Stack 1: jPOS (switch completo)
```xml
<dependency>
    <groupId>org.jpos</groupId>
    <artifactId>jpos</artifactId>
    <version>2.1.9</version>
</dependency>
```

### Stack 2: j8583 + seu framework de networking
```xml
<dependency>
    <groupId>net.sf.j8583</groupId>
    <artifactId>j8583</artifactId>
    <version>3.0.1</version>
</dependency>
```

### Stack 3: jreactive-8583 (Netty + j8583)
```xml
<dependency>
    <groupId>com.github.kpavlov.jreactive8583</groupId>
    <artifactId>netty-iso8583</artifactId>
    <version>1.5.1</version>
</dependency>
```

### Stack 4: imohsenb (lightweight + Android)
```xml
<dependency>
    <groupId>com.imohsenb</groupId>
    <artifactId>ISO8583</artifactId>
    <version>1.0.5</version>
</dependency>
```

### Stack 5: Apache Camel + Spring Boot
```xml
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-iso8583-starter</artifactId>
    <version>4.14.0</version>
</dependency>
```
