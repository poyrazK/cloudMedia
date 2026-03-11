package com.cloudmedia.identity.events;

public record UserUpdatedPayload(String userId, String email, String updateType) {
}
