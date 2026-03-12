package com.cloudmedia.content.api.content.dto;

import com.cloudmedia.content.persistence.entity.ContentVisibility;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateContentRequest(@NotBlank @Size(max = 36) String userId,
		@Size(max = 255) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String title,
		@Size(max = 4000) String description, ContentVisibility visibility) {

	@AssertTrue(message = "At least one field must be provided for update")
	public boolean hasUpdatableField() {
		return title != null || description != null || visibility != null;
	}
}
