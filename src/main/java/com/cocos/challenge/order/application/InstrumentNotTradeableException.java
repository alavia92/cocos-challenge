package com.cocos.challenge.order.application;

/**
 * Se envió una orden sobre un instrumento que no se puede comprar ni vender (una moneda).
 */
public class InstrumentNotTradeableException extends RuntimeException {

	public InstrumentNotTradeableException(String message) {
		super(message);
	}

}
