package com.novelosoftware.expenses;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;

@SpringBootTest(properties = "spring.sql.init.mode=never")
class ExpensesApplicationTests {

	@MockitoBean
	DataSource dataSource;

	@Test
	void contextLoads() {
	}

}
