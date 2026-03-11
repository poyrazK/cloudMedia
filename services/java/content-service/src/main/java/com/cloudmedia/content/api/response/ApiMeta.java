package com.cloudmedia.content.api.response;

import java.time.Instant;

public record ApiMeta(String requestId, Instant timestamp) {
}
