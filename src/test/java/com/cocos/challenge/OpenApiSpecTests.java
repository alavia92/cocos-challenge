package com.cocos.challenge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@AutoConfigureMockMvc
class OpenApiSpecTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void servesTheContract() {
		assertThat(mvc.get().uri("/openapi.yaml")).hasStatusOk().bodyText().startsWith("openapi: 3.0.3");
	}

	@Test
	void swaggerUiShowsTheContract() {
		assertThat(mvc.get().uri("/swagger-ui.html")).hasStatusOk().bodyText().contains("url: '/openapi.yaml'");
	}

	@ParameterizedTest
	@ValueSource(strings = { "swagger-ui.css", "swagger-ui-bundle.js", "swagger-ui-standalone-preset.js",
			"favicon-32x32.png" })
	void servesSwaggerUiAssets(String asset) {
		assertThat(mvc.get().uri("/webjars/swagger-ui/{asset}", asset)).hasStatusOk();
	}

}
