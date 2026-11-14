package com.cocos.challenge.order;

import java.math.BigDecimal;

/**
 * Orden a enviar al mercado, ya validada. El tamaño se indica con {@code quantity} (acciones) o con {@code amount}
 * (monto en pesos a invertir), uno de los dos.
 *
 * @param quantity cantidad exacta de acciones
 * @param amount monto en pesos; se envía la cantidad máxima de acciones enteras que cubre
 * @param price precio límite; solo en órdenes {@code LIMIT}
 */
public record OrderCommand(Integer userId, Integer instrumentId, OrderSide side, OrderType type, Integer quantity,
		BigDecimal amount, BigDecimal price) {

}
