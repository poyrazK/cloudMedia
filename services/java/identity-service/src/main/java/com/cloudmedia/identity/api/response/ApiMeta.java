package com.cloudmedia.identity.api.response;

import java.time.Instant;

public record ApiMeta(String requestId, Instant timestamp) {
}
