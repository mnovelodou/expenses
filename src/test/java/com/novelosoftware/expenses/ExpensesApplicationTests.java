package com.novelosoftware.expenses;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;

/**
 * Test class for {@link ExpensesApplication.class}
 */
@SpringBootTest(properties = {
		"spring.sql.init.mode=never",
		"spring.security.oauth2.resourceserver.jwt.public-key-location=classpath:local/test-rsa-public.pem"
})
class ExpensesApplicationTests {

	@MockitoBean
	DataSource dataSource;

	@Test
	void contextLoads() {
	}

}
