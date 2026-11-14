# Cocos Backend Challenge

API REST en Java / Spring Boot para el challenge descrito en [backend-challenge.md](backend-challenge.md).

## Stack

- Java 25
- Spring Boot 4.1 (Web MVC, Data JPA, Validation)
- PostgreSQL (base provista por el challenge) + Flyway
- Testcontainers para tests de integración

## Requisitos

- JDK 25
- Acceso a la base PostgreSQL del challenge
- Docker (para los tests, que levantan un PostgreSQL con Testcontainers)

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

Los tests de integración corren contra un PostgreSQL 18 efímero (Testcontainers) con el esquema original y una copia
de los datos provistos (`src/test/resources/db/seed`). Nunca tocan la base real.

Para levantar la app localmente contra esa base efímera en lugar de la real: ejecutar `TestChallengeApplication`.

## Cambios en la base de datos

El esquema se versiona con Flyway (`src/main/resources/db/migration`):

| Versión | Cambio | Motivo |
|---|---|---|
| V1 | Esquema original | No se ejecuta sobre la base existente (se registra como *baseline*); permite recrear el esquema en una base vacía, como en los tests. |
| V2 | Índice `orders (userid, status)` | El portfolio y la validación de órdenes leen las órdenes de un usuario por estado (`FILLED` / `NEW`). Antes solo existían índices de PK. |
| V2 | Índice único `marketdata (instrumentid, date)` | Búsqueda del último precio de un instrumento; además garantiza una sola cotización por instrumento y día. |

## Decisiones de diseño y supuestos

Ver el plan en [docs/PLAN.md](docs/PLAN.md). _Se completará al finalizar la implementación._
