package com.cloudmedia.policy.persistence.repository;

import com.cloudmedia.policy.persistence.entity.ContentPolicyEntity;
import com.cloudmedia.policy.persistence.entity.ModerationState;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class ContentPolicyRepositoryTest {

	@Autowired
	private ContentPolicyRepository contentPolicyRepository;

	@Test
	void savesAndLoadsContentPolicy() {
		ContentPolicyEntity policy = new ContentPolicyEntity();
		policy.setContentId("cnt_1");
		policy.setAgeRestricted(true);
		policy.setGeoAllowList("TR,DE");
		policy.setGeoBlockList("US");
		policy.setModerationState(ModerationState.VISIBLE);
		policy.setCreatedAt(LocalDateTime.now());
		policy.setUpdatedAt(LocalDateTime.now());

		contentPolicyRepository.saveAndFlush(policy);

		var found = contentPolicyRepository.findById("cnt_1");
		assertTrue(found.isPresent());
		assertTrue(found.get().isAgeRestricted());
		assertEquals("TR,DE", found.get().getGeoAllowList());
		assertEquals("US", found.get().getGeoBlockList());
		assertEquals(ModerationState.VISIBLE, found.get().getModerationState());
	}
}
