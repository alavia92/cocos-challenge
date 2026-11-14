# Plan de implementación

Plan para resolver el [backend-challenge](../backend-challenge.md) de forma iterativa.

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

## Iteraciones

Cada iteración termina con un commit.

1. **Modelo y datos de prueba**: entidades JPA validadas contra el esquema real, enums, repositories, Flyway con índices,
   infraestructura de tests con esquema y datos semilla.
2. **Búsqueda de instrumentos** + tests.
3. **Portfolio**: cálculo de cash, posiciones y rendimiento + tests unitarios del cálculo.
4. **Envío de órdenes**: MARKET/LIMIT, por cantidad o por monto, validación de fondos y tenencia, lock por usuario
   + **test funcional** requerido por el challenge.
5. **Cancelación de órdenes** + tests.
6. **Cierre**: README con supuestos y decisiones, colección `.http` con ejemplos de invocación, revisión general.
