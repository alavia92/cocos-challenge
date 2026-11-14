# Cocos Backend Challenge

API REST en Java / Spring Boot para el challenge descrito en [backend-challenge.md](backend-challenge.md).

## Stack

- Java 25
- Spring Boot 4.1 (Web MVC, Data JPA, Validation)
- PostgreSQL (base provista por el challenge)
- Testcontainers para tests de integración

## Requisitos

- JDK 25
- Acceso a la base PostgreSQL del challenge

## Ejecución

Las credenciales se leen de variables de entorno o de un archivo `.env` en la raíz (no versionado):

```properties
DB_URL=jdbc:postgresql://<host>/<database>?sslmode=require
DB_USERNAME=<usuario>
DB_PASSWORD=<password>
```

```bash
./mvnw spring-boot:run
```

## Tests

```bash
./mvnw test
```

## Decisiones de diseño y supuestos

_Pendiente._
