package com.cloudmedia.identity.persistence.repository;

import com.cloudmedia.identity.persistence.entity.RefreshTokenEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, String> {
	Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

	List<RefreshTokenEntity> findBySession_IdAndRevokedAtIsNull(String sessionId);
}
