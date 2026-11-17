# Plan de implementación

Plan para resolver el [backend-challenge](../backend-challenge.md) de forma iterativa.

> **Estado: completado.** Este documento registra el plan y las decisiones a medida que se tomaron. La documentación
> final y consolidada (API, modelo de cálculo, supuestos y decisiones) está en el [README](../README.md).
>
> Después se refactorizó el proyecto: contrato OpenAPI con código generado, MapStruct, Lombok, paquetes por capa
> (`api`, `application`, `domain`, `persistence`), excepciones propias en lugar de `ResponseStatusException`, Swagger UI
> y Docker Compose. Lo que este plan dice sobre la estructura de paquetes y los DTOs ya no aplica; el README describe el
> estado actual.

## Estado de la base de datos provista

- Tablas `users`, `instruments`, `orders`, `marketdata`, con columnas en minúscula
  (`instrumentid`, `previousclose`, `accountnumber`). `marketdata` usa una columna `date` (tipo `date`), no `datetime`.
- Solo existen índices de PK. No hay índices sobre `orders.userid`, `orders.instrumentid` ni `marketdata.instrumentid`.
- Datos: 4 usuarios, 66 instrumentos (65 `ACCIONES` + `ARS` tipo `MONEDA`, id 66), 11 órdenes (todas del usuario 1),
  `marketdata` de 2 días (2023-07-13 y 2023-07-14).
- `PGR`, `IRCP` y `ARS` no tienen `marketdata`.
- Inconsistencia: el usuario 1 compró 20 del instrumento 31 y vendió 30 (`FILLED`), por lo que la posición calculada queda en **−10**.

## Arquitectura

- Paquetes por feature: `instrument`, `marketdata`, `order`, `portfolio`, `user`, `common` (errores, utilidades).
  Cada uno con controller → service → repository y DTOs.
- Montos con `BigDecimal` y redondeo explícito.
- Errores con `ProblemDetail`:
  - 400: validación de la request.
  - 404: usuario, instrumento u orden inexistente.
  - Orden que no cumple los requerimientos de negocio: se persiste como `REJECTED` y se responde 201 con ese estado.

### Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/users/{userId}/portfolio` | Valor total, pesos disponibles y posiciones |
| `GET` | `/api/instruments?query=...` | Búsqueda por ticker y/o nombre (case-insensitive) |
| `POST` | `/api/orders` | Enviar orden (`quantity` **o** `amount`; `price` solo LIMIT) |
| `POST` | `/api/orders/{id}/cancel` | Cancelar una orden `NEW` |

## Decisiones de diseño

1. **Pesos disponibles** = cash de órdenes `FILLED` (`CASH_IN`, `CASH_OUT`, `BUY`, `SELL`) − cash reservado por órdenes
   `BUY` LIMIT en estado `NEW`. Para vender se descuentan además las acciones reservadas en órdenes `SELL` `NEW`.
   Evita gastar dos veces el mismo saldo. Ejemplo usuario 1: cash $753.000, reservado $125.500, disponible $627.500.
2. **Rendimiento de cada posición** (portfolio): rendimiento total = (valor de mercado − costo) / costo, con costo
   promedio ponderado de las compras `FILLED`. Se agrega además el retorno diario (`close` vs `previousClose`) como campo aparte.
3. **Orden por monto**: cantidad = `floor(monto / precio)`. Si da 0, la orden queda `REJECTED`.
4. **Orden MARKET sobre instrumento sin `marketdata`**: `REJECTED`.
5. **Órdenes LIMIT**: quedan en `NEW`; no se simula el matching del mercado.
6. **Concurrencia**: al enviar una orden se bloquea la fila del usuario (`SELECT … FOR UPDATE`) para que dos órdenes
   simultáneas no puedan consumir el mismo saldo.
7. **Posiciones negativas** de los datos de origen se muestran tal cual y se documentan; las validaciones nuevas evitan generarlas.
8. **Transferencias** (`CASH_IN` / `CASH_OUT`): se consideran en los cálculos, sin endpoint propio (fuera del alcance pedido).
9. **Cambios de esquema** con Flyway en modo `baselineOnMigrate` sobre la base existente, versionados y justificados en el README.
10. **Base de pruebas**: Testcontainers (PostgreSQL real con el mismo esquema y datos semilla). Nunca contra Neon.
11. **Búsqueda de instrumentos**: un único parámetro `query` que se busca como substring, sin distinguir mayúsculas,
    en ticker **o** nombre. Excluye instrumentos `MONEDA` (el cash no es un activo operable). Los comodines de `LIKE`
    (`%`, `_`) se escapan. Resultados ordenados por ticker, sin paginación (66 instrumentos). Con un universo mayor,
    se agregaría un índice GIN con `pg_trgm` y paginación.
12. **Portfolio**:
    - `totalValue` = pesos según órdenes `FILLED` (incluye los reservados por órdenes `NEW`, que siguen siendo del
      usuario) + valor de mercado de las posiciones. `availableCash` = pesos − reservados.
    - Costo promedio ponderado: las compras suman su monto al costo; las ventas lo reducen en proporción a las
      acciones vendidas. Rendimiento total = (valor de mercado − costo) / costo.
    - Posiciones con cantidad 0 no se muestran. Si un activo no tiene cotización, `lastPrice`, `marketValue` y los
      rendimientos son `null` y la posición no suma al total.
    - Una posición negativa (BMA del usuario 1) se muestra con su valor negativo y sin rendimiento total.
    - Montos y porcentajes con 2 decimales, redondeo `HALF_UP`. El cálculo se hace en memoria sobre las órdenes del
      usuario (índice `orders (userid, status)`): simple y suficiente para el volumen esperado por usuario.
13. **Envío de órdenes** (`POST /api/orders`):
    - Errores de request → 400 con el detalle por campo en `errors`: falta un campo obligatorio, `side` distinto de
      `BUY`/`SELL`, no se envía exactamente uno de `quantity`/`amount`, `price` ausente en `LIMIT` o presente en
      `MARKET`, valores no positivos o con más de 2 decimales. Usuario o instrumento inexistente → 404. Instrumento
      `MONEDA` → 400 (los pesos no se compran ni venden; las transferencias son `CASH_IN`/`CASH_OUT`).
    - Una orden válida se registra siempre y se responde 201, incluso si queda `REJECTED`.
    - Precio: `MARKET` usa el último `close`; `LIMIT` usa el precio límite (también para calcular la cantidad cuando
      se envía `amount`).
    - Se rechaza (`REJECTED`) si: la compra supera los pesos disponibles (se permite igualarlos); la venta supera las
      acciones disponibles (descontando las reservadas por ventas `NEW`); la cantidad calculada por monto es 0; o es
      `MARKET` sobre un instrumento sin cotización (se registra con `price` nulo).
    - El usuario se bloquea con `SELECT … FOR UPDATE` durante la transacción; `OrderConcurrencyTests` verifica que
      dos órdenes simultáneas no puedan gastar los mismos pesos.
    - No se persiste el motivo del rechazo (el esquema no tiene columna para eso); sería una mejora posible.
14. **Cancelación de órdenes** (`POST /api/orders/{id}/cancel`):
    - Solo órdenes `NEW` → pasan a `CANCELLED` y liberan los pesos o acciones reservados. Cualquier otro estado → 409
      con el estado actual en el detalle; orden inexistente → 404.
    - Se modela como acción (`POST …/cancel`) y no como `DELETE`, porque la orden no se borra: cambia de estado y
      queda en el historial.
    - La fila de la orden se bloquea (`SELECT … FOR UPDATE`): dos cancelaciones simultáneas no pueden pasar ambas.
    - Sin autenticación (fuera del alcance), no se valida que quien cancela sea el dueño de la orden. No se registra
      la fecha de cancelación (`datetime` conserva la fecha de envío).

## Iteraciones

Cada iteración terminó con un commit.

1. **Modelo y datos de prueba**: entidades JPA validadas contra el esquema real, enums, repositories, Flyway con índices,
   infraestructura de tests con esquema y datos semilla.
2. **Búsqueda de instrumentos** + tests.
3. **Portfolio**: cálculo de cash, posiciones y rendimiento + tests unitarios del cálculo.
4. **Envío de órdenes**: MARKET/LIMIT, por cantidad o por monto, validación de fondos y tenencia, lock por usuario
   + **test funcional** requerido por el challenge.
5. **Cancelación de órdenes** + tests.
6. **Cierre**: README con supuestos y decisiones, colección `.http` con ejemplos de invocación, revisión general.
