package com.cocos.challenge.order.api;

import com.cocos.challenge.openapi.model.OrderDto;
import com.cocos.challenge.openapi.model.OrderRequestDto;
import com.cocos.challenge.order.application.OrderCommand;
import com.cocos.challenge.order.domain.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface OrderMapper {

	OrderCommand toCommand(OrderRequestDto request);

	@Mapping(target = "userId", source = "user.id")
	@Mapping(target = "instrumentId", source = "instrument.id")
	@Mapping(target = "ticker", source = "instrument.ticker")
	OrderDto toDto(Order order);

}
