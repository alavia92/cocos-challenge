package com.cocos.challenge.order;

import com.cocos.challenge.api.OrdersApi;
import com.cocos.challenge.api.model.OrderDto;
import com.cocos.challenge.api.model.OrderRequestDto;
import com.cocos.challenge.api.model.OrderSideDto;
import com.cocos.challenge.api.model.OrderStatusDto;
import com.cocos.challenge.api.model.OrderTypeDto;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController implements OrdersApi {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	/**
	 * Envía una orden. Se registra siempre (201), también cuando es rechazada: el resultado se informa en
	 * {@code status}.
	 */
	@Override
	public ResponseEntity<OrderDto> submitOrder(OrderRequestDto request) {
		OrderCommand command = new OrderCommand(request.userId(), request.instrumentId(),
				OrderSide.valueOf(request.side().name()), OrderType.valueOf(request.type().name()), request.quantity(),
				request.amount(), request.price());
		return ResponseEntity.status(HttpStatus.CREATED).body(toDto(orderService.submit(command)));
	}

	/**
	 * Cancela una orden. Solo se pueden cancelar las órdenes {@code NEW}; para cualquier otro estado responde 409.
	 */
	@Override
	public ResponseEntity<OrderDto> cancelOrder(Integer orderId) {
		return ResponseEntity.ok(toDto(orderService.cancel(orderId)));
	}

	private static OrderDto toDto(Order order) {
		return new OrderDto(order.getId(), order.getUser().getId(), order.getInstrument().getId(),
				order.getInstrument().getTicker(), OrderSideDto.valueOf(order.getSide().name()),
				OrderTypeDto.valueOf(order.getType().name()), order.getSize(), order.getPrice(),
				OrderStatusDto.valueOf(order.getStatus().name()), order.getDatetime());
	}

}
