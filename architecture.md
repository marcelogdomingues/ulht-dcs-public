# ULHT Digital Credential System - Complete Architecture

**Version:** 2.0  
**Last Updated:** October 2025  
**Status:** Production Ready

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Infrastructure Architecture](#infrastructure-architecture)
3. [Microservices Architecture](#microservices-architecture)
4. [Event-Driven Flow](#event-driven-flow)
5. [API Gateway Architecture](#api-gateway-architecture)
6. [Network Topology](#network-topology)
7. [Data Flow Diagrams](#data-flow-diagrams)
8. [Technology Stack](#technology-stack)

---

## System Overview

```mermaid
graph TB
    subgraph "External Clients"
        MobileApp[📱 Mobile App<br/>iOS/Android]
        WebApp[🌐 Web Application]
        VerifierApp[🔍 Verifier App]
    end

    subgraph "API Gateway Layer"
        Kong[🛡️ Kong API Gateway<br/>:8000, :8001]
        KongUI[📊 Kong UI<br/>:8080]
    end

    subgraph "Microservices Layer"
        StudentService[🎓 Student Service<br/>:8084<br/>Entry Point]
        LusofonaService[🍪 Lusofona Service<br/>:8085<br/>ULHT Integration]
        CredentialService[🎁 Credential Service<br/>:8086<br/>W3C Issuance]
        FulfilmentService[📦 Fulfilment Service<br/>:8087<br/>Progress Tracking]
    end

    subgraph "Message Broker"
        Kafka[📨 Kafka<br/>:9092, :29092]
        Zookeeper[🐘 Zookeeper<br/>:2181]
    end

    subgraph "Service Discovery"
        Consul[🔍 Consul<br/>:8500]
    end

    subgraph "Management & Monitoring"
        KafkaUI[📊 Kafka UI<br/>:8081]
    end

    subgraph "External Services"
        WaltIDWallet[💼 WaltID Wallet<br/>:7001]
        WaltIDIssuer[✍️ WaltID Issuer<br/>:7002]
        WaltIDVerifier[✅ WaltID Verifier<br/>:7003]
        ULHTAPI[🏛️ ULHT API<br/>SIGES]
    end

    MobileApp --> Kong
    WebApp --> Kong
    VerifierApp --> Kong
    Kong --> StudentService
    Kong --> LusofonaService
    Kong --> CredentialService
    Kong --> FulfilmentService

    StudentService --> Kafka
    LusofonaService --> Kafka
    CredentialService --> Kafka
    FulfilmentService --> Kafka

    Kafka --> Zookeeper
    LusofonaService --> ULHTAPI
    CredentialService --> WaltIDIssuer
    CredentialService --> WaltIDWallet
    CredentialService --> WaltIDVerifier

    StudentService -.-> Consul
    LusofonaService -.-> Consul
    CredentialService -.-> Consul
    FulfilmentService -.-> Consul

    KafkaUI --> Kafka
    KongUI --> Kong

    style StudentService fill:#e1f5ff
    style LusofonaService fill:#fff4e1
    style CredentialService fill:#ffe1f5
    style FulfilmentService fill:#e1ffe1
    style Kafka fill:#f0e1ff
    style Kong fill:#ffe1e1
```

---

## Infrastructure Architecture

```mermaid
graph TB
    subgraph "Infrastructure Tier 1"
        Zookeeper[🐘 Zookeeper<br/>Container: zookeeper<br/>Port: 2181<br/>Network: backend]
    end

    subgraph "Infrastructure Tier 2"
        Kafka[📨 Kafka Broker<br/>Container: kafka<br/>Ports: 9092, 29092<br/>Network: backend]
    end

    subgraph "Infrastructure Tier 3"
        Consul[🔍 Consul<br/>Container: consul<br/>Port: 8500<br/>DNS: 8600<br/>Networks: backend, frontend]
        KafkaUI[📊 Kafka UI<br/>Container: kafka-ui<br/>Port: 8081<br/>Networks: frontend, backend]
        Kong[🛡️ Kong Gateway<br/>Container: api-gateway<br/>Ports: 8000, 8001, 8443, 8444<br/>Networks: frontend, backend, waltid_network]
        KongUI[🌐 Kong UI<br/>Container: kong-ui<br/>Port: 8080<br/>Network: frontend]
    end

    Zookeeper --> Kafka
    KafkaUI --> Kafka
    KafkaUI --> Zookeeper

    style Zookeeper fill:#ffebcd
    style Kafka fill:#e6f3ff
    style Consul fill:#fffacd
    style KafkaUI fill:#f0f8ff
    style Kong fill:#ffe4e1
```

---

## Microservices Architecture

```mermaid
graph LR
    subgraph "Student Service :8084"
        SS[Student Service<br/>Entry Point<br/>Request Validation<br/>Correlation ID Gen]
        SSP[Kafka Producer<br/>student.login.requested]
        SSF[Feign Client<br/>Fulfilment Service]
    end

    subgraph "Lusofona Service :8085"
        LS[Lusofona Service<br/>ULHT Integration<br/>Student Authentication<br/>Data Retrieval]
        LSC[Kafka Consumer<br/>student.login.requested]
        LSP[Kafka Producer<br/>credential.requests]
        LSF[Feign Client<br/>ULHT API]
    end

    subgraph "Credential Service :8086"
        CS[Credential Service<br/>W3C VC Issuance<br/>Generic Templates<br/>walt.id Integration]
        CSC[Kafka Consumer<br/>credential.requests]
        CSP1[Kafka Producer<br/>credential.progress]
        CSP2[Kafka Producer<br/>credential.completed]
        CSP3[Kafka Producer<br/>credential.error]
        CSW[WaltID Clients<br/>Issuer, Wallet, Verifier]
    end

    subgraph "Fulfilment Service :8087"
        FS[Fulfilment Service<br/>Progress Tracking<br/>SSE Notifications<br/>Result Storage]
        FSC1[Kafka Consumer<br/>credential.progress]
        FSC2[Kafka Consumer<br/>credential.completed]
        FSC3[Kafka Consumer<br/>credential.error]
        FSSSE[SSE Endpoint<br/>Real-time Updates]
    end

    SS --> SSP
    SSP -->|Kafka| LSC
    LSC --> LS
    LS --> LSP
    LSP -->|Kafka| CSC
    CSC --> CS
    CS --> CSP1
    CS --> CSP2
    CS --> CSP3
    CSP1 -->|Kafka| FSC1
    CSP2 -->|Kafka| FSC2
    CSP3 -->|Kafka| FSC3
    FSC1 --> FS
    FSC2 --> FS
    FSC3 --> FS
    FS --> FSSSE
    LS --> LSF
    CS --> CSW
    SS --> SSF
    SSF --> FS

    style SS fill:#e1f5ff
    style LS fill:#fff4e1
    style CS fill:#ffe1f5
    style FS fill:#e1ffe1
```

---

## Event-Driven Flow

```mermaid
sequenceDiagram
    participant Mobile as 📱 Mobile App
    participant Student as 🎓 Student Service
    participant Kafka as 📨 Kafka
    participant Lusofona as 🍪 Lusofona Service
    participant ULHT as 🏛️ ULHT API
    participant Credential as 🎁 Credential Service
    participant WaltID as ✍️ WaltID Issuer
    participant Fulfilment as 📦 Fulfilment Service

    Mobile->>Student: POST /student/issue<br/>{userName, installKey}
    Student->>Student: Generate correlationId
    Student->>Kafka: Publish: student.login.requested
    Student-->>Mobile: 202 Accepted<br/>{correlationId, status: PROCESSING}

    Kafka->>Lusofona: Consume: student.login.requested
    Lusofona->>ULHT: Authenticate & Fetch Student Data
    ULHT-->>Lusofona: Student Data
    Lusofona->>Kafka: Publish: credential.requests<br/>{correlationId, studentData}

    Kafka->>Credential: Consume: credential.requests
    Credential->>Credential: Load Credential Templates
    Credential->>Credential: Build Credential Data
    
    loop For each credential type
        Credential->>WaltID: POST /onboard/issuer (if needed)
        Credential->>WaltID: POST /openid4vc/jwt/issue
        WaltID-->>Credential: Credential Offer URL
        Credential->>Kafka: Publish: credential.progress<br/>{progress: 20%, 40%, 60%, 80%}
    end

    Credential->>Kafka: Publish: credential.completed<br/>{correlationId, credentialOfferUrls}

    Kafka->>Fulfilment: Consume: credential.completed
    Fulfilment->>Fulfilment: Store Results
    Fulfilment->>Fulfilment: Send SSE Update

    Mobile->>Student: GET /student/status/{correlationId}
    Student->>Fulfilment: GET /fulfilment/status/{correlationId}
    Fulfilment-->>Student: {status: COMPLETED, progress: 100%}
    Student-->>Mobile: Status Response

    Mobile->>Student: GET /student/credentials/{correlationId}
    Student->>Fulfilment: GET /fulfilment/result/{correlationId}
    Fulfilment-->>Student: {credentialOfferUrls: [...]}
    Student-->>Mobile: Credentials Response
```

---

## API Gateway Architecture

```mermaid
graph TB
    %% External Clients
    subgraph "External Clients"
        Client[🌐 Client Applications]
    end

    %% Kong API Gateway
    subgraph "Kong API Gateway"
        direction TB
        KongProxy[Kong Proxy<br/>🧩 Port: 8000<br/>Routes: /api/v1/*]
        KongAdmin[Kong Admin API<br/>⚙️ Port: 8001]
    end

    %% Kong Routes
    subgraph "Kong Routes"
        direction TB
        Route1[/api/v1/credentials<br/>→ Credential Service :8086/]
        Route2[/api/v1/students<br/>→ WaltID Proxy :8085/]
        Route3[/api/v1/fulfilment<br/>→ Fulfilment Service :8087/]
        Route4[/api/v1/lusofona<br/>→ Lusofona Service :8085/]
    end

    %% Kong Plugins
    subgraph "Kong Plugins"
        direction TB
        Plugin1[CORS Plugin<br/>All Origins<br/>Methods: GET, POST, PUT, DELETE]
        Plugin2[Rate Limiting Plugin<br/>100/min, 1000/hour]
        Plugin3[Request Transformer<br/>Add Header: X-Service]
        Plugin4[Response Transformer<br/>Add Header: X-Service-Version]
    end

    %% Backend Services
    subgraph "Backend Services"
        direction TB
        CredentialService[Credential Service<br/>:8086]
        WaltIDProxy[WaltID Proxy<br/>:8085]
        FulfilmentService[Fulfilment Service<br/>:8087]
        LusofonaService[Lusofona Service<br/>:8085]
    end

    %% Connections
    Client --> KongProxy
    KongProxy --> Route1
    KongProxy --> Route2
    KongProxy --> Route3
    KongProxy --> Route4

    %% Route 1
    Route1 --> Plugin1
    Route1 --> Plugin2
    Route1 --> Plugin3
    Route1 --> Plugin4
    Route1 --> CredentialService

    %% Route 2
    Route2 --> Plugin1
    Route2 --> Plugin2
    Route2 --> WaltIDProxy

    %% Route 3
    Route3 --> Plugin1
    Route3 --> Plugin2
    Route3 --> FulfilmentService

    %% Route 4
    Route4 --> Plugin1
    Route4 --> Plugin2
    Route4 --> LusofonaService

    %% Styling
    style KongProxy fill:#ffe4e1,stroke:#d66,stroke-width:1px
    style Route1 fill:#e1f5ff,stroke:#39f,stroke-width:0.5px
    style Route2 fill:#fff4e1,stroke:#f9a602,stroke-width:0.5px
    style Route3 fill:#e1ffe1,stroke:#4CAF50,stroke-width:0.5px
    style Route4 fill:#ffe1f5,stroke:#f48fb1,stroke-width:0.5px
    style Plugin1 fill:#f0f0f0,stroke:#aaa
    style Plugin2 fill:#f0f0f0,stroke:#aaa
    style Plugin3 fill:#f0f0f0,stroke:#aaa
    style Plugin4 fill:#f0f0f0,stroke:#aaa

```

---

## Network Topology

```mermaid
graph TB
    subgraph "Frontend Network (172.21.0.0/16)"
        Kong[Kong Gateway]
        KongUI[Kong UI]
        KafkaUI[Kafka UI]
        Consul[Consul]
        StudentService[Student Service]
        LusofonaService[Lusofona Service]
        CredentialService[Credential Service]
        FulfilmentService[Fulfilment Service]
    end

    subgraph "Backend Network (172.20.0.0/16)"
        Kafka[Kafka]
        Zookeeper[Zookeeper]
        Consul2[Consul]
        KafkaUI2[Kafka UI]
        StudentService2[Student Service]
        LusofonaService2[Lusofona Service]
        CredentialService2[Credential Service]
        FulfilmentService2[Fulfilment Service]
    end

    subgraph "WaltID Network (External)"
        Kong3[Kong Gateway]
        CredentialService3[Credential Service]
        KafkaUI3[Kafka UI]
        WaltIDWallet[WaltID Wallet :7001]
        WaltIDIssuer[WaltID Issuer :7002]
        WaltIDVerifier[WaltID Verifier :7003]
    end

    Kong -.->|HTTP| StudentService
    Kong -.->|HTTP| LusofonaService
    Kong -.->|HTTP| CredentialService
    Kong -.->|HTTP| FulfilmentService

    StudentService -.->|Kafka| Kafka
    LusofonaService -.->|Kafka| Kafka
    CredentialService -.->|Kafka| Kafka
    FulfilmentService -.->|Kafka| Kafka

    Kafka -.->|Zookeeper| Zookeeper

    CredentialService3 -.->|HTTP| WaltIDWallet
    CredentialService3 -.->|HTTP| WaltIDIssuer
    CredentialService3 -.->|HTTP| WaltIDVerifier

    style Frontend fill:#e1f5ff
    style Backend fill:#fff4e1
    style WaltID fill:#ffe1f5
```

---

## Data Flow Diagrams

### Complete Credential Issuance Flow

```mermaid
flowchart TD
    Start([📱 Student Login Request]) --> Validate{Validate Request}
    Validate -->|Invalid| Error1[❌ Return Error]
    Validate -->|Valid| GenerateID[Generate correlationId]
    
    GenerateID --> PublishKafka[📨 Publish to Kafka<br/>Topic: student.login.requested]
    
    PublishKafka --> ConsumeLusofona[🍪 Lusofona Service Consumes]
    ConsumeLusofona --> AuthULHT[🔐 Authenticate with ULHT API]
    
    AuthULHT -->|Auth Failed| Error2[❌ Publish Error Event]
    AuthULHT -->|Success| FetchData[📊 Fetch Student Data]
    
    FetchData --> EnrichData[✨ Enrich Student Data]
    EnrichData --> PublishCredential[📨 Publish to Kafka<br/>Topic: credential.requests]
    
    PublishCredential --> ConsumeCredential[🎁 Credential Service Consumes]
    ConsumeCredential --> LoadTemplates[📋 Load Credential Templates]
    
    LoadTemplates --> CheckTemplates{Templates Found?}
    CheckTemplates -->|No| Error3[❌ Publish Error Event]
    CheckTemplates -->|Yes| LoopCreds[🔄 For Each Credential Type]
    
    LoopCreds --> CheckEnabled{Enabled?}
    CheckEnabled -->|No| Skip[⏭️ Skip Credential]
    CheckEnabled -->|Yes| CheckCondition{Condition Met?}
    
    CheckCondition -->|No| Skip
    CheckCondition -->|Yes| MapFields[🗺️ Map Fields from Student Data]
    MapFields --> BuildCredential[🏗️ Build Credential Data]
    BuildCredential --> OnboardIssuer[🔑 Onboard Issuer<br/>walt.id]
    OnboardIssuer --> IssueCredential[✍️ Issue Credential<br/>walt.id]
    
    IssueCredential -->|Success| PublishProgress[📨 Publish Progress<br/>Topic: credential.progress]
    IssueCredential -->|Error| PublishError[📨 Publish Error<br/>Topic: credential.error]
    
    PublishProgress --> CheckMore{More Credentials?}
    CheckMore -->|Yes| LoopCreds
    CheckMore -->|No| PublishComplete[📨 Publish Complete<br/>Topic: credential.completed]
    
    PublishComplete --> ConsumeFulfilment[📦 Fulfilment Service Consumes]
    ConsumeFulfilment --> StoreResults[💾 Store Results]
    StoreResults --> SendSSE[📡 Send SSE Update]
    SendSSE --> End([✅ Complete])
    
    Error1 --> End
    Error2 --> ConsumeFulfilment
    Error3 --> ConsumeFulfilment
    PublishError --> ConsumeFulfilment
    Skip --> CheckMore

    style Start fill:#e1f5ff
    style End fill:#e1ffe1
    style Error1 fill:#ffe1e1
    style Error2 fill:#ffe1e1
    style Error3 fill:#ffe1e1
```

### Kafka Topics Flow

```mermaid
graph LR
    subgraph "Student Service"
        SSP[Producer<br/>student.login.requested]
    end

    subgraph "Kafka Topics"
        T1[(student.login.requested)]
        T2[(credential.requests)]
        T3[(credential.progress)]
        T4[(credential.completed)]
        T5[(credential.error)]
        T6[(verification.requested)]
        T7[(verification.completed)]
    end

    subgraph "Lusofona Service"
        LSC[Consumer<br/>student.login.requested]
        LSP[Producer<br/>credential.requests]
    end

    subgraph "Credential Service"
        CSC[Consumer<br/>credential.requests]
        CSP1[Producer<br/>credential.progress]
        CSP2[Producer<br/>credential.completed]
        CSP3[Producer<br/>credential.error]
        VSC[Consumer<br/>verification.requested]
        VSP[Producer<br/>verification.completed]
    end

    subgraph "Fulfilment Service"
        FSC1[Consumer<br/>credential.progress]
        FSC2[Consumer<br/>credential.completed]
        FSC3[Consumer<br/>credential.error]
        FSC4[Consumer<br/>verification.completed]
    end

    SSP -->|Publish| T1
    T1 -->|Consume| LSC
    LSP -->|Publish| T2
    T2 -->|Consume| CSC
    CSP1 -->|Publish| T3
    CSP2 -->|Publish| T4
    CSP3 -->|Publish| T5
    T3 -->|Consume| FSC1
    T4 -->|Consume| FSC2
    T5 -->|Consume| FSC3

    VSC -->|Consume| T6
    VSP -->|Publish| T7
    T7 -->|Consume| FSC4

    style T1 fill:#f0e1ff
    style T2 fill:#f0e1ff
    style T3 fill:#f0e1ff
    style T4 fill:#f0e1ff
    style T5 fill:#ffe1e1
    style T6 fill:#f0e1ff
    style T7 fill:#f0e1ff
```

---

## Technology Stack

```mermaid
graph TB
    subgraph "Runtime"
        Java[☕ Java 25]
        SpringBoot[🍃 Spring Boot 3.5.6]
        SpringCloud[☁️ Spring Cloud 2025.0.0]
    end

    subgraph "Messaging"
        Kafka[📨 Apache Kafka 3.x]
        Zookeeper[🐘 Zookeeper 7.4.0]
    end

    subgraph "API Gateway"
        Kong[🛡️ Kong Gateway Latest]
        Nginx[🌐 Nginx Alpine]
    end

    subgraph "Service Discovery"
        Consul[🔍 Consul 1.15.4]
    end

    subgraph "External Integrations"
        WaltID[💼 WaltID v0.15.0<br/>Wallet, Issuer, Verifier]
        ULHTAPI[🏛️ ULHT SIGES API]
    end

    subgraph "Standards"
        W3C[✅ W3C VC Data Model]
        OID4VCI[🔐 OID4VCI Protocol]
        SCHAC[📚 SCHAC 2.0]
        ESC[🇪🇺 European Student Card]
    end

    subgraph "Development Tools"
        Maven[📦 Maven 3.9+]
        Docker[🐳 Docker]
        OpenAPI[📝 OpenAPI 3.0]
        Swagger[📊 Swagger UI]
    end

    Java --> SpringBoot
    SpringBoot --> SpringCloud
    SpringBoot --> Kafka
    Kafka --> Zookeeper
    SpringCloud --> Consul
    Kong --> Nginx
    SpringBoot --> WaltID
    SpringBoot --> ULHTAPI
    SpringBoot --> W3C
    SpringBoot --> OID4VCI
    Maven --> Docker
    OpenAPI --> Swagger

    style Java fill:#ffd700
    style SpringBoot fill:#6db33f
    style Kafka fill:#231f20
    style Kong fill:#003459
    style WaltID fill:#4a90e2
```

---

## Service Ports Summary

| Service | Container Name | Port(s) | Network(s) | Purpose |
|---------|---------------|---------|-------------|---------|
| **Zookeeper** | zookeeper | 2181 | backend | Kafka coordination |
| **Kafka** | kafka | 9092, 29092 | backend | Message broker |
| **Consul** | consul | 8500, 8600 | backend, frontend | Service discovery |
| **Kafka UI** | kafka-ui | 8081 | frontend, backend | Kafka management |
| **Kong Gateway** | api-gateway | 8000, 8001, 8443, 8444 | frontend, backend, waltid_network | API gateway |
| **Kong UI** | kong-ui | 8080 | frontend | Kong management UI |
| **Student Service** | ulht-student-service | 8084 | frontend, backend | Entry point |
| **Lusofona Service** | ulht-waltid-proxy | 8085 | frontend, backend, waltid_network | ULHT integration |
| **Credential Service** | ulht-credential-service | 8086 | frontend, backend, waltid_network | W3C credential issuance |
| **Fulfilment Service** | ulht-fulfilment-service | 8087 | frontend, backend | Progress tracking |
| **WaltID Wallet** | (external) | 7001 | waltid_network | Wallet service |
| **WaltID Issuer** | (external) | 7002 | waltid_network | Credential issuance |
| **WaltID Verifier** | (external) | 7003 | waltid_network | Credential verification |

---

## Kafka Topics Summary

| Topic | Producer | Consumer | Purpose |
|-------|----------|----------|---------|
| `student.login.requested` | Student Service | Lusofona Service | Student login events |
| `credential.requests` | Lusofona Service | Credential Service | Credential issuance requests |
| `credential.progress` | Credential Service | Fulfilment Service | Progress updates (20%, 40%, 60%, 80%, 100%) |
| `credential.completed` | Credential Service | Fulfilment Service | Completion with credential URLs |
| `credential.error` | Credential Service | Fulfilment Service | Error events |
| `verification.requested` | Student Service | Credential Service | Verification requests |
| `verification.completed` | Credential Service | Fulfilment Service | Verification results |
| `wallet.requests` | Credential Service | (Internal) | Wallet operations |
| `wallet.progress` | Credential Service | (Internal) | Wallet progress |
| `wallet.completed` | Credential Service | (Internal) | Wallet completion |

---

## Network Architecture

### Network Segments

1. **Frontend Network (172.21.0.0/16)**
   - Client-facing services
   - API Gateway
   - Management UIs
   - Service endpoints

2. **Backend Network (172.20.0.0/16)**
   - Internal microservices communication
   - Kafka brokers
   - Service discovery
   - Database connections (if any)

3. **WaltID Network (External)**
   - External walt.id services
   - Credential Service (for walt.id integration)
   - Kong Gateway (for walt.id routing)

---

## Deployment Architecture

```mermaid
graph TB
    subgraph "Docker Compose"
        DC[Docker Compose<br/>Orchestration]
    end

    subgraph "Infrastructure Services"
        ZK[Zookeeper Container]
        KF[Kafka Container]
        CS[Consul Container]
        KU[Kafka UI Container]
        KG[Kong Gateway Container]
        KUI[Kong UI Container]
    end

    subgraph "Application Services"
        SS[Student Service Container]
        LS[Lusofona Service Container]
        CS2[Credential Service Container]
        FS[Fulfilment Service Container]
    end

    subgraph "External Services"
        WID[WaltID Services<br/>Wallet, Issuer, Verifier]
    end

    subgraph "Volumes"
        V1[Kafka Data Volume]
        V2[Zookeeper Data Volume]
        V3[Zookeeper Logs Volume]
        V4[Consul Data Volume]
    end

    DC --> ZK
    DC --> KF
    DC --> CS
    DC --> KU
    DC --> KG
    DC --> KUI
    DC --> SS
    DC --> LS
    DC --> CS2
    DC --> FS

    KF --> V1
    ZK --> V2
    ZK --> V3
    CS --> V4

    CS2 --> WID

    style DC fill:#4a90e2
    style WID fill:#ff6b6b
```

---

## Security Architecture

```mermaid
graph TB
    subgraph "External Layer"
        Client[Client Applications]
    end

    subgraph "API Gateway Security"
        Kong[Cong Gateway]
        CORS[CORS Policy<br/>Configurable Origins]
        RateLimit[Rate Limiting<br/>100/min, 1000/hour]
        Auth[Authentication<br/>Future: OAuth2/JWT]
    end

    subgraph "Service Security"
        HTTPS[HTTPS/TLS<br/>Production Only]
        Validation[Input Validation<br/>All Endpoints]
        CorrelationID[Correlation ID Tracking]
    end

    subgraph "Network Security"
        NetworkSeg[Network Segmentation<br/>Frontend/Backend/WaltID]
        Firewall[Firewall Rules<br/>Port Restrictions]
    end

    subgraph "Data Security"
        Encryption[Data Encryption<br/>In Transit]
        Secrets[Secret Management<br/>Environment Variables]
    end

    Client --> Kong
    Kong --> CORS
    Kong --> RateLimit
    Kong --> Auth
    Kong --> HTTPS
    Kong --> Validation
    Kong --> NetworkSeg
    Kong --> Firewall
    Kong --> Encryption
    Kong --> Secrets

    style Kong fill:#ffe4e1
    style CORS fill:#e1f5ff
    style RateLimit fill:#fff4e1
    style Encryption fill:#e1ffe1
```

---

## Monitoring & Observability

```mermaid
graph TB
    subgraph "Application Metrics"
        Actuator[Spring Boot Actuator<br/>Health, Metrics, Info]
        Prometheus[Prometheus<br/>:9090<br/>Metrics Collection]
        Grafana[Grafana<br/>:3000<br/>Visualization]
    end

    subgraph "Logging"
        StructuredLog[Structured Logging<br/>Correlation IDs]
        LogLevel[Configurable Log Levels]
    end

    subgraph "Kafka Monitoring"
        KafkaUI[Kafka UI<br/>Topic Monitoring]
        KafkaExporter[Kafka Exporter<br/>:9308<br/>Kafka Metrics]
        ConsumerLag[Consumer Lag Tracking]
    end

    subgraph "Service Health"
        HealthChecks[Health Check Endpoints<br/>/actuator/health]
        CustomHealth[Custom Health Indicators]
    end

    subgraph "Distributed Tracing"
        CorrelationID[Correlation ID Propagation<br/>Across Services]
    end

    subgraph "Microservices"
        StudentService[Student Service<br/>:8084]
        LusofonaService[Lusofona Service<br/>:8085]
        CredentialService[Credential Service<br/>:8086]
        FulfilmentService[Fulfilment Service<br/>:8087]
    end

    StudentService -->|/actuator/prometheus| Actuator
    LusofonaService -->|/actuator/prometheus| Actuator
    CredentialService -->|/actuator/prometheus| Actuator
    FulfilmentService -->|/actuator/prometheus| Actuator
    
    Actuator --> Prometheus
    KafkaExporter --> Prometheus
    Prometheus --> Grafana
    StructuredLog --> CorrelationID
    KafkaUI --> ConsumerLag
    HealthChecks --> CustomHealth

    style Actuator fill:#e1f5ff
    style Prometheus fill:#e5a50a
    style Grafana fill:#f46800
    style KafkaUI fill:#f0e1ff
    style KafkaExporter fill:#f0e1ff
    style CorrelationID fill:#ffe1f5
```

### Monitoring Stack

**Prometheus** (`:9090`)
- Scrapes metrics from all microservices every 15 seconds
- Stores metrics for 30 days
- Provides query interface for metrics analysis

**Grafana** (`:3000`)
- Pre-configured dashboards for:
  - Microservices Overview (Request rate, Error rate, Response time)
  - JVM Metrics (Memory, GC, Threads)
  - Kafka Overview (Topic size, Consumer lag)
- Auto-provisioned data source connection to Prometheus

**Kafka Exporter** (`:9308`)
- Exports Kafka-specific metrics:
  - Topic sizes and offsets
  - Consumer group lag
  - Broker information
  - Partition metrics

**Metrics Exposed by Each Service**
- HTTP request/response metrics (rate, latency, errors)
- JVM metrics (memory, GC, threads, CPU)
- Kafka producer/consumer metrics
- Custom business metrics

For detailed monitoring setup, see [monitoring/README.md](monitoring/README.md)

---

## Credential Types Architecture

```mermaid
graph TB
    subgraph "Credential Templates"
        Template1[Educational ID<br/>Priority: 10<br/>SCHAC Compliant<br/>Always Issued]
        Template2[Identity Credential<br/>Priority: 15<br/>Digital Identity<br/>Always Issued]
        Template3[European Student Card<br/>Priority: 20<br/>ESC Initiative<br/>Always Issued]
        Template4[University Degree<br/>Priority: 30<br/>Graduation Certificate<br/>Conditional: Graduates Only]
    end

    subgraph "Configuration"
        YAML[YAML Configuration<br/>application.yml<br/>No Code Changes Needed]
    end

    subgraph "Field Mapping"
        Mapping[Dynamic Field Mapping<br/>Multiple Source Fields<br/>Fallback Support]
    end

    subgraph "Conditional Issuance"
        Condition[SpEL Conditions<br/>Graduation Date<br/>Nationality<br/>Academic Level]
    end

    subgraph "WaltID Integration"
        WaltID[WaltID Issuer<br/>OID4VCI Protocol<br/>JWT & SD-JWT Formats]
    end

    YAML --> Template1
    YAML --> Template2
    YAML --> Template3
    YAML --> Template4

    Template1 --> Mapping
    Template2 --> Mapping
    Template3 --> Mapping
    Template4 --> Mapping

    Template4 --> Condition

    Mapping --> WaltID
    Condition --> WaltID

    style Template1 fill:#e1f5ff
    style Template2 fill:#fff4e1
    style Template3 fill:#ffe1f5
    style Template4 fill:#e1ffe1
    style YAML fill:#f0e1ff
```

---

## Conclusion

This architecture provides:

- ✅ **Scalable Microservices** - Independent, deployable services
- ✅ **Event-Driven Communication** - Asynchronous processing via Kafka
- ✅ **API Gateway** - Centralized routing and security
- ✅ **Service Discovery** - Dynamic service registration
- ✅ **W3C Compliance** - Standards-based credential issuance
- ✅ **Generic Credential System** - Configuration-driven credential types
- ✅ **Comprehensive Monitoring** - Health checks, metrics, and logging
- ✅ **Network Segmentation** - Security through isolation
- ✅ **Production Ready** - Complete infrastructure setup

---

**For detailed API documentation, see:** [DOCUMENTATION.md](DOCUMENTATION.md)  
**For deployment instructions, see:** [README.md](README.md)

