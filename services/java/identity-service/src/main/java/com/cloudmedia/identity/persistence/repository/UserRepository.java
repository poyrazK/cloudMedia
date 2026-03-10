package com.cloudmedia.identity.persistence.repository;

import com.cloudmedia.identity.persistence.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {
	Optional<UserEntity> findByEmail(String email);
}
