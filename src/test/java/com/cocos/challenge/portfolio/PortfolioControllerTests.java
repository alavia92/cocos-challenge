package com.cocos.challenge.portfolio;

import com.cocos.challenge.IntegrationTest;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class PortfolioControllerTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private PortfolioService portfolioService;

	/**
	 * Valores esperados para los datos semilla del usuario 1:
	 * <ul>
	 * <li>Pesos: 1.000.000 - 46.500 + 9.400 - 100.000 - 30.800 - 125.000 + 45.900 = 753.000; reservados por compras
	 * NEW: 35.500 + 90.000 = 125.500; disponibles: 627.500.</li>
	 * <li>PAMP: 40 acciones (compra 50 a 930, vende 10) a 925,85 = 37.034; costo 37.200 -> -0,45%.</li>
	 * <li>METR: 500 acciones a 229,50 = 114.750; costo 125.000 -> -8,20%.</li>
	 * <li>BMA: compra 20 y vende 30 en los datos de origen -> -10 acciones a 1.502,80 = -15.028; sin rendimiento.</li>
	 * <li>Total: 753.000 + 37.034 + 114.750 - 15.028 = 889.756.</li>
	 * </ul>
	 */
	@Test
	void returnsPortfolioFromSeedData() {
		assertThat(mvc.get().uri("/api/users/1/portfolio")).hasStatusOk().bodyJson().isStrictlyEqualTo("""
				{
				  "userId": 1,
				  "accountNumber": "10001",
				  "totalValue": 889756.00,
				  "availableCash": 627500.00,
				  "positions": [
				    {"instrumentId": 31, "ticker": "BMA", "name": "Banco Macro S.A.", "quantity": -10,
				     "lastPrice": 1502.80, "marketValue": -15028.00,
				     "totalReturnPercent": null, "dailyReturnPercent": -1.15},
				    {"instrumentId": 54, "ticker": "METR", "name": "MetroGAS S.A.", "quantity": 500,
				     "lastPrice": 229.50, "marketValue": 114750.00,
				     "totalReturnPercent": -8.20, "dailyReturnPercent": -1.08},
				    {"instrumentId": 47, "ticker": "PAMP", "name": "Pampa Holding S.A.", "quantity": 40,
				     "lastPrice": 925.85, "marketValue": 37034.00,
				     "totalReturnPercent": -0.45, "dailyReturnPercent": 0.44}
				  ]
				}""");
	}

	@Test
	void returnsEmptyPortfolioForUserWithoutOrders() {
		assertThat(mvc.get().uri("/api/users/3/portfolio")).hasStatusOk().bodyJson().isStrictlyEqualTo("""
				{"userId": 3, "accountNumber": "10003", "totalValue": 0.00, "availableCash": 0.00, "positions": []}""");
	}

	/**
	 * Ver el detalle del escenario en {@code portfolio/user-2-orders.sql}. PAMP: 15 acciones a 925,85 = 13.887,75;
	 * costo 14.250 -> -2,54%. Total: 86.500 + 13.887,75 = 100.387,75; disponibles: 86.500 - 2.000 = 84.500.
	 */
	@Test
	@Sql("/portfolio/user-2-orders.sql")
	void usesAverageCostAndExcludesReservedCash() {
		assertThat(mvc.get().uri("/api/users/2/portfolio")).hasStatusOk().bodyJson().isStrictlyEqualTo("""
				{
				  "userId": 2,
				  "accountNumber": "10002",
				  "totalValue": 100387.75,
				  "availableCash": 84500.00,
				  "positions": [
				    {"instrumentId": 47, "ticker": "PAMP", "name": "Pampa Holding S.A.", "quantity": 15,
				     "lastPrice": 925.85, "marketValue": 13887.75,
				     "totalReturnPercent": -2.54, "dailyReturnPercent": 0.44}
				  ]
				}""");
	}

	@Test
	@Sql("/portfolio/user-2-orders.sql")
	void reservesSharesOfPendingSellOrders() {
		Holdings.Position pamp = portfolioService.loadHoldings(2).getPosition(47).orElseThrow();

		assertThat(pamp.getQuantity()).isEqualTo(15);
		assertThat(pamp.getReservedQuantity()).isEqualTo(3);
		assertThat(pamp.getAvailableQuantity()).isEqualTo(12);
	}

	@Test
	void returnsNotFoundForUnknownUser() {
		assertThat(mvc.get().uri("/api/users/999/portfolio")).hasStatus(HttpStatus.NOT_FOUND);
	}

}
