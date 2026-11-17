package com.cocos.challenge.portfolio.api;

import com.cocos.challenge.openapi.model.PortfolioDto;
import com.cocos.challenge.openapi.model.PositionDto;
import com.cocos.challenge.portfolio.domain.Portfolio;
import com.cocos.challenge.portfolio.domain.Portfolio.PositionValue;
import org.mapstruct.Mapper;

@Mapper
interface PortfolioMapper {

	PortfolioDto toDto(Portfolio portfolio);

	PositionDto toDto(PositionValue position);

}
