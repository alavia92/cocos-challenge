package com.cocos.challenge.portfolio;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortfolioController {

	private final PortfolioService portfolioService;

	public PortfolioController(PortfolioService portfolioService) {
		this.portfolioService = portfolioService;
	}

	@GetMapping("/api/users/{userId}/portfolio")
	public PortfolioResponse getPortfolio(@PathVariable Integer userId) {
		return portfolioService.getPortfolio(userId);
	}

}
