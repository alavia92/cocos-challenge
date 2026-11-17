package com.cocos.challenge.order.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.cocos.challenge.order.domain.Order;
import com.cocos.challenge.order.domain.OrderStatus;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Integer> {

	/**
	 * Órdenes de un usuario con los estados dados, en orden cronológico y con su instrumento cargado.
	 */
	@Query("""
			select o from Order o join fetch o.instrument
			where o.user.id = :userId and o.status in :statuses
			order by o.datetime, o.id""")
	List<Order> findByUserIdAndStatusIn(Integer userId, Collection<OrderStatus> statuses);

	/**
	 * Obtiene la orden con su instrumento, bloqueando su fila ({@code SELECT ... FOR UPDATE}) hasta el fin de la
	 * transacción.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from Order o join fetch o.instrument where o.id = :id")
	Optional<Order> findByIdForUpdate(Integer id);

}
