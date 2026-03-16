package com.cloudmedia.discovery.discovery;

import java.util.List;

public record HomeFeedCandidates(List<HomeFeedItem> followed, List<HomeFeedItem> trending, List<HomeFeedItem> fresh,
		List<HomeFeedItem> similar) {

	public static HomeFeedCandidates empty() {
		return new HomeFeedCandidates(List.of(), List.of(), List.of(), List.of());
	}
}
