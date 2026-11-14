package com.cocos.challenge.instrument;

import java.util.List;

import com.cocos.challenge.api.InstrumentsApi;
import com.cocos.challenge.api.model.InstrumentDto;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InstrumentController implements InstrumentsApi {

	private final InstrumentService instrumentService;

	private final InstrumentMapper instrumentMapper;

	public InstrumentController(InstrumentService instrumentService, InstrumentMapper instrumentMapper) {
		this.instrumentService = instrumentService;
		this.instrumentMapper = instrumentMapper;
	}

	/**
	 * Busca activos por ticker y/o nombre: devuelve los que contengan {@code query} en cualquiera de los dos.
	 */
	@Override
	public ResponseEntity<List<InstrumentDto>> searchInstruments(String query) {
		return ResponseEntity.ok(instrumentMapper.toDtos(instrumentService.search(query)));
	}

}
