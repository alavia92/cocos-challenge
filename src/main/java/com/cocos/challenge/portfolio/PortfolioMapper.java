package com.cocos.challenge.portfolio;

import com.cocos.challenge.api.model.PortfolioDto;
import com.cocos.challenge.api.model.PositionDto;
import com.cocos.challenge.portfolio.Portfolio.PositionValue;
import org.mapstruct.Mapper;

@Mapper
interface PortfolioMapper {

	PortfolioDto toDto(Portfolio portfolio);

	PositionDto toDto(PositionValue position);

}
