-- Esquema original provisto por el challenge.
-- En la base existente no se ejecuta: Flyway la registra como baseline (versión 1).
-- Se versiona para poder recrear el esquema en una base vacía (por ejemplo, en los tests).

CREATE TABLE users (
    id            SERIAL PRIMARY KEY,
    email         VARCHAR(255),
    accountnumber VARCHAR(20)
);

CREATE TABLE instruments (
    id     SERIAL PRIMARY KEY,
    ticker VARCHAR(10),
    name   VARCHAR(255),
    type   VARCHAR(10)
);

CREATE TABLE orders (
    id           SERIAL PRIMARY KEY,
    instrumentid INT REFERENCES instruments (id),
    userid       INT REFERENCES users (id),
    size         INT,
    price        NUMERIC(10, 2),
    type         VARCHAR(10),
    side         VARCHAR(10),
    status       VARCHAR(20),
    datetime     TIMESTAMP
);

CREATE TABLE marketdata (
    id            SERIAL PRIMARY KEY,
    instrumentid  INT REFERENCES instruments (id),
    high          NUMERIC(10, 2),
    low           NUMERIC(10, 2),
    open          NUMERIC(10, 2),
    close         NUMERIC(10, 2),
    previousclose NUMERIC(10, 2),
    date          DATE
);
