package com.cocos.challenge.marketdata;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MarketDataRepository extends JpaRepository<MarketData, Integer> {

	/**
	 * Última cotización (mayor fecha) de cada uno de los instrumentos dados. Los instrumentos sin cotizaciones no
	 * aparecen en el resultado.
	 */
	@Query("""
			select m from MarketData m
			where m.instrument.id in :instrumentIds
			  and m.date = (select max(l.date) from MarketData l where l.instrument = m.instrument)""")
	List<MarketData> findLatestByInstrumentIdIn(Collection<Integer> instrumentIds);

}
