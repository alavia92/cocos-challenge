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
