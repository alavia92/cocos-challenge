package com.cocos.challenge.instrument;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {

	private final InstrumentService instrumentService;

	public InstrumentController(InstrumentService instrumentService) {
		this.instrumentService = instrumentService;
	}

	/**
	 * Busca activos por ticker y/o nombre: devuelve los que contengan {@code query} en cualquiera de los dos.
	 */
	@GetMapping
	public List<InstrumentResponse> search(@RequestParam @NotBlank String query) {
		return instrumentService.search(query).stream().map(InstrumentResponse::from).toList();
	}

}
