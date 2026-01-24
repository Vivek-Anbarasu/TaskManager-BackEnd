package com.taskmanager;

import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TasksManagementApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
	}

	@Test
	void applicationContextIsNotNull() {
		assertNotNull(applicationContext);
	}

	@Test
	void mainMethodStartsSpringBootApplication() {
		assertDoesNotThrow(() -> {
			String[] args = {};
			TaskManagementApplication.main(args);
		});
	}

	@Test
	void applicationHasSecuritySchemeAnnotation() {
		SecurityScheme securityScheme = TaskManagementApplication.class.getAnnotation(SecurityScheme.class);

		assertNotNull(securityScheme);
		assertEquals("Bearer Authentication", securityScheme.name());
		assertEquals("bearer", securityScheme.scheme());
		assertEquals("JWT", securityScheme.bearerFormat());
	}

	@Test
	void applicationIsAnnotatedWithSpringBootApplication() {
		assertTrue(TaskManagementApplication.class.isAnnotationPresent(org.springframework.boot.autoconfigure.SpringBootApplication.class));
	}


	@Test
	void applicationContextContainsRegistrationServiceBean() {
		assertTrue(applicationContext.containsBean("registrationService"));
	}

	@Test
	void applicationContextContainsJWTServiceBean() {
		assertTrue(applicationContext.containsBean("JWTService"));
	}

	@Test
	void applicationContextContainsUserDetailsServiceImplBean() {
		assertTrue(applicationContext.containsBean("userDetailsServiceImpl"));
	}

	@Test
	void applicationContextContainsTaskManagementControllerBean() {
		assertTrue(applicationContext.containsBean("taskManagementController"));
	}

	@Test
	void applicationContextContainsUserServicesControllerBean() {
		assertTrue(applicationContext.containsBean("userServicesController"));
	}

	@Test
	void applicationContextContainsSecurityConfigurationBean() {
		assertTrue(applicationContext.containsBean("securityConfiguration"));
	}

	@Test
	void applicationContextContainsDataSourceBean() {
		assertTrue(applicationContext.containsBean("dataSource"));
	}

	@Test
	void allExpectedBeansAreLoadedInContext() {
		String[] beanNames = applicationContext.getBeanDefinitionNames();
		assertTrue(beanNames.length > 0);
	}

}
