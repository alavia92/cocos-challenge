package com.cocos.challenge.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(Integer id, Integer userId, Integer instrumentId, String ticker, OrderSide side,
		OrderType type, Integer size, BigDecimal price, OrderStatus status, LocalDateTime datetime) {

	static OrderResponse from(Order order) {
		return new OrderResponse(order.getId(), order.getUser().getId(), order.getInstrument().getId(),
				order.getInstrument().getTicker(), order.getSide(), order.getType(), order.getSize(), order.getPrice(),
				order.getStatus(), order.getDatetime());
	}

}
