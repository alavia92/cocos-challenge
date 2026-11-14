-- Portfolio y validación de órdenes: se leen las órdenes de un usuario filtrando por estado (FILLED / NEW).
CREATE INDEX idx_orders_user_status ON orders (userid, status);

-- Último precio de un instrumento (mayor fecha). Además, impide cargar dos cotizaciones del mismo día.
CREATE UNIQUE INDEX ux_marketdata_instrument_date ON marketdata (instrumentid, date);
