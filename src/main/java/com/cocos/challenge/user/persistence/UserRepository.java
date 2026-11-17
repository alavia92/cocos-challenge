package com.cocos.challenge.user.persistence;

import java.util.Optional;

import com.cocos.challenge.user.domain.User;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Integer> {

	/**
	 * Obtiene el usuario bloqueando su fila ({@code SELECT ... FOR UPDATE}) hasta el fin de la transacción. Serializa
	 * las operaciones que leen y modifican la tenencia de un mismo usuario, para que dos órdenes concurrentes no
	 * consuman los mismos pesos o acciones.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.id = :id")
	Optional<User> findByIdForUpdate(Integer id);

}
