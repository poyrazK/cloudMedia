package com.cloudmedia.content.persistence.repository;

import com.cloudmedia.content.persistence.entity.ChannelEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelRepository extends JpaRepository<ChannelEntity, String> {
	Optional<ChannelEntity> findBySlug(String slug);
}
