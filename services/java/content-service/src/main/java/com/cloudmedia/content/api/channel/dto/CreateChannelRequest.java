package com.cloudmedia.content.api.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateChannelRequest(@NotBlank @Size(max = 36) String ownerUserId,
		@NotBlank @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$") @Size(max = 128) String slug,
		@NotBlank @Size(max = 255) String displayName, @Size(max = 2000) String description) {
}
