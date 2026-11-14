package com.cocos.challenge.order;

public enum OrderType {

	/** Se ejecuta inmediatamente al último precio ({@code close}). */
	MARKET,

	/** Se envía con un precio límite y queda pendiente ({@code NEW}). */
	LIMIT

}
