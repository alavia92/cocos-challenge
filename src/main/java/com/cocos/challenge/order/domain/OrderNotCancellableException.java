package com.cocos.challenge.order.domain;

/**
 * Se intentó cancelar una orden que no está pendiente ({@code NEW}).
 */
public class OrderNotCancellableException extends RuntimeException {

	public OrderNotCancellableException(String message) {
		super(message);
	}

}
