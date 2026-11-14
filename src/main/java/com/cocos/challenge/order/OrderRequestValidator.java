package com.cocos.challenge.order;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.cocos.challenge.api.model.OrderRequestDto;
import com.cocos.challenge.api.model.OrderTypeDto;

/**
 * Valida que se indique exactamente uno de {@code quantity} / {@code amount}, y que {@code price} se envíe solo en
 * órdenes {@code LIMIT}.
 */
class OrderRequestValidator implements ConstraintValidator<ValidOrderRequest, OrderRequestDto> {

	@Override
	public boolean isValid(OrderRequestDto request, ConstraintValidatorContext context) {
		if (request == null) {
			return true;
		}
		context.disableDefaultConstraintViolation();
		boolean valid = true;
		if ((request.quantity() == null) == (request.amount() == null)) {
			context.buildConstraintViolationWithTemplate("exactly one of quantity or amount must be provided")
				.addConstraintViolation();
			valid = false;
		}
		if (request.type() != null && (request.type() == OrderTypeDto.LIMIT) != (request.price() != null)) {
			context.buildConstraintViolationWithTemplate("required for LIMIT orders and not allowed for MARKET orders")
				.addPropertyNode("price")
				.addConstraintViolation();
			valid = false;
		}
		return valid;
	}

}
