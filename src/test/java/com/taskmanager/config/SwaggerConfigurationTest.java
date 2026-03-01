package com.taskmanager.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SwaggerConfigurationTest {

    private SwaggerConfiguration cfg;

    @BeforeEach
    void setUp() {
        cfg = new SwaggerConfiguration();
    }

    @Test
    void openApiBeanProvidesComponents() {
        OpenAPI openAPI = cfg.customOpenAPI();
        assertNotNull(openAPI);
        assertNotNull(openAPI.getComponents());
    }

    @Test
    void openApiBeanContainsBearerSecurityScheme() {
        OpenAPI openAPI = cfg.customOpenAPI();
        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSecuritySchemes());

        SecurityScheme securityScheme = openAPI.getComponents().getSecuritySchemes().get("Bearer Authentication");
        assertNotNull(securityScheme);
        assertEquals(SecurityScheme.Type.HTTP, securityScheme.getType());
        assertEquals("bearer", securityScheme.getScheme());
        assertEquals("JWT", securityScheme.getBearerFormat());
    }

    @Test
    void openApiBeanContainsErrorResponseSchema() {
        OpenAPI openAPI = cfg.customOpenAPI();
        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSchemas());
        assertTrue(openAPI.getComponents().getSchemas().containsKey("ErrorResponse"));
    }

    @Test
    void openApiBeanContainsApiInfo() {
        OpenAPI openAPI = cfg.customOpenAPI();
        assertNotNull(openAPI.getInfo());
        assertEquals("Task Management Application API", openAPI.getInfo().getTitle());
        assertEquals("1.0.0", openAPI.getInfo().getVersion());
        assertNotNull(openAPI.getInfo().getDescription());
        assertNotNull(openAPI.getInfo().getContact());
        assertEquals("TaskManager Team", openAPI.getInfo().getContact().getName());
        assertEquals("taskmanager@example.com", openAPI.getInfo().getContact().getEmail());
    }

    @Test
    void apiResponsesAreNotNull() {
        assertNotNull(SwaggerConfiguration.badRequestApiResponse);
        assertNotNull(SwaggerConfiguration.internalServerApiResponse);
        assertNotNull(SwaggerConfiguration.notFoundServerApiResponse);
    }

    @Test
    void apiResponsesContainCorrectDescriptions() {
        assertEquals("Bad request", SwaggerConfiguration.badRequestApiResponse.getDescription());
        assertEquals("Internal Server Error", SwaggerConfiguration.internalServerApiResponse.getDescription());
        assertEquals("Not Found", SwaggerConfiguration.notFoundServerApiResponse.getDescription());
    }

    @Test
    void apiResponsesContainContent() {
        assertNotNull(SwaggerConfiguration.badRequestApiResponse.getContent());
        assertNotNull(SwaggerConfiguration.badRequestApiResponse.getContent().get("application/json"));

        assertNotNull(SwaggerConfiguration.internalServerApiResponse.getContent());
        assertNotNull(SwaggerConfiguration.internalServerApiResponse.getContent().get("application/json"));

        assertNotNull(SwaggerConfiguration.notFoundServerApiResponse.getContent());
        assertNotNull(SwaggerConfiguration.notFoundServerApiResponse.getContent().get("application/json"));
    }

    @Test
    void operationCustomizerBeanIsCreated() {
        OperationCustomizer customizer = cfg.customize();
        assertNotNull(customizer);
    }

    @Test
    void operationCustomizerAdds400Response() {
        OperationCustomizer customizer = cfg.customize();
        Operation operation = new Operation();
        operation.setResponses(new ApiResponses());
        HandlerMethod handlerMethod = mock(HandlerMethod.class);

        Operation customizedOperation = customizer.customize(operation, handlerMethod);

        assertNotNull(customizedOperation.getResponses());
        assertTrue(customizedOperation.getResponses().containsKey("400"));
        assertEquals("Bad request", customizedOperation.getResponses().get("400").getDescription());
    }

    @Test
    void operationCustomizerAdds401Response() {
        OperationCustomizer customizer = cfg.customize();
        Operation operation = new Operation();
        operation.setResponses(new ApiResponses());
        HandlerMethod handlerMethod = mock(HandlerMethod.class);

        Operation customizedOperation = customizer.customize(operation, handlerMethod);

        assertNotNull(customizedOperation.getResponses());
        assertTrue(customizedOperation.getResponses().containsKey("401"));
        ApiResponse unauthorizedResponse = customizedOperation.getResponses().get("401");
        assertNotNull(unauthorizedResponse);
        assertTrue(unauthorizedResponse.getDescription().contains("Unauthorized"));
    }

    @Test
    void operationCustomizerAdds404Response() {
        OperationCustomizer customizer = cfg.customize();
        Operation operation = new Operation();
        operation.setResponses(new ApiResponses());
        HandlerMethod handlerMethod = mock(HandlerMethod.class);

        Operation customizedOperation = customizer.customize(operation, handlerMethod);

        assertNotNull(customizedOperation.getResponses());
        assertTrue(customizedOperation.getResponses().containsKey("404"));
        assertEquals("Not Found", customizedOperation.getResponses().get("404").getDescription());
    }

    @Test
    void operationCustomizerAdds500Response() {
        OperationCustomizer customizer = cfg.customize();
        Operation operation = new Operation();
        operation.setResponses(new ApiResponses());
        HandlerMethod handlerMethod = mock(HandlerMethod.class);

        Operation customizedOperation = customizer.customize(operation, handlerMethod);

        assertNotNull(customizedOperation.getResponses());
        assertTrue(customizedOperation.getResponses().containsKey("500"));
        assertEquals("Internal Server Error", customizedOperation.getResponses().get("500").getDescription());
    }

    @Test
    void operationCustomizerDoesNotOverrideExisting400Response() {
        OperationCustomizer customizer = cfg.customize();
        Operation operation = new Operation();
        ApiResponses responses = new ApiResponses();
        ApiResponse customResponse = new ApiResponse().description("Custom 400");
        responses.addApiResponse("400", customResponse);
        operation.setResponses(responses);
        HandlerMethod handlerMethod = mock(HandlerMethod.class);

        Operation customizedOperation = customizer.customize(operation, handlerMethod);

        assertEquals("Custom 400", customizedOperation.getResponses().get("400").getDescription());
    }

    @Test
    void operationCustomizerDoesNotOverrideExisting401Response() {
        OperationCustomizer customizer = cfg.customize();
        Operation operation = new Operation();
        ApiResponses responses = new ApiResponses();
        ApiResponse customResponse = new ApiResponse().description("Custom 401");
        responses.addApiResponse("401", customResponse);
        operation.setResponses(responses);
        HandlerMethod handlerMethod = mock(HandlerMethod.class);

        Operation customizedOperation = customizer.customize(operation, handlerMethod);

        assertEquals("Custom 401", customizedOperation.getResponses().get("401").getDescription());
    }

    @Test
    void operationCustomizerDoesNotOverrideExisting404Response() {
        OperationCustomizer customizer = cfg.customize();
        Operation operation = new Operation();
        ApiResponses responses = new ApiResponses();
        ApiResponse customResponse = new ApiResponse().description("Custom 404");
        responses.addApiResponse("404", customResponse);
        operation.setResponses(responses);
        HandlerMethod handlerMethod = mock(HandlerMethod.class);

        Operation customizedOperation = customizer.customize(operation, handlerMethod);

        assertEquals("Custom 404", customizedOperation.getResponses().get("404").getDescription());
    }

    @Test
    void operationCustomizerDoesNotOverrideExisting500Response() {
        OperationCustomizer customizer = cfg.customize();
        Operation operation = new Operation();
        ApiResponses responses = new ApiResponses();
        ApiResponse customResponse = new ApiResponse().description("Custom 500");
        responses.addApiResponse("500", customResponse);
        operation.setResponses(responses);
        HandlerMethod handlerMethod = mock(HandlerMethod.class);

        Operation customizedOperation = customizer.customize(operation, handlerMethod);

        assertEquals("Custom 500", customizedOperation.getResponses().get("500").getDescription());
    }
}
