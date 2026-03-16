package com.cloudmedia.discovery.discovery;

import java.util.List;

public record HomeFeedResponse(List<HomeFeedItem> items, int size) {
}
