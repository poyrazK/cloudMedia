package com.cloudmedia.identity.persistence.repository;

import com.cloudmedia.identity.persistence.entity.SessionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<SessionEntity, String> {
	List<SessionEntity> findByUser_IdAndRevokedAtIsNullOrderByCreatedAtAsc(String userId);
}
