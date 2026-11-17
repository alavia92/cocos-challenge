package com.cocos.challenge.order.api;

import com.cocos.challenge.order.application.InstrumentNotTradeableException;
import com.cocos.challenge.order.domain.OrderNotCancellableException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Errores propios de las órdenes, en formato RFC 9457.
 */
@RestControllerAdvice
class OrderExceptionHandler {

	@ExceptionHandler
	ProblemDetail handleInstrumentNotTradeable(InstrumentNotTradeableException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler
	ProblemDetail handleOrderNotCancellable(OrderNotCancellableException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

}
