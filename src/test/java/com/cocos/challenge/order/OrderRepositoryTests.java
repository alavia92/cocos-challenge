package com.cocos.challenge.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.cocos.challenge.IntegrationTest;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Transactional
class OrderRepositoryTests {

	@Autowired
	private OrderRepository orderRepository;

	@Test
	void mapsSeedOrder() {
		Order order = orderRepository.findById(1).orElseThrow();

		assertThat(order.getUser().getEmail()).isEqualTo("emiliano@test.com");
		assertThat(order.getInstrument().getTicker()).isEqualTo("ARS");
		assertThat(order.getSize()).isEqualTo(1_000_000);
		assertThat(order.getPrice()).isEqualByComparingTo(BigDecimal.ONE);
		assertThat(order.getType()).isEqualTo(OrderType.MARKET);
		assertThat(order.getSide()).isEqualTo(OrderSide.CASH_IN);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
		assertThat(order.getDatetime()).isEqualTo(LocalDateTime.of(2023, 7, 12, 12, 11, 20));
	}

}
