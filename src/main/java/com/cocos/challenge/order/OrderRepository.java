package com.cocos.challenge.order;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
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

}
