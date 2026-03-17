package com.cloudmedia.policy.persistence.repository;

import com.cloudmedia.policy.persistence.entity.ContentPolicyEntity;
import com.cloudmedia.policy.persistence.entity.ModerationState;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class ContentPolicyRepositoryTest {

	@Autowired
	private ContentPolicyRepository contentPolicyRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void savesAndLoadsContentPolicy() {
		ContentPolicyEntity policy = new ContentPolicyEntity();
		policy.setContentId("cnt_1");
		policy.setAgeRestricted(true);
		policy.setGeoAllowList("TR,DE");
		policy.setGeoBlockList("US");
		policy.setModerationState(ModerationState.VISIBLE);

		contentPolicyRepository.saveAndFlush(policy);
		entityManager.clear();

		var found = contentPolicyRepository.findById("cnt_1");
		assertTrue(found.isPresent());
		assertTrue(found.get().isAgeRestricted());
		assertEquals("TR,DE", found.get().getGeoAllowList());
		assertEquals("US", found.get().getGeoBlockList());
		assertEquals(ModerationState.VISIBLE, found.get().getModerationState());
		assertTrue(found.get().getCreatedAt() != null);
		assertTrue(found.get().getUpdatedAt() != null);
	}
}
