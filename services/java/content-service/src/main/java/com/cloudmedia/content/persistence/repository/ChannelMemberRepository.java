package com.cloudmedia.content.persistence.repository;

import com.cloudmedia.content.persistence.entity.ChannelMemberEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelMemberRepository extends JpaRepository<ChannelMemberEntity, String> {
	List<ChannelMemberEntity> findByUserId(String userId);

	List<ChannelMemberEntity> findByChannel_Id(String channelId);

	Optional<ChannelMemberEntity> findByChannel_IdAndUserId(String channelId, String userId);
}
