package com.cloudmedia.discovery.search;

import java.util.List;

public record SearchResponse(List<SearchResultItem> items, int page, int size, long total) {
}
