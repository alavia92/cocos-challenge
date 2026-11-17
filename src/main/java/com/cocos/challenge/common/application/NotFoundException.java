package com.cocos.challenge.common.application;

/**
 * El recurso pedido (usuario, instrumento, orden) no existe.
 */
public class NotFoundException extends RuntimeException {

	public NotFoundException(String message) {
		super(message);
	}

}
