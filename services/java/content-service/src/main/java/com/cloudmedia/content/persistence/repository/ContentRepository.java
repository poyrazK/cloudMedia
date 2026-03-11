package com.cloudmedia.content.persistence.repository;

import com.cloudmedia.content.persistence.entity.ContentEntity;
import com.cloudmedia.content.persistence.entity.ContentState;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepository extends JpaRepository<ContentEntity, String> {
	List<ContentEntity> findByChannel_Id(String channelId);

	List<ContentEntity> findByChannel_IdAndState(String channelId, ContentState state);

	Optional<ContentEntity> findByIdAndChannel_Id(String id, String channelId);
}
