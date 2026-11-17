package com.cocos.challenge.instrument.persistence;

import java.util.List;

import com.cocos.challenge.instrument.domain.Instrument;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InstrumentRepository extends JpaRepository<Instrument, Integer> {

	/**
	 * Busca instrumentos operables (excluye monedas) cuyo ticker o nombre coincida con el patrón {@code LIKE}
	 * dado, sin distinguir mayúsculas. El patrón debe venir en minúsculas y con {@code \} como carácter de escape.
	 */
	@Query("""
			select i from Instrument i
			where i.type <> com.cocos.challenge.instrument.domain.InstrumentType.MONEDA
			  and (lower(i.ticker) like :pattern escape '\\' or lower(i.name) like :pattern escape '\\')
			order by i.ticker""")
	List<Instrument> search(String pattern);

}
