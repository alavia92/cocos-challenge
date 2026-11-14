package com.cocos.challenge.order;

import com.cocos.challenge.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test funcional del envío de órdenes, sobre los datos semilla. El usuario 1 tiene $627.500 disponibles y 40 acciones
 * de PAMP (id 47, último precio 925,85). Cada test se revierte al terminar.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class OrderControllerTests {

	private static final int PAMP = 47;

	private static final int METR = 54;

	/** Instrumento sin cotizaciones. */
	private static final int PGR = 3;

	private static final int ARS = 66;

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private OrderRepository orderRepository;

	@Test
	void marketOrderIsFilledAtLastPriceAndUpdatesPortfolio() throws Exception {
		MvcTestResult result = submit("""
				{"userId": 1, "instrumentId": %d, "side": "BUY", "type": "MARKET", "quantity": 10}""".formatted(PAMP));

		assertThat(result).hasStatus(HttpStatus.CREATED).bodyJson().isLenientlyEqualTo("""
				{"userId": 1, "instrumentId": 47, "ticker": "PAMP", "side": "BUY", "type": "MARKET",
				 "size": 10, "price": 925.85, "status": "FILLED"}""");
		Order order = persisted(result);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
		assertThat(order.getPrice()).isEqualByComparingTo("925.85");
		assertThat(order.getDatetime()).isNotNull();
		// 627.500 - 10 x 925,85 = 618.241,50
		assertThat(mvc.get().uri("/api/users/1/portfolio")).bodyJson()
			.isLenientlyEqualTo("""
					{"availableCash": 618241.50, "positions": [{"ticker": "BMA"}, {"ticker": "METR"},
					 {"ticker": "PAMP", "quantity": 50}]}""");
	}

	@Test
	void marketOrderByAmountBuysMaximumWholeShares() {
		// 10.000 / 925,85 = 10,8 -> 10 acciones
		assertThat(submit("""
				{"userId": 1, "instrumentId": %d, "side": "BUY", "type": "MARKET", "amount": 10000}""".formatted(PAMP)))
			.hasStatus(HttpStatus.CREATED)
			.bodyJson()
			.isLenientlyEqualTo("""
					{"size": 10, "price": 925.85, "status": "FILLED"}""");
	}

	@Test
	void limitOrderIsNewAndReservesCash() {
		assertThat(submit("""
				{"userId": 1, "instrumentId": %d, "side": "BUY", "type": "LIMIT", "quantity": 100, "price": 200}"""
			.formatted(METR))).hasStatus(HttpStatus.CREATED).bodyJson().isLenientlyEqualTo("""
					{"size": 100, "price": 200.00, "status": "NEW"}""");

		// La orden no se ejecuta (METR sigue en 500), pero reserva 100 x 200 = 20.000
		assertThat(mvc.get().uri("/api/users/1/portfolio")).bodyJson()
			.isLenientlyEqualTo("""
					{"availableCash": 607500.00, "positions": [{"ticker": "BMA"},
					 {"ticker": "METR", "quantity": 500}, {"ticker": "PAMP"}]}""");
	}

	@Test
	void limitOrderByAmountUsesLimitPrice() {
		// 1.000 / 300 = 3,33 -> 3 acciones
		assertThat(submit("""
				{"userId": 1, "instrumentId": %d, "side": "SELL", "type": "LIMIT", "amount": 1000, "price": 300}"""
			.formatted(METR))).hasStatus(HttpStatus.CREATED).bodyJson().isLenientlyEqualTo("""
					{"side": "SELL", "size": 3, "price": 300.00, "status": "NEW"}""");
	}

	@Test
	void marketSellOfWholePositionClosesIt() {
		assertThat(submit("""
				{"userId": 1, "instrumentId": %d, "side": "SELL", "type": "MARKET", "quantity": 40}""".formatted(PAMP)))
			.hasStatus(HttpStatus.CREATED)
			.bodyJson()
			.isLenientlyEqualTo("""
					{"size": 40, "price": 925.85, "status": "FILLED"}""");

		// 627.500 + 40 x 925,85 = 664.534
		assertThat(mvc.get().uri("/api/users/1/portfolio")).bodyJson()
			.isLenientlyEqualTo("""
					{"availableCash": 664534.00, "positions": [{"ticker": "BMA"}, {"ticker": "METR"}]}""");
	}

	@Test
	void buyOverAvailableCashIsRejectedAndRecorded() throws Exception {
		// 1.000 x 925,85 = 925.850 > 627.500
		MvcTestResult result = submit("""
				{"userId": 1, "instrumentId": %d, "side": "BUY", "type": "MARKET", "quantity": 1000}""".formatted(PAMP));

		assertThat(result).hasStatus(HttpStatus.CREATED).bodyJson().isLenientlyEqualTo("""
				{"size": 1000, "price": 925.85, "status": "REJECTED"}""");
		assertThat(persisted(result).getStatus()).isEqualTo(OrderStatus.REJECTED);
		assertThat(mvc.get().uri("/api/users/1/portfolio")).bodyJson()
			.extractingPath("$.availableCash")
			.isEqualTo(627500.00);
	}

	@Test
	void buyForExactlyAvailableCashIsAccepted() {
		assertThat(submit("""
				{"userId": 1, "instrumentId": %d, "side": "BUY", "type": "LIMIT", "quantity": 1, "price": 627500}"""
			.formatted(PAMP))).bodyJson().extractingPath("$.status").isEqualTo("NEW");

		// Los pesos quedaron reservados por la orden anterior
		assertThat(submit("""
				{"userId": 1, "instrumentId": %d, "side": "BUY", "type": "LIMIT", "quantity": 1, "price": 0.01}"""
			.formatted(PAMP))).bodyJson().extractingPath("$.status").isEqualTo("REJECTED");
	}

	@Test
	void sellOverHoldingIsRejected() {
		assertThat(submit("""
				{"userId": 1, "instrumentId": %d, "side": "SELL", "type": "MARKET", "quantity": 41}""".formatted(PAMP)))
			.bodyJson()
			.extractingPath("$.status")
			.isEqualTo("REJECTED");
	}

	@Test
	void sharesReservedByPendingSellCannotBeSold() {
		assertThat(submit("""
				{"userId": 1, "instrumentId": %d, "side": "SELL", "type": "LIMIT", "quantity": 30, "price": 1000}"""
			.formatted(PAMP))).bodyJson().extractingPath("$.status").isEqualTo("NEW");

		// Quedan 40 - 30 = 10 acciones disponibles
		assertThat(submit("""
				{"userId": 1, "instrumentId": %d, "side": "SELL", "type": "MARKET", "quantity": 11}""".formatted(PAMP)))
			.bodyJson()
			.extractingPath("$.status")
			.isEqualTo("REJECTED");
		assertThat(submit("""
				{"userId": 1, "instrumentId": %d, "side": "SELL", "type": "MARKET", "quantity": 10}""".formatted(PAMP)))
			.bodyJson()
			.extractingPath("$.status")
			.isEqualTo("FILLED");
	}

	@Test
	void amountBelowPriceIsRejected() {
		assertThat(submit("""
				{"userId": 1, "instrumentId": %d, "side": "BUY", "type": "MARKET", "amount": 900}""".formatted(PAMP)))
			.hasStatus(HttpStatus.CREATED)
			.bodyJson()
			.isLenientlyEqualTo("""
					{"size": 0, "status": "REJECTED"}""");
	}

	@Test
	void marketOrderWithoutMarketDataIsRejected() throws Exception {
		MvcTestResult result = submit("""
				{"userId": 1, "instrumentId": %d, "side": "BUY", "type": "MARKET", "quantity": 1}""".formatted(PGR));

		assertThat(result).hasStatus(HttpStatus.CREATED).bodyJson().isLenientlyEqualTo("""
				{"size": 1, "status": "REJECTED"}""");
		assertThat(persisted(result).getPrice()).isNull();
	}

	@Test
	void userWithoutCashCannotBuy() {
		assertThat(submit("""
				{"userId": 3, "instrumentId": %d, "side": "BUY", "type": "MARKET", "quantity": 1}""".formatted(PAMP)))
			.bodyJson()
			.extractingPath("$.status")
			.isEqualTo("REJECTED");
	}

	@Test
	void unknownUserIsNotFound() {
		assertThat(submit("""
				{"userId": 999, "instrumentId": %d, "side": "BUY", "type": "MARKET", "quantity": 1}""".formatted(PAMP)))
			.hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void unknownInstrumentIsNotFound() {
		assertThat(submit("""
				{"userId": 1, "instrumentId": 999, "side": "BUY", "type": "MARKET", "quantity": 1}"""))
			.hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void currencyCannotBeTraded() {
		assertThat(submit("""
				{"userId": 1, "instrumentId": %d, "side": "BUY", "type": "MARKET", "quantity": 1}""".formatted(ARS)))
			.hasStatus(HttpStatus.BAD_REQUEST);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
			"side": "CASH_IN", "type": "MARKET", "quantity": 1                  | tradeSide: must be BUY or SELL
			"side": "BUY", "type": "MARKET"                                     | sizeSpecified: exactly one of quantity or amount must be provided
			"side": "BUY", "type": "MARKET", "quantity": 1, "amount": 100       | sizeSpecified: exactly one of quantity or amount must be provided
			"side": "BUY", "type": "LIMIT", "quantity": 1                       | priceConsistentWithType: price is required for LIMIT orders and not allowed for MARKET orders
			"side": "BUY", "type": "MARKET", "quantity": 1, "price": 100        | priceConsistentWithType: price is required for LIMIT orders and not allowed for MARKET orders
			"side": "BUY", "type": "MARKET", "quantity": 0                      | quantity: must be greater than 0
			"side": "BUY", "type": "MARKET", "amount": -5                       | amount: must be greater than 0
			"side": "BUY", "type": "LIMIT", "quantity": 1, "price": 10.123      | price: numeric value out of bounds (<8 digits>.<2 digits> expected)
			"type": "MARKET", "quantity": 1                                     | side: must not be null
			""")
	void invalidRequestIsBadRequest(String fields, String expectedError) {
		MvcTestResult result = submit("""
				{"userId": 1, "instrumentId": %d, %s}""".formatted(PAMP, fields));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors")
			.asArray()
			.containsExactly(expectedError);
		assertThat(orderRepository.count()).isEqualTo(11);
	}

	private MvcTestResult submit(String json) {
		return mvc.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON).content(json).exchange();
	}

	private Order persisted(MvcTestResult result) throws Exception {
		Integer id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
		return orderRepository.findById(id).orElseThrow();
	}

}
