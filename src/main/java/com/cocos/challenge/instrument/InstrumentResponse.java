package com.cocos.challenge.instrument;

public record InstrumentResponse(Integer id, String ticker, String name, InstrumentType type) {

	static InstrumentResponse from(Instrument instrument) {
		return new InstrumentResponse(instrument.getId(), instrument.getTicker(), instrument.getName(),
				instrument.getType());
	}

}
