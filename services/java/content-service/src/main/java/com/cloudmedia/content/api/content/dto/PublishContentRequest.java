package com.cloudmedia.content.api.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublishContentRequest(@NotBlank @Size(max = 36) String userId) {
}
