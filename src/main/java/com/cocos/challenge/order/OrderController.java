package com.cocos.challenge.order;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	/**
	 * Envía una orden. Se registra siempre (201), también cuando es rechazada: el resultado se informa en
	 * {@code status}.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OrderResponse submit(@Valid @RequestBody OrderRequest request) {
		return OrderResponse.from(orderService.submit(request));
	}

}
