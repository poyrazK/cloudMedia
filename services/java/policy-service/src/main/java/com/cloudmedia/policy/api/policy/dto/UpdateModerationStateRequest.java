package com.cloudmedia.policy.api.policy.dto;

import com.cloudmedia.policy.persistence.entity.ModerationState;
import jakarta.validation.constraints.NotNull;

public record UpdateModerationStateRequest(@NotNull ModerationState moderationState) {
}
