package com.cocos.challenge.order.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Reglas de una orden que dependen de más de un campo y no se pueden expresar en el contrato OpenAPI. Se aplica al DTO
 * generado mediante {@code x-class-extra-annotation}.
 *
 * @see OrderRequestValidator
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = OrderRequestValidator.class)
public @interface ValidOrderRequest {

	String message() default "invalid order";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
