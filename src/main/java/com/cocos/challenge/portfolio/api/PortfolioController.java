package com.cocos.challenge.portfolio.api;

import com.cocos.challenge.openapi.PortfolioApi;
import com.cocos.challenge.openapi.model.PortfolioDto;
import com.cocos.challenge.portfolio.application.PortfolioService;

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
