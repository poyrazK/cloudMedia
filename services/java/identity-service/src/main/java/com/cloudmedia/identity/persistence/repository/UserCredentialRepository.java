package com.cloudmedia.identity.persistence.repository;

import com.cloudmedia.identity.persistence.entity.UserCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCredentialRepository extends JpaRepository<UserCredentialEntity, String> {
}
