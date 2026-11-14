package com.cocos.challenge.order;

import com.cocos.challenge.api.model.OrderDto;
import com.cocos.challenge.api.model.OrderRequestDto;
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
