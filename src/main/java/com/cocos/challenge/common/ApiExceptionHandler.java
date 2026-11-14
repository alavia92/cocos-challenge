package com.cocos.challenge.common;

import java.util.List;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Errores de la API en formato RFC 9457 ({@code application/problem+json}). Los errores de validación incluyen el
 * detalle de cada campo en la propiedad {@code errors}.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<String> errors = ex.getBindingResult()
			.getAllErrors()
			.stream()
			.map(error -> (error instanceof FieldError fieldError)
					? fieldError.getField() + ": " + fieldError.getDefaultMessage() : error.getDefaultMessage())
			.toList();
		return handleExceptionInternal(ex, invalidRequest(ex.getBody(), errors), headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<String> errors = ex.getParameterValidationResults()
			.stream()
			.flatMap(result -> result.getResolvableErrors()
				.stream()
				.map(MessageSourceResolvable::getDefaultMessage)
				.map(message -> result.getMethodParameter().getParameterName() + ": " + message))
			.toList();
		return handleExceptionInternal(ex, invalidRequest(ex.getBody(), errors), headers, status, request);
	}

	private static ProblemDetail invalidRequest(ProblemDetail body, List<String> errors) {
		body.setDetail("Invalid request");
		body.setProperty("errors", errors);
		return body;
	}

}
