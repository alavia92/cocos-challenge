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
 * Cancelación de órdenes sobre los datos semilla del usuario 1. La orden 5 (LIMIT BUY 50 LOMA a 710) está
 * {@code NEW} y reserva $35.500 de los $125.500 reservados.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class OrderCancellationTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private OrderRepository orderRepository;

	@Test
	void cancelsNewOrderAndReleasesReservedCash() {
		assertThat(cancel(5)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{"id": 5, "userId": 1, "ticker": "LOMA", "side": "BUY", "type": "LIMIT", "size": 50,
				 "price": 710.00, "status": "CANCELLED"}""");

		assertThat(orderRepository.findById(5).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
		// 627.500 + 35.500
		assertThat(mvc.get().uri("/api/users/1/portfolio")).bodyJson()
			.extractingPath("$.availableCash")
			.isEqualTo(663000.00);
	}

	@Test
	void cancellingPendingSellReleasesShares() throws Exception {
		MvcTestResult sell = submit("""
				{"userId": 1, "instrumentId": 47, "side": "SELL", "type": "LIMIT", "quantity": 40, "price": 1000}""");
		Integer orderId = JsonPath.read(sell.getResponse().getContentAsString(), "$.id");
		assertThat(submitMarketSell(40)).bodyJson().extractingPath("$.status").isEqualTo("REJECTED");

		assertThat(cancel(orderId)).hasStatusOk();

		assertThat(submitMarketSell(40)).bodyJson().extractingPath("$.status").isEqualTo("FILLED");
	}

	@ParameterizedTest
	@CsvSource({ "2, FILLED", "3, CANCELLED", "6, REJECTED" })
	void onlyNewOrdersCanBeCancelled(int orderId, OrderStatus status) {
		assertThat(cancel(orderId)).hasStatus(HttpStatus.CONFLICT)
			.bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("Order %d is %s; only NEW orders can be cancelled".formatted(orderId, status));

		assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(status);
	}

	@Test
	void orderCannotBeCancelledTwice() {
		assertThat(cancel(7)).hasStatusOk();

		assertThat(cancel(7)).hasStatus(HttpStatus.CONFLICT);
	}

	@Test
	void unknownOrderIsNotFound() {
		assertThat(cancel(999)).hasStatus(HttpStatus.NOT_FOUND);
	}

	private MvcTestResult cancel(int orderId) {
		return mvc.post().uri("/api/orders/{id}/cancel", orderId).exchange();
	}

	private MvcTestResult submitMarketSell(int quantity) {
		return submit("""
				{"userId": 1, "instrumentId": 47, "side": "SELL", "type": "MARKET", "quantity": %d}""".formatted(quantity));
	}

	private MvcTestResult submit(String json) {
		return mvc.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON).content(json).exchange();
	}

}
