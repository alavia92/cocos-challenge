package com.cocos.challenge.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.cocos.challenge.instrument.Instrument;
import com.cocos.challenge.user.User;
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

/**
 * Orden de compra/venta de un instrumento o transferencia de pesos ({@code CASH_IN} / {@code CASH_OUT}).
 * Las tenencias y el cash del usuario se calculan a partir de estas órdenes.
 */
@Entity
@Table(name = "orders")
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

	protected Order() {
	}

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
	 * Cancela la orden. Solo se pueden cancelar las órdenes pendientes ({@code NEW}).
	 */
	public void cancel() {
		if (status != OrderStatus.NEW) {
			throw new IllegalStateException("Order %d is %s; only NEW orders can be cancelled".formatted(id, status));
		}
		status = OrderStatus.CANCELLED;
	}

	public Integer getId() {
		return id;
	}

	public Instrument getInstrument() {
		return instrument;
	}

	public User getUser() {
		return user;
	}

	public Integer getSize() {
		return size;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public OrderType getType() {
		return type;
	}

	public OrderSide getSide() {
		return side;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public LocalDateTime getDatetime() {
		return datetime;
	}

}
