package com.cloudmedia.policy.persistence.repository;

import com.cloudmedia.policy.persistence.entity.ContentPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentPolicyRepository extends JpaRepository<ContentPolicyEntity, String> {
}
