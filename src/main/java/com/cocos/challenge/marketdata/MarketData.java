package com.cocos.challenge.marketdata;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cocos.challenge.instrument.Instrument;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Cotización diaria de un instrumento. {@code close} es el último precio.
 */
@Entity
@Table(name = "marketdata")
public class MarketData {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "instrumentid")
	private Instrument instrument;

	private BigDecimal high;

	private BigDecimal low;

	private BigDecimal open;

	private BigDecimal close;

	@Column(name = "previousclose")
	private BigDecimal previousClose;

	private LocalDate date;

	protected MarketData() {
	}

	public Integer getId() {
		return id;
	}

	public Instrument getInstrument() {
		return instrument;
	}

	public BigDecimal getHigh() {
		return high;
	}

	public BigDecimal getLow() {
		return low;
	}

	public BigDecimal getOpen() {
		return open;
	}

	public BigDecimal getClose() {
		return close;
	}

	public BigDecimal getPreviousClose() {
		return previousClose;
	}

	public LocalDate getDate() {
		return date;
	}

}
