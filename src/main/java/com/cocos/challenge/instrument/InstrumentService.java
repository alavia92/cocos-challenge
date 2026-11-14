package com.cocos.challenge.instrument;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstrumentService {

	private final InstrumentRepository instrumentRepository;

	public InstrumentService(InstrumentRepository instrumentRepository) {
		this.instrumentRepository = instrumentRepository;
	}

	/**
	 * Busca instrumentos cuyo ticker o nombre contenga el texto dado, sin distinguir mayúsculas.
	 */
	@Transactional(readOnly = true)
	public List<Instrument> search(String query) {
		return instrumentRepository.search("%" + escapeLike(query.strip().toLowerCase(Locale.ROOT)) + "%");
	}

	/** Evita que los comodines de {@code LIKE} enviados por el usuario se interpreten como tales. */
	private static String escapeLike(String text) {
		return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}
