package com.cocos.challenge.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.cocos.challenge.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dos órdenes simultáneas de un mismo usuario que, por separado, tienen fondos suficientes pero juntas no. Gracias al
 * bloqueo del usuario solo una puede ser aceptada. Sin transacción de test: cada orden se commitea de verdad.
 */
@IntegrationTest
class OrderConcurrencyTests {

	/** Usuario sin órdenes en los datos semilla. */
	private static final int USER_ID = 4;

	@Autowired
	private OrderService orderService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void deleteOrders() {
		jdbcTemplate.update("DELETE FROM orders WHERE userid = ?", USER_ID);
	}

	@RepeatedTest(10)
	void concurrentOrdersCannotSpendTheSameCash() throws Exception {
		jdbcTemplate.update("""
				INSERT INTO orders (instrumentid, userid, size, price, type, side, status, datetime)
				VALUES (66, ?, 1000, 1, 'MARKET', 'CASH_IN', 'FILLED', now())""", USER_ID);
		OrderRequest buy = new OrderRequest(USER_ID, 47, OrderSide.BUY, OrderType.LIMIT, 1, null,
				BigDecimal.valueOf(600));

		CountDownLatch start = new CountDownLatch(1);
		Callable<OrderStatus> submit = () -> {
			start.await();
			return orderService.submit(buy).getStatus();
		};
		List<OrderStatus> statuses;
		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			List<Future<OrderStatus>> futures = List.of(executor.submit(submit), executor.submit(submit));
			start.countDown();
			statuses = List.of(futures.get(0).get(), futures.get(1).get());
		}

		assertThat(statuses).containsExactlyInAnyOrder(OrderStatus.NEW, OrderStatus.REJECTED);
	}

}
