package com.cloudmedia.content.persistence.repository;

import com.cloudmedia.content.persistence.entity.ContentEntity;
import com.cloudmedia.content.persistence.entity.ContentState;
import com.cloudmedia.content.persistence.entity.ContentVisibility;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentRepository extends JpaRepository<ContentEntity, String> {
	List<ContentEntity> findByChannel_IdOrderByCreatedAtAsc(String channelId);

	List<ContentEntity> findByChannel_IdAndStateOrderByCreatedAtAsc(String channelId, ContentState state);

	List<ContentEntity> findByChannel_IdAndVisibilityOrderByCreatedAtAsc(String channelId,
			ContentVisibility visibility);

	List<ContentEntity> findByChannel_IdAndStateAndVisibilityOrderByCreatedAtAsc(String channelId, ContentState state,
			ContentVisibility visibility);

	@EntityGraph(attributePaths = {"channel"})
	@Query("SELECT c FROM ContentEntity c WHERE c.channel.id = :channelId ORDER BY c.createdAt ASC")
	List<ContentEntity> findByChannel_IdWithChannelOrderByCreatedAtAsc(@Param("channelId") String channelId);

	@EntityGraph(attributePaths = {"channel"})
	@Query("SELECT c FROM ContentEntity c WHERE c.channel.id = :channelId AND c.visibility = :visibility ORDER BY c.createdAt ASC")
	List<ContentEntity> findByChannel_IdAndVisibilityWithChannelOrderByCreatedAtAsc(
			@Param("channelId") String channelId, @Param("visibility") ContentVisibility visibility);

	@EntityGraph(attributePaths = {"channel"})
	@Query("SELECT c FROM ContentEntity c WHERE c.channel.id = :channelId AND c.state = :state AND c.visibility = :visibility ORDER BY c.createdAt ASC")
	List<ContentEntity> findByChannel_IdAndStateAndVisibilityWithChannelOrderByCreatedAtAsc(
			@Param("channelId") String channelId, @Param("state") ContentState state,
			@Param("visibility") ContentVisibility visibility);

	Optional<ContentEntity> findByIdAndChannel_Id(String id, String channelId);
}
