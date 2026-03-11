package com.cloudmedia.identity.events;

public record UserCreatedPayload(String userId, String email, String source) {
}
