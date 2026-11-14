package com.cocos.challenge.instrument;

import java.util.List;

import com.cocos.challenge.api.InstrumentsApi;
import com.cocos.challenge.api.model.InstrumentDto;
import com.cocos.challenge.api.model.InstrumentTypeDto;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InstrumentController implements InstrumentsApi {

	private final InstrumentService instrumentService;

	public InstrumentController(InstrumentService instrumentService) {
		this.instrumentService = instrumentService;
	}

	/**
	 * Busca activos por ticker y/o nombre: devuelve los que contengan {@code query} en cualquiera de los dos.
	 */
	@Override
	public ResponseEntity<List<InstrumentDto>> searchInstruments(String query) {
		return ResponseEntity.ok(instrumentService.search(query).stream().map(InstrumentController::toDto).toList());
	}

	private static InstrumentDto toDto(Instrument instrument) {
		return new InstrumentDto(instrument.getId(), instrument.getTicker(), instrument.getName(),
				InstrumentTypeDto.valueOf(instrument.getType().name()));
	}

}
