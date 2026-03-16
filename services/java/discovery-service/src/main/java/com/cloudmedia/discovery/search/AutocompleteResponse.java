package com.cloudmedia.discovery.search;

import java.util.List;

/**
 * `size` reflects the requested suggestion limit, not necessarily
 * `items.size()`.
 */
public record AutocompleteResponse(List<AutocompleteSuggestion> items, int size) {
}
