package com.cocos.challenge.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.cocos.challenge.marketdata.MarketData;
import com.cocos.challenge.marketdata.MarketDataRepository;
import com.cocos.challenge.order.OrderRepository;
import com.cocos.challenge.order.OrderStatus;
import com.cocos.challenge.portfolio.Holdings.Position;
import com.cocos.challenge.portfolio.PortfolioResponse.PositionResponse;
import com.cocos.challenge.user.User;
import com.cocos.challenge.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PortfolioService {

	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private final UserRepository userRepository;

	private final OrderRepository orderRepository;

	private final MarketDataRepository marketDataRepository;

	public PortfolioService(UserRepository userRepository, OrderRepository orderRepository,
			MarketDataRepository marketDataRepository) {
		this.userRepository = userRepository;
		this.orderRepository = orderRepository;
		this.marketDataRepository = marketDataRepository;
	}

	@Transactional(readOnly = true)
	public PortfolioResponse getPortfolio(Integer userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User %d not found".formatted(userId)));
		Holdings holdings = loadHoldings(userId);

		List<Position> openPositions = holdings.getPositions().stream().filter(p -> p.getQuantity() != 0).toList();
		Map<Integer, MarketData> latestPrices = marketDataRepository
			.findLatestByInstrumentIdIn(openPositions.stream().map(p -> p.getInstrument().getId()).toList())
			.stream()
			.collect(Collectors.toMap(m -> m.getInstrument().getId(), Function.identity()));

		List<PositionResponse> positions = openPositions.stream()
			.map(p -> toResponse(p, latestPrices.get(p.getInstrument().getId())))
			.sorted(Comparator.comparing(PositionResponse::ticker))
			.toList();
		BigDecimal totalValue = positions.stream()
			.map(PositionResponse::marketValue)
			.filter(v -> v != null)
			.reduce(holdings.getCash(), BigDecimal::add);

		return new PortfolioResponse(user.getId(), user.getAccountNumber(), money(totalValue),
				money(holdings.getAvailableCash()), positions);
	}

	/** Tenencia del usuario según sus órdenes ejecutadas y pendientes. */
	@Transactional(readOnly = true)
	public Holdings loadHoldings(Integer userId) {
		return Holdings.of(orderRepository.findByUserIdAndStatusIn(userId, EnumSet.of(OrderStatus.FILLED, OrderStatus.NEW)));
	}

	private static PositionResponse toResponse(Position position, MarketData marketData) {
		BigDecimal lastPrice = null;
		BigDecimal marketValue = null;
		BigDecimal totalReturn = null;
		BigDecimal dailyReturn = null;
		if (marketData != null) {
			lastPrice = marketData.getClose();
			marketValue = money(lastPrice.multiply(BigDecimal.valueOf(position.getQuantity())));
			dailyReturn = percentChange(marketData.getPreviousClose(), lastPrice);
			if (position.getQuantity() > 0 && position.getCost().signum() > 0) {
				totalReturn = percentChange(position.getCost(), marketValue);
			}
		}
		return new PositionResponse(position.getInstrument().getId(), position.getInstrument().getTicker(),
				position.getInstrument().getName(), position.getQuantity(), lastPrice, marketValue, totalReturn,
				dailyReturn);
	}

	private static BigDecimal percentChange(BigDecimal from, BigDecimal to) {
		return to.subtract(from).multiply(HUNDRED).divide(from, 2, RoundingMode.HALF_UP);
	}

	private static BigDecimal money(BigDecimal amount) {
		return amount.setScale(2, RoundingMode.HALF_UP);
	}

}
