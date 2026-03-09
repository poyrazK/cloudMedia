package com.cloudmedia.identity.api.dto;

public record LogoutRequest(String sessionId, boolean allSessions) {
}
