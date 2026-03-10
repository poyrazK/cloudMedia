package com.cloudmedia.identity.persistence.repository;

import com.cloudmedia.identity.persistence.entity.UserCredentialEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCredentialRepository extends JpaRepository<UserCredentialEntity, String> {
	Optional<UserCredentialEntity> findByUser_Id(String userId);
}
