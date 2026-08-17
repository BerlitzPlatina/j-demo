package com.example.mq.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * The context has to come up without a broker, so topic creation is allowed to fail and the
 * listener containers stay down.
 */
@SpringBootTest
@TestPropertySource(properties = {
		"spring.kafka.admin.fail-fast=false",
		"spring.kafka.listener.auto-startup=false"
})
class MqKafkaApplicationTests {

	@Test
	void contextLoads() {
	}

}
