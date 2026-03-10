package com.cloudmedia.identity.persistence.repository;

import com.cloudmedia.identity.persistence.entity.OAuthAccountEntity;
import com.cloudmedia.identity.persistence.entity.OAuthProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccountEntity, String> {
	Optional<OAuthAccountEntity> findByProviderAndProviderSubject(OAuthProvider provider, String providerSubject);
}
