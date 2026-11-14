package com.cocos.challenge.instrument;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "instruments")
public class Instrument {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String ticker;

	private String name;

	@Enumerated(EnumType.STRING)
	private InstrumentType type;

	protected Instrument() {
	}

	public Integer getId() {
		return id;
	}

	public String getTicker() {
		return ticker;
	}

	public String getName() {
		return name;
	}

	public InstrumentType getType() {
		return type;
	}

}
