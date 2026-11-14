package com.cocos.challenge.instrument;

import com.cocos.challenge.IntegrationTest;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@AutoConfigureMockMvc
class InstrumentControllerTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void searchesByTickerIgnoringCase() {
		assertThat(mvc.get().uri("/api/instruments").param("query", "pamp")).hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("""
					[{"id": 47, "ticker": "PAMP", "name": "Pampa Holding S.A.", "type": "ACCIONES"}]""");
	}

	@Test
	void searchesByTickerOrName() {
		// IRSA coincide por ticker y nombre; IRCP solo por nombre ("IRSA Propiedades Comerciales S.A.")
		assertThat(mvc.get().uri("/api/instruments").param("query", "IRSA")).hasStatusOk()
			.bodyJson()
			.extractingPath("$[*].ticker")
			.asArray()
			.containsExactly("IRCP", "IRSA");
	}

	@Test
	void searchesByPartialName() {
		assertThat(mvc.get().uri("/api/instruments").param("query", " boldt ")).hasStatusOk()
			.bodyJson()
			.extractingPath("$[*].ticker")
			.asArray()
			.containsExactly("BOLT", "GAMI");
	}

	@Test
	void excludesCurrencies() {
		assertThat(mvc.get().uri("/api/instruments").param("query", "pesos")).hasStatusOk()
			.bodyJson()
			.isEqualTo("[]");
	}

	@Test
	void treatsLikeWildcardsAsLiterals() {
		assertThat(mvc.get().uri("/api/instruments").param("query", "%")).hasStatusOk()
			.bodyJson()
			.isEqualTo("[]");
	}

	@Test
	void rejectsBlankQuery() {
		assertThat(mvc.get().uri("/api/instruments").param("query", " ")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void rejectsMissingQuery() {
		assertThat(mvc.get().uri("/api/instruments")).hasStatus(HttpStatus.BAD_REQUEST);
	}

}
