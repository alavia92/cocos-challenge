package com.cocos.challenge.portfolio;

import com.cocos.challenge.api.PortfolioApi;
import com.cocos.challenge.api.model.PortfolioDto;
import com.cocos.challenge.api.model.PositionDto;
import com.cocos.challenge.portfolio.Portfolio.PositionValue;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortfolioController implements PortfolioApi {

	private final PortfolioService portfolioService;

	public PortfolioController(PortfolioService portfolioService) {
		this.portfolioService = portfolioService;
	}

	@Override
	public ResponseEntity<PortfolioDto> getPortfolio(Integer userId) {
		Portfolio portfolio = portfolioService.getPortfolio(userId);
		return ResponseEntity.ok(new PortfolioDto(portfolio.userId(), portfolio.accountNumber(),
				portfolio.totalValue(), portfolio.availableCash(),
				portfolio.positions().stream().map(PortfolioController::toDto).toList()));
	}

	private static PositionDto toDto(PositionValue position) {
		return new PositionDto(position.instrumentId(), position.ticker(), position.name(), position.quantity(),
				position.lastPrice(), position.marketValue(), position.totalReturnPercent(),
				position.dailyReturnPercent());
	}

}
