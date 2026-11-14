package com.cocos.challenge.portfolio;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.cocos.challenge.instrument.Instrument;
import com.cocos.challenge.order.Order;

/**
 * Tenencia de un usuario calculada a partir de sus órdenes:
 * <ul>
 * <li>las órdenes {@code FILLED} determinan los pesos y la cantidad de acciones de cada activo;</li>
 * <li>las órdenes {@code NEW} reservan pesos (compras) o acciones (ventas), que dejan de estar disponibles para
 * operar;</li>
 * <li>las órdenes {@code REJECTED} y {@code CANCELLED} no tienen efecto.</li>
 * </ul>
 */
public final class Holdings {

	private BigDecimal cash = BigDecimal.ZERO;

	private BigDecimal reservedCash = BigDecimal.ZERO;

	private final Map<Integer, Position> positions = new LinkedHashMap<>();

	private Holdings() {
	}

	/**
	 * @param orders órdenes del usuario en orden cronológico (el costo promedio depende de ese orden)
	 */
	public static Holdings of(List<Order> orders) {
		Holdings holdings = new Holdings();
		orders.forEach(holdings::apply);
		return holdings;
	}

	private void apply(Order order) {
		BigDecimal amount = order.getPrice().multiply(BigDecimal.valueOf(order.getSize()));
		switch (order.getStatus()) {
			case FILLED -> applyFilled(order, amount);
			case NEW -> applyPending(order, amount);
			default -> {
			}
		}
	}

	private void applyFilled(Order order, BigDecimal amount) {
		switch (order.getSide()) {
			case CASH_IN -> cash = cash.add(amount);
			case CASH_OUT -> cash = cash.subtract(amount);
			case BUY -> {
				cash = cash.subtract(amount);
				position(order.getInstrument()).buy(order.getSize(), amount);
			}
			case SELL -> {
				cash = cash.add(amount);
				position(order.getInstrument()).sell(order.getSize());
			}
		}
	}

	private void applyPending(Order order, BigDecimal amount) {
		switch (order.getSide()) {
			case BUY -> reservedCash = reservedCash.add(amount);
			case SELL -> position(order.getInstrument()).reserve(order.getSize());
			default -> {
			}
		}
	}

	private Position position(Instrument instrument) {
		return positions.computeIfAbsent(instrument.getId(), id -> new Position(instrument));
	}

	/** Pesos según las órdenes ejecutadas, incluidos los reservados por órdenes pendientes. */
	public BigDecimal getCash() {
		return cash;
	}

	/** Pesos comprometidos en órdenes de compra pendientes ({@code NEW}). */
	public BigDecimal getReservedCash() {
		return reservedCash;
	}

	/** Pesos disponibles para operar. */
	public BigDecimal getAvailableCash() {
		return cash.subtract(reservedCash);
	}

	public Collection<Position> getPositions() {
		return positions.values();
	}

	public Optional<Position> getPosition(Integer instrumentId) {
		return Optional.ofNullable(positions.get(instrumentId));
	}

	/**
	 * Posición en un activo, con su costo calculado a costo promedio ponderado: las compras suman su monto al costo y
	 * las ventas lo reducen en proporción a las acciones vendidas (el costo promedio por acción no cambia).
	 */
	public static final class Position {

		private final Instrument instrument;

		private int quantity;

		private int reservedQuantity;

		private BigDecimal cost = BigDecimal.ZERO;

		private Position(Instrument instrument) {
			this.instrument = instrument;
		}

		private void buy(int size, BigDecimal amount) {
			quantity += size;
			cost = cost.add(amount);
		}

		private void sell(int size) {
			if (size >= quantity) {
				// Venta total, o por encima de la tenencia (solo posible con datos de origen inconsistentes)
				cost = BigDecimal.ZERO;
			}
			else {
				BigDecimal soldCost = cost.multiply(BigDecimal.valueOf(size))
					.divide(BigDecimal.valueOf(quantity), MathContext.DECIMAL64);
				cost = cost.subtract(soldCost);
			}
			quantity -= size;
		}

		private void reserve(int size) {
			reservedQuantity += size;
		}

		public Instrument getInstrument() {
			return instrument;
		}

		/** Acciones en cartera según las órdenes ejecutadas. */
		public int getQuantity() {
			return quantity;
		}

		/** Acciones comprometidas en órdenes de venta pendientes ({@code NEW}). */
		public int getReservedQuantity() {
			return reservedQuantity;
		}

		/** Acciones disponibles para vender. */
		public int getAvailableQuantity() {
			return quantity - reservedQuantity;
		}

		/** Costo total de las acciones en cartera. */
		public BigDecimal getCost() {
			return cost;
		}

	}

}
