package com.cloudmedia.policy.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ApiExceptionHandlerTest.ValidationTestController.class)
@Import({ApiExceptionHandler.class, ApiExceptionHandlerTest.ValidationTestController.class})
class ApiExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsValidationErrorEnvelope() throws Exception {
		mockMvc.perform(post("/test/validate").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.details.name").value("must not be blank"));
	}

	@RestController
	@RequestMapping("/test")
	public static class ValidationTestController {

		@PostMapping("/validate")
		void validate(@Valid @RequestBody ValidationRequest request) {
		}
	}

	record ValidationRequest(@NotBlank String name) {
	}
}
