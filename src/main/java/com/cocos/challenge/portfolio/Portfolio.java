package com.cocos.challenge.portfolio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Portfolio de un usuario valuado a los últimos precios.
 *
 * @param totalValue valor total de la cuenta: pesos (incluidos los reservados por órdenes pendientes) más el valor de
 * mercado de las posiciones
 * @param availableCash pesos disponibles para operar
 */
public record Portfolio(Integer userId, String accountNumber, BigDecimal totalValue, BigDecimal availableCash,
		List<PositionValue> positions) {

	/**
	 * @param lastPrice último precio ({@code close}); {@code null} si el activo no tiene cotizaciones
	 * @param marketValue valor de la posición al último precio; {@code null} si no hay cotización
	 * @param totalReturnPercent rendimiento total respecto del costo promedio de compra; {@code null} si no se puede
	 * calcular (sin cotización o posición inconsistente)
	 * @param dailyReturnPercent variación del día ({@code close} vs {@code previousClose}); {@code null} si no hay
	 * cotización
	 */
	public record PositionValue(Integer instrumentId, String ticker, String name, int quantity,
			BigDecimal lastPrice, BigDecimal marketValue, BigDecimal totalReturnPercent,
			BigDecimal dailyReturnPercent) {

	}

}
