package com.cocos.challenge.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import com.cocos.challenge.instrument.Instrument;
import com.cocos.challenge.instrument.InstrumentRepository;
import com.cocos.challenge.instrument.InstrumentType;
import com.cocos.challenge.marketdata.MarketData;
import com.cocos.challenge.marketdata.MarketDataRepository;
import com.cocos.challenge.portfolio.Holdings;
import com.cocos.challenge.portfolio.Holdings.Position;
import com.cocos.challenge.portfolio.PortfolioService;
import com.cocos.challenge.user.User;
import com.cocos.challenge.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

	private static final BigDecimal MAX_SIZE = BigDecimal.valueOf(Integer.MAX_VALUE);

	private final UserRepository userRepository;

	private final InstrumentRepository instrumentRepository;

	private final MarketDataRepository marketDataRepository;

	private final OrderRepository orderRepository;

	private final PortfolioService portfolioService;

	public OrderService(UserRepository userRepository, InstrumentRepository instrumentRepository,
			MarketDataRepository marketDataRepository, OrderRepository orderRepository,
			PortfolioService portfolioService) {
		this.userRepository = userRepository;
		this.instrumentRepository = instrumentRepository;
		this.marketDataRepository = marketDataRepository;
		this.orderRepository = orderRepository;
		this.portfolioService = portfolioService;
	}

	/**
	 * Envía una orden al mercado y la registra:
	 * <ul>
	 * <li>{@code MARKET}: se ejecuta al último precio ({@code close}) y queda {@code FILLED};</li>
	 * <li>{@code LIMIT}: se registra al precio límite y queda {@code NEW};</li>
	 * <li>si no cumple los requerimientos (pesos o acciones insuficientes, tamaño 0, instrumento sin cotización para
	 * una orden {@code MARKET}), queda {@code REJECTED}.</li>
	 * </ul>
	 */
	@Transactional
	public Order submit(OrderRequest request) {
		// Bloquea al usuario hasta el commit: la validación de fondos y el alta de la orden son atómicas
		User user = userRepository.findByIdForUpdate(request.userId())
			.orElseThrow(() -> notFound("User %d not found".formatted(request.userId())));
		Instrument instrument = instrumentRepository.findById(request.instrumentId())
			.orElseThrow(() -> notFound("Instrument %d not found".formatted(request.instrumentId())));
		if (instrument.getType() == InstrumentType.MONEDA) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Instrument %s is a currency and cannot be traded".formatted(instrument.getTicker()));
		}

		BigDecimal price = (request.type() == OrderType.LIMIT) ? request.price() : lastPrice(instrument);
		int size = size(request, price);
		OrderStatus status = isExecutable(user, instrument, request.side(), size, price)
				? (request.type() == OrderType.MARKET ? OrderStatus.FILLED : OrderStatus.NEW) : OrderStatus.REJECTED;

		return orderRepository.save(new Order(user, instrument, request.side(), request.type(), size, price, status,
				LocalDateTime.now()));
	}

	/**
	 * Cancela una orden pendiente ({@code NEW}), liberando los pesos o acciones que reservaba.
	 */
	@Transactional
	public Order cancel(Integer orderId) {
		Order order = orderRepository.findByIdForUpdate(orderId)
			.orElseThrow(() -> notFound("Order %d not found".formatted(orderId)));
		if (order.getStatus() != OrderStatus.NEW) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Order %d is %s; only NEW orders can be cancelled".formatted(orderId, order.getStatus()));
		}
		order.cancel();
		return order;
	}

	/** Último precio del instrumento, o {@code null} si no tiene cotizaciones. */
	private BigDecimal lastPrice(Instrument instrument) {
		return marketDataRepository.findFirstByInstrumentIdOrderByDateDesc(instrument.getId())
			.map(MarketData::getClose)
			.orElse(null);
	}

	/**
	 * Cantidad de acciones de la orden: la indicada, o la máxima cantidad entera que cubre el monto al precio de la
	 * orden. Es 0 si no hay precio para calcularla.
	 */
	private static int size(OrderRequest request, BigDecimal price) {
		if (request.quantity() != null) {
			return request.quantity();
		}
		if (price == null) {
			return 0;
		}
		return request.amount().divide(price, 0, RoundingMode.DOWN).min(MAX_SIZE).intValue();
	}

	private boolean isExecutable(User user, Instrument instrument, OrderSide side, int size, BigDecimal price) {
		if (price == null || size == 0) {
			return false;
		}
		Holdings holdings = portfolioService.loadHoldings(user.getId());
		return switch (side) {
			case BUY -> price.multiply(BigDecimal.valueOf(size)).compareTo(holdings.getAvailableCash()) <= 0;
			case SELL -> size <= holdings.getPosition(instrument.getId()).map(Position::getAvailableQuantity).orElse(0);
			default -> throw new IllegalArgumentException("Unsupported order side " + side);
		};
	}

	private static ResponseStatusException notFound(String detail) {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, detail);
	}

}
