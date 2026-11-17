package com.cocos.challenge.order.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.cocos.challenge.instrument.domain.Instrument;
import com.cocos.challenge.user.domain.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Orden de compra/venta de un instrumento o transferencia de pesos ({@code CASH_IN} / {@code CASH_OUT}).
 * Las tenencias y el cash del usuario se calculan a partir de estas órdenes.
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "instrumentid")
	private Instrument instrument;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "userid")
	private User user;

	private Integer size;

	private BigDecimal price;

	@Enumerated(EnumType.STRING)
	private OrderType type;

	@Enumerated(EnumType.STRING)
	private OrderSide side;

	@Enumerated(EnumType.STRING)
	private OrderStatus status;

	private LocalDateTime datetime;

	public Order(User user, Instrument instrument, OrderSide side, OrderType type, int size, BigDecimal price,
			OrderStatus status, LocalDateTime datetime) {
		this.user = user;
		this.instrument = instrument;
		this.side = side;
		this.type = type;
		this.size = size;
		this.price = price;
		this.status = status;
		this.datetime = datetime;
	}

	/**
	 * Cancela la orden.
	 *
	 * @throws OrderNotCancellableException si la orden no está pendiente ({@code NEW})
	 */
	public void cancel() {
		if (status != OrderStatus.NEW) {
			throw new OrderNotCancellableException(
					"Order %d is %s; only NEW orders can be cancelled".formatted(id, status));
		}
		status = OrderStatus.CANCELLED;
	}

}
