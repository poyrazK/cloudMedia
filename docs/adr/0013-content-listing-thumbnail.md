# ADR 013: Content Listing and Thumbnail Support

## Status

Accepted

## Date

2026-04-26

## Context

The content-service MVP was missing support for listing channel content and did not have thumbnail URL tracking. The implementation plan specified adding:

1. Channel content listing endpoint (`GET /v1/channels/{channelId}/content`)
2. Thumbnail URL field on content entity and responses

These features are needed for:

- Users viewing all content on a channel
- Displaying video thumbnails in UI
- Integration with discovery/search (which uses thumbnail URLs)

## Decision

### Database Schema

Added `thumbnail_url` column to the `content` table via Flyway migration:

```sql
ALTER TABLE content ADD COLUMN thumbnail_url varchar(512);
```

### API Endpoint

Added `GET /v1/channels/{channelId}/content` endpoint to ChannelController with optional `state` query parameter for filtering by ContentState.

### Response Format

ContentResponse DTO extended to include `thumbnailUrl` field:

```java
public record ContentResponse(..., String thumbnailUrl) { }
```

### Implementation Details

- Repository already had `findByChannel_Id()` method - no changes needed
- State filtering uses existing `findByChannel_IdAndState()` method
- 404 returned when channel does not exist
- Results ordered by `created_at` ascending (chronological order)

## Consequences

### Positive

- Channel content is now queryable via REST API
- Thumbnail URLs can be stored and retrieved
- Consistent with existing API response patterns
- Ready for discovery service integration

### Negative

- Additional database column increases storage
- More fields to maintain in migrations

### Neutral

- Existing content endpoints unchanged
- Backward compatible (thumbnailUrl is optional)

## Alternatives Considered

### Alternative 1: Separate Content Listing Controller

Create a new `ChannelContentController` instead of adding to existing `ChannelController`.

**Why rejected:** Keeping listing in `ChannelController` maintains consistency with other channel-scoped endpoints and avoids unnecessary controller proliferation.

### Alternative 2: Pagination

Implement cursor-based pagination for content listing.

**Why rejected:** MVP scope kept simple. Pagination can be added when volume warrants it per the roadmap's TODO note in the REST API contract.

## Implementation Notes

- Migration file: `V3__add_content_thumbnail.sql`
- New tests: 7 tests added (5 integration + 2 web MVC)
- Total test count: 50 (all passing)