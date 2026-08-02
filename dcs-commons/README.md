# DCS Commons

Shared, auto-configured building blocks used by every DCS backend service.

## What it provides

- **API-key security** (`ApiKeySecurityAutoConfiguration`) — a `SecurityFilterChain`
  that protects all business endpoints with a constant-time `apikey`-header check,
  while leaving actuator `health`/`info`/`prometheus` and the OpenAPI/Swagger
  endpoints public. Optional JWT (OAuth2/OIDC) resource-server support activates
  automatically when a JWT issuer URI is configured.

Auto-configuration is registered via
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`,
so it activates just by being on the classpath — no explicit `@Import` needed. It is
ordered before Spring Boot's own `SecurityAutoConfiguration`.

## Usage

This module is **not** published to Maven Central. Install it into your local Maven
repository before building any service (the service Dockerfiles do this
automatically):

```bash
mvn -q -f dcs-commons/pom.xml -DskipTests install
```

Then depend on it:

```xml
<dependency>
  <groupId>com.example.dcs</groupId>
  <artifactId>dcs-commons</artifactId>
  <version>1.0.0</version>
</dependency>
```
