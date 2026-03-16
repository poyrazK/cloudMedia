package com.cloudmedia.discovery.search;

import java.util.List;

public record AutocompleteResponse(List<AutocompleteSuggestion> items, int size) {
}
