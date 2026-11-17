# Cocos Backend Challenge

API REST en Java / Spring Boot para el challenge descrito en [backend-challenge.md](backend-challenge.md): portfolio
de un usuario, búsqueda de activos y envío y cancelación de órdenes.

## Stack

- Java 25
- Spring Boot 4.1 (Web MVC, Data JPA, Validation)
- PostgreSQL (base provista por el challenge) + Flyway
- Testcontainers para tests de integración
- OpenAPI 3 + OpenAPI Generator (enfoque API-first)
- MapStruct para la conversión entre DTOs y dominio
- Lombok para los getters de las entidades

## Requisitos

- Docker
- JDK 25, solo para ejecutar sin Docker o correr los tests (que levantan un PostgreSQL con Testcontainers)

## Ejecución

### Con Docker (autocontenido)

```bash
docker compose up --build
```

Levanta la API y un PostgreSQL 18 propio. Flyway crea el esquema y carga una copia de los datos provistos (los
mismos que usan los tests); no se necesita la base del challenge ni credenciales. Para empezar de cero con los datos
originales:

```bash
docker compose down -v
```

### Contra la base del challenge

Las credenciales se leen de variables de entorno o de un archivo `.env` en la raíz (no versionado):

```properties
DB_URL=jdbc:postgresql://<host>/<database>?sslmode=require
DB_USERNAME=<usuario>
DB_PASSWORD=<password>
```

```bash
./mvnw spring-boot:run
```

Al arrancar, Flyway aplica las migraciones pendientes sobre la base. También se puede levantar la app contra una base
efímera de Testcontainers, sin Docker Compose: `./mvnw spring-boot:test-run`.

En todos los casos la API queda en `http://localhost:8080`, y se puede probar desde Swagger UI en
`http://localhost:8080/swagger-ui.html`.

## Tests

```bash
./mvnw test
```

Los tests de integración corren contra un PostgreSQL 18 efímero (Testcontainers) con el esquema original y una copia
de los datos provistos (`src/test/resources/db/seed`). Nunca tocan la base real.

| Clase | Qué cubre |
|---|---|
| `OrderControllerTests` | **Test funcional del envío de órdenes**: MARKET / LIMIT, por cantidad o monto, rechazos, validaciones y efecto en el portfolio |
| `OrderConcurrencyTests` | Dos órdenes simultáneas del mismo usuario no pueden gastar los mismos pesos |
| `OrderCancellationTests` | Cancelación de órdenes `NEW` y rechazo para los demás estados |
| `PortfolioControllerTests` | Valores exactos del portfolio (cash, reservas, costo promedio, rendimientos) |
| `InstrumentControllerTests` | Búsqueda por ticker y/o nombre |
| `OpenApiSpecTests` | El contrato OpenAPI se publica en `/openapi.yaml` y Swagger UI en `/swagger-ui.html` |

## API

El contrato está definido en OpenAPI 3 en [`src/main/resources/static/openapi.yaml`](src/main/resources/static/openapi.yaml)
y la app lo publica en `http://localhost:8080/openapi.yaml`. [Swagger UI](http://localhost:8080/swagger-ui.html)
muestra ese mismo contrato y permite ejecutar los endpoints. Es la fuente de verdad de la API (enfoque *API-first*):
en cada build, `openapi-generator-maven-plugin` genera a partir de él las interfaces de los controllers y los DTOs
(`com.cocos.challenge.openapi`, en `target/generated-sources/openapi`), y los controllers las implementan. Para cambiar la
API se modifica el contrato y se recompila.

Ejemplos listos para ejecutar en [`http/cocos-api.http`](http/cocos-api.http) (IntelliJ HTTP Client / VS Code REST
Client). Los errores se devuelven en formato RFC 9457 (`application/problem+json`); los de validación incluyen el
detalle por campo en `errors`.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/users/{userId}/portfolio` | Valor total de la cuenta, pesos disponibles y posiciones |
| `GET` | `/api/instruments?query={texto}` | Búsqueda de activos por ticker y/o nombre |
| `POST` | `/api/orders` | Envío de una orden de compra o venta |
| `POST` | `/api/orders/{orderId}/cancel` | Cancelación de una orden `NEW` |

### Portfolio

```http
GET /api/users/1/portfolio
```

```json
{
  "userId": 1,
  "accountNumber": "10001",
  "totalValue": 889756.00,
  "availableCash": 627500.00,
  "positions": [
    {
      "instrumentId": 47,
      "ticker": "PAMP",
      "name": "Pampa Holding S.A.",
      "quantity": 40,
      "lastPrice": 925.85,
      "marketValue": 37034.00,
      "totalReturnPercent": -0.45,
      "dailyReturnPercent": 0.44
    }
  ]
}
```

(Se muestra una sola posición por brevedad.) Usuario inexistente → 404.

### Búsqueda de activos

```http
GET /api/instruments?query=banco
```

```json
[
  {"id": 22, "ticker": "BBAR", "name": "Banco Frances", "type": "ACCIONES"},
  {"id": 28, "ticker": "BHIP", "name": "Banco Hipotecario S.A.", "type": "ACCIONES"},
  {"id": 31, "ticker": "BMA", "name": "Banco Macro S.A.", "type": "ACCIONES"},
  {"id": 19, "ticker": "BPAT", "name": "Banco Patagonia", "type": "ACCIONES"}
]
```

`query` vacío o ausente → 400.

### Envío de órdenes

```http
POST /api/orders
Content-Type: application/json

{"userId": 1, "instrumentId": 47, "side": "BUY", "type": "MARKET", "quantity": 10}
```

| Campo | Descripción |
|---|---|
| `userId`, `instrumentId` | Obligatorios |
| `side` | `BUY` o `SELL` |
| `type` | `MARKET` o `LIMIT` |
| `quantity` | Cantidad de acciones. Exactamente uno de `quantity` / `amount` |
| `amount` | Monto en pesos; se envía la máxima cantidad de acciones enteras que cubre |
| `price` | Precio límite: obligatorio en `LIMIT`, no admitido en `MARKET` |

Responde **201** con la orden registrada (`id`, `size`, `price`, `status`, `datetime`, …), también cuando queda
`REJECTED`. Request inválida → 400; usuario o instrumento inexistente → 404; instrumento `MONEDA` → 400.

### Cancelación de órdenes

```http
POST /api/orders/5/cancel
```

Responde 200 con la orden en estado `CANCELLED`. Si la orden no está `NEW` → 409; si no existe → 404.

## Modelo de cálculo

Todo se calcula a partir de la tabla `orders`, usando la columna `size`:

- **Pesos**: suman `CASH_IN` y `SELL`, restan `CASH_OUT` y `BUY` (monto = `size × price`), de órdenes `FILLED`.
- **Pesos disponibles** = pesos − monto de las compras `NEW`, que quedan reservados hasta ejecutarse o cancelarse.
- **Cantidad de cada activo** = compras − ventas `FILLED`. Las acciones de ventas `NEW` quedan reservadas y no se
  pueden volver a vender.
- **Valor de mercado** de una posición = cantidad × último precio (`close` de la cotización más reciente).
- **Rendimiento total (%)** = (valor de mercado − costo) / costo, con costo promedio ponderado: las compras suman su
  monto al costo y las ventas lo reducen en proporción a las acciones vendidas.
- **Rendimiento diario (%)** = (`close` − `previousClose`) / `previousClose`.
- **Valor total de la cuenta** = pesos (incluidos los reservados, que siguen siendo del usuario) + valor de mercado de
  las posiciones.
- `REJECTED` y `CANCELLED` no afectan ningún cálculo.

## Supuestos y decisiones de diseño

### Órdenes

- **Precio**: `MARKET` usa el último `close`; `LIMIT` usa el precio límite, también para calcular la cantidad cuando
  la orden se envía por monto.
- **Estado inicial**: `MARKET` → `FILLED`; `LIMIT` → `NEW`. Las órdenes `LIMIT` no se ejecutan nunca, ya que el
  challenge indica que no hace falta simular el mercado.
- **Rechazo** (`REJECTED`, la orden se registra igual y se responde 201):
  - compra por un monto mayor a los pesos disponibles (igualarlos está permitido);
  - venta de más acciones que las disponibles;
  - orden por monto que no alcanza para una acción (`size` 0);
  - orden `MARKET` sobre un instrumento sin cotizaciones (`PGR`, `IRCP`), que se registra con `price` nulo.
- **Errores de request vs. rechazo**: una request mal formada (campos faltantes, `quantity` y `amount` juntos, `LIMIT`
  sin precio, etc.) no es una orden, por lo que responde 400 y no se registra. Lo mismo para un `side` `CASH_IN` /
  `CASH_OUT` o un instrumento `MONEDA`: las transferencias de pesos se modelan como órdenes, pero no se envían por
  este endpoint.
- **Concurrencia**: al enviar una orden se bloquea la fila del usuario (`SELECT … FOR UPDATE`) hasta el commit, de
  modo que la validación de fondos y el alta de la orden son atómicas: dos órdenes simultáneas no pueden gastar los
  mismos pesos o acciones. Al cancelar se bloquea la fila de la orden.
- **Cancelación**: se modela como acción (`POST …/cancel`) y no como `DELETE`, porque la orden no se borra sino que
  cambia de estado y queda en el historial.
- **Fecha**: `datetime` se registra con la hora local del servidor, como los datos existentes (columna `timestamp`
  sin zona horaria).

### Portfolio

- Se listan solo posiciones con cantidad distinta de 0, ordenadas por ticker. No incluye los pesos como posición:
  se informan en `availableCash` y forman parte de `totalValue`.
- Si un activo no tiene cotización, `lastPrice`, `marketValue` y los rendimientos son `null` y la posición no suma al
  total.
- **Inconsistencia en los datos provistos**: el usuario 1 compró 20 BMA y vendió 30 (ambas `FILLED`), por lo que su
  posición es **−10**. Se muestra tal cual, con valor de mercado negativo y sin rendimiento total. Las validaciones de
  la API impiden generar nuevas posiciones negativas.
- Montos y porcentajes con 2 decimales y redondeo `HALF_UP`.
- El cálculo se hace en memoria sobre las órdenes del usuario (una consulta indexada), más una consulta para los
  últimos precios. Es simple y suficiente para el volumen esperado por usuario; con historiales muy grandes se podría
  agregar en SQL o mantener una tabla de posiciones.

### Búsqueda

- Un único parámetro `query`, buscado como substring sin distinguir mayúsculas en ticker **o** nombre.
- Excluye instrumentos `MONEDA` (el cash no es un activo operable).
- Los comodines de `LIKE` (`%`, `_`) del texto buscado se escapan.
- Sin paginación: hay 66 instrumentos.

### Otros

- **Sin autenticación**, como indica el challenge: el usuario se indica en la URL o en el body, y no se valida que
  quien cancela una orden sea su dueño.
- **Montos** con `BigDecimal` en todo el cálculo; nunca `double`.
- **OpenAPI (API-first)**: el contrato se escribe a mano y el código se genera de él, y no al revés, para que la API
  se diseñe y revise antes de implementarla y no pueda divergir de su documentación. Se generan solo interfaces
  (`interfaceOnly`) y DTOs como records (sufijo `Dto`, para no chocar con las entidades y enums del dominio); los
  controllers convierten entre DTOs y dominio, de modo que servicios y entidades no dependen del código generado.
- **Mappers con MapStruct** (`OrderMapper`, `InstrumentMapper`, `PortfolioMapper`): la conversión se genera al compilar,
  sin reflexión. Está configurado para que un campo del DTO sin origen sea un error de compilación, y los enums se
  convierten por nombre con chequeo en compilación: un cambio en el contrato que el dominio no contempla rompe el
  build en lugar de llegar a producción.
- **Lombok, acotado**: en las entidades JPA solo `@Getter` y `@NoArgsConstructor(access = PROTECTED)`. Sin setters
  (el estado cambia solo por métodos de negocio, como `Order.cancel()`), ni `@Data`, `@EqualsAndHashCode` o
  `@ToString`, que en entidades JPA comparan o recorren relaciones `LAZY`.
- **Swagger UI sin springdoc**: springdoc solo sirve la UI si además genera su propio spec a partir del código
  (`/v3/api-docs`), lo que dejaría dos contratos que pueden divergir. En su lugar, una página estática
  (`static/swagger-ui.html`) carga Swagger UI desde su WebJar y muestra `/openapi.yaml`. El contrato declara un
  servidor relativo (`/`), para que "Try it out" funcione en cualquier host y puerto.
- **Errores sin HTTP en el dominio**: `application` y `domain` lanzan excepciones propias (`NotFoundException`,
  `InstrumentNotTradeableException`, `OrderNotCancellableException`) y la capa `api` las traduce a 404, 400 y 409
  (`ApiExceptionHandler`, `OrderExceptionHandler`). La regla de que solo se cancelan órdenes `NEW` vive en
  `Order.cancel()`.
- **Validaciones que el contrato no expresa** (exactamente uno de `quantity` / `amount`; `price` solo en `LIMIT`; 2
  decimales en montos) se agregan al DTO generado con las extensiones `x-class-extra-annotation` y
  `x-field-extra-annotation`. Un valor que no se puede convertir al tipo del campo (por ejemplo un `side` fuera de
  `BUY` / `SELL`) se informa como error de ese campo.
- **Base de tests**: Testcontainers con PostgreSQL real, no H2, para probar el mismo motor que producción (bloqueos,
  `numeric`, `LIKE`).

## Cambios en la base de datos

El esquema se versiona con Flyway (`src/main/resources/db/migration`). Sobre la base existente, Flyway registra el
esquema original como *baseline* (versión 1) y aplica solo las migraciones posteriores.

| Versión | Cambio | Motivo |
|---|---|---|
| V1 | Esquema original | No se ejecuta sobre la base existente; permite recrear el esquema en una base vacía, como en los tests. |
| V2 | Índice `orders (userid, status)` | El portfolio y la validación de órdenes leen las órdenes de un usuario por estado (`FILLED` / `NEW`). Antes solo existían índices de PK. |
| V2 | Índice único `marketdata (instrumentid, date)` | Búsqueda del último precio de un instrumento; además garantiza una sola cotización por instrumento y día. |

## Estructura

Paquetes por funcionalidad (`instrument`, `marketdata`, `order`, `portfolio`, `user`) y, dentro de cada uno, por capa:

```
com.cocos.challenge
├── openapi/          (generado) Interfaces y DTOs del contrato OpenAPI
├── common/           Excepciones comunes y su traducción a errores HTTP
└── <funcionalidad>/
    ├── api/          Controllers, mappers y validación del request
    ├── application/  Servicios (casos de uso)
    ├── domain/       Entidades, enums y lógica de negocio
    └── persistence/  Repositorios
```

Las dependencias van hacia abajo (`api` → `application` → `persistence` → `domain`). Las entidades JPA están en
`domain` porque son también el modelo de dominio (por ejemplo `Order.cancel()`), y separarlas exigiría duplicar el
modelo.

## Posibles mejoras

- Registrar el motivo de rechazo de una orden y la fecha de cancelación (requiere columnas nuevas).
- Endpoints para consultar órdenes (`GET /api/orders/{id}`, órdenes de un usuario) y para transferencias de pesos.
- Búsqueda con índice GIN (`pg_trgm`) y paginación si crece el universo de instrumentos.
- Autenticación, para que cada usuario solo opere y consulte su propia cuenta.
