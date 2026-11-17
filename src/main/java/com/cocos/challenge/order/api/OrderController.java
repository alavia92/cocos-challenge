package com.cocos.challenge.order.api;

import com.cocos.challenge.openapi.OrdersApi;
import com.cocos.challenge.openapi.model.OrderDto;
import com.cocos.challenge.openapi.model.OrderRequestDto;
import com.cocos.challenge.order.application.OrderService;
import com.cocos.challenge.order.domain.Order;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController implements OrdersApi {

	private final OrderService orderService;

	private final OrderMapper orderMapper;

	public OrderController(OrderService orderService, OrderMapper orderMapper) {
		this.orderService = orderService;
		this.orderMapper = orderMapper;
	}

	/**
	 * Envía una orden. Se registra siempre (201), también cuando es rechazada: el resultado se informa en
	 * {@code status}.
	 */
	@Override
	public ResponseEntity<OrderDto> submitOrder(OrderRequestDto request) {
		Order order = orderService.submit(orderMapper.toCommand(request));
		return ResponseEntity.status(HttpStatus.CREATED).body(orderMapper.toDto(order));
	}

	/**
	 * Cancela una orden. Solo se pueden cancelar las órdenes {@code NEW}; para cualquier otro estado responde 409.
	 */
	@Override
	public ResponseEntity<OrderDto> cancelOrder(Integer orderId) {
		return ResponseEntity.ok(orderMapper.toDto(orderService.cancel(orderId)));
	}

}
