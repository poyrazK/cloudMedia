package com.cloudmedia.content.api.content.dto;

import com.cloudmedia.content.persistence.entity.ContentType;
import com.cloudmedia.content.persistence.entity.ContentVisibility;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateContentRequest(@NotBlank @Size(max = 36) String userId, @NotBlank @Size(max = 36) String channelId,
		@NotBlank @Size(max = 255) String title, @Size(max = 4000) String description, @NotNull ContentType contentType,
		ContentVisibility visibility) {
}
