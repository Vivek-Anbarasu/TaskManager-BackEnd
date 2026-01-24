package com.restapp.config;

import com.restapp.exception.ErrorResponse;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SwaggerConfigurationTest {

    private final SwaggerConfiguration cfg = new SwaggerConfiguration();

    @Test
    void openApiBeanProvidesComponents() {
        OpenAPI openAPI = cfg.customOpenAPI();
        assertNotNull(openAPI);
        assertNotNull(openAPI.getComponents());
    }

    @Test
    void apiResponsesAreNotNull() {
        assertNotNull(SwaggerConfiguration.badRequestApiResponse);
        assertNotNull(SwaggerConfiguration.internalServerApiResponse);
        assertNotNull(SwaggerConfiguration.notFoundServerApiResponse);
    }
}
