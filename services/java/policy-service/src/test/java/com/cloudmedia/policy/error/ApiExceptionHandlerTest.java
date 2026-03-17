package com.cloudmedia.policy.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

	@Test
	void returnsConstraintViolationEnvelope() throws Exception {
		mockMvc.perform(get("/test/constraint/ab")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.details['constraint.arg0']").exists());
	}

	@Test
	void returnsApiExceptionEnvelope() throws Exception {
		mockMvc.perform(get("/test/api-exception")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("POLICY_CONFLICT"))
				.andExpect(jsonPath("$.error.message").value("Policy conflict"));
	}

	@Test
	void returnsInternalErrorEnvelope() throws Exception {
		mockMvc.perform(get("/test/unhandled")).andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"));
	}

	@RestController
	@Validated
	@RequestMapping("/test")
	public static class ValidationTestController {

		@PostMapping("/validate")
		void validate(@Valid @RequestBody ValidationRequest request) {
		}

		@GetMapping("/constraint/{contentId}")
		void constraint(@PathVariable("contentId") @Size(min = 3) @Pattern(regexp = "^[a-z0-9_]+$") String contentId) {
		}

		@GetMapping("/api-exception")
		void apiException() {
			throw new ApiException(HttpStatus.CONFLICT, "POLICY_CONFLICT", "Policy conflict", null);
		}

		@GetMapping("/unhandled")
		void unhandled() {
			throw new IllegalStateException("boom");
		}
	}

	record ValidationRequest(@NotBlank String name) {
	}
}
