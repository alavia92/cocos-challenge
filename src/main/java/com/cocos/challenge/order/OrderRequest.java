package com.cocos.challenge.order;

import java.math.BigDecimal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Orden enviada al mercado. El tamaño se indica con {@code quantity} (acciones) o con {@code amount} (monto en pesos
 * a invertir), uno de los dos.
 *
 * @param quantity cantidad exacta de acciones
 * @param amount monto en pesos; se envía la cantidad máxima de acciones enteras que cubre
 * @param price precio límite; obligatorio para órdenes {@code LIMIT} y no admitido en {@code MARKET}
 */
public record OrderRequest(@NotNull Integer userId, @NotNull Integer instrumentId, @NotNull OrderSide side,
		@NotNull OrderType type, @Positive Integer quantity, @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
		@Positive @Digits(integer = 8, fraction = 2) BigDecimal price) {

	@AssertTrue(message = "must be BUY or SELL")
	public boolean isTradeSide() {
		return side == null || side == OrderSide.BUY || side == OrderSide.SELL;
	}

	@AssertTrue(message = "exactly one of quantity or amount must be provided")
	public boolean isSizeSpecified() {
		return (quantity == null) != (amount == null);
	}

	@AssertTrue(message = "price is required for LIMIT orders and not allowed for MARKET orders")
	public boolean isPriceConsistentWithType() {
		return type == null || (type == OrderType.LIMIT) == (price != null);
	}

}
