package com.cloudmedia.content.api.content.dto;

import com.cloudmedia.content.persistence.entity.ContentVisibility;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.MalformedURLException;
import java.net.URL;

public record UpdateContentRequest(@NotBlank @Size(max = 36) String userId,
		@Size(max = 255) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String title,
		@Size(max = 4000) String description, ContentVisibility visibility, @Size(max = 512) String thumbnailUrl) {

	@AssertTrue(message = "At least one field must be provided for update")
	public boolean hasUpdatableField() {
		return title != null || description != null || visibility != null || thumbnailUrl != null;
	}

	@AssertTrue(message = "thumbnailUrl must be a valid URL")
	public boolean isThumbnailUrlValid() {
		if (thumbnailUrl == null || thumbnailUrl.isBlank()) {
			return true;
		}
		try {
			new URL(thumbnailUrl);
			return true;
		} catch (MalformedURLException e) {
			return false;
		}
	}
}
