-- Usuario 2 (sin órdenes en los datos semilla).
-- PAMP: compra 10 a 900 y 10 a 1000 (costo promedio 950), vende 5 -> quedan 15 con costo 14.250.
-- Pesos: 100.000 - 9.000 - 10.000 + 5.500 = 86.500; reservados por la compra NEW de METR: 2.000.
INSERT INTO orders (instrumentid, userid, size, price, type, side, status, datetime) VALUES
    (66, 2, 100000, 1.00, 'MARKET', 'CASH_IN', 'FILLED', '2023-07-13 10:00:00'),
    (47, 2, 10, 900.00, 'MARKET', 'BUY', 'FILLED', '2023-07-13 11:00:00'),
    (47, 2, 10, 1000.00, 'LIMIT', 'BUY', 'FILLED', '2023-07-13 12:00:00'),
    (47, 2, 5, 1100.00, 'MARKET', 'SELL', 'FILLED', '2023-07-13 13:00:00'),
    (47, 2, 3, 1200.00, 'LIMIT', 'SELL', 'NEW', '2023-07-13 14:00:00'),
    (54, 2, 10, 200.00, 'LIMIT', 'BUY', 'NEW', '2023-07-13 15:00:00'),
    (54, 2, 100, 1.00, 'LIMIT', 'BUY', 'CANCELLED', '2023-07-13 16:00:00'),
    (47, 2, 1000, 925.85, 'MARKET', 'BUY', 'REJECTED', '2023-07-13 17:00:00');
