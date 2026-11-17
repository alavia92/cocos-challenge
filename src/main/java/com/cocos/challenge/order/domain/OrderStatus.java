package com.cocos.challenge.order.domain;

public enum OrderStatus {

	/** Orden límite enviada al mercado, pendiente de ejecución. */
	NEW,

	FILLED,

	/** No cumple los requerimientos (por ejemplo, fondos o tenencia insuficientes). */
	REJECTED,

	/** Cancelada por el usuario; solo aplica a órdenes {@code NEW}. */
	CANCELLED

}
