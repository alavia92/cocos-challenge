package com.cocos.challenge.portfolio;

import com.cocos.challenge.api.PortfolioApi;
import com.cocos.challenge.api.model.PortfolioDto;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortfolioController implements PortfolioApi {

	private final PortfolioService portfolioService;

	private final PortfolioMapper portfolioMapper;

	public PortfolioController(PortfolioService portfolioService, PortfolioMapper portfolioMapper) {
		this.portfolioService = portfolioService;
		this.portfolioMapper = portfolioMapper;
	}

	@Override
	public ResponseEntity<PortfolioDto> getPortfolio(Integer userId) {
		return ResponseEntity.ok(portfolioMapper.toDto(portfolioService.getPortfolio(userId)));
	}

}
