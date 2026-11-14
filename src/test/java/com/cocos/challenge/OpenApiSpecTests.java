package com.cocos.challenge;

import org.junit.jupiter.api.Test;

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

}
