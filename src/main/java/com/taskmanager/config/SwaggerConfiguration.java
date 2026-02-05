package com.taskmanager.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.core.converter.ModelConverters;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

@Configuration
@Slf4j
public class SwaggerConfiguration {

    public static final ApiResponse internalServerApiResponse;
    public static final ApiResponse badRequestApiResponse;
    public static final ApiResponse notFoundServerApiResponse;

    static {
        // Register ApiResponse objects that reference the ErrorResponse schema from components
        Schema<?> refSchema = new Schema<>().$ref("#/components/schemas/ErrorResponse");

        internalServerApiResponse = new ApiResponse().description("Internal Server Error")
                .content(new Content().addMediaType("application/json", new MediaType().schema(refSchema)));

        badRequestApiResponse = new ApiResponse().description("Bad request")
                .content(new Content().addMediaType("application/json", new MediaType().schema(refSchema)));

        notFoundServerApiResponse = new ApiResponse().description("Not Found")
                .content(new Content().addMediaType("application/json", new MediaType().schema(refSchema)));
    }

    @Bean
    public OperationCustomizer customize() {
        return this::customize;
    }

    private Operation customize(Operation operation, HandlerMethod handlerMethod) {
        ApiResponses apiResponses = operation.getResponses();

        // Add common error responses to all operations
        // 400 - Bad Request (for validation errors, duplicate entries, etc.)
        if (!apiResponses.containsKey("400")) {
            apiResponses.addApiResponse("400", badRequestApiResponse);
        }

        // 401 - Unauthorized (for authentication failures)
        if (!apiResponses.containsKey("401")) {
            Schema<?> refSchema = new Schema<>().$ref("#/components/schemas/ErrorResponse");
            ApiResponse unauthorizedResponse = new ApiResponse().description("Unauthorized - Invalid or missing authentication token")
                    .content(new Content().addMediaType("application/json", new MediaType().schema(refSchema)));
            apiResponses.addApiResponse("401", unauthorizedResponse);
        }

        // 404 - Not Found (for resources that don't exist)
        if (!apiResponses.containsKey("404")) {
            apiResponses.addApiResponse("404", notFoundServerApiResponse);
        }

        // 500 - Internal Server Error
        if (!apiResponses.containsKey("500")) {
            apiResponses.addApiResponse("500", internalServerApiResponse);
        }

        return operation;
    }

    // Add ErrorResponse schema to OpenAPI components so the $ref resolves in generated docs
    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI();
        Components components = new Components();

        // Add Bearer Token security scheme
        components.addSecuritySchemes("Bearer Authentication",
            new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter JWT token obtained from /user/authenticate endpoint"));

        // Use ModelConverters to read the ErrorResponse POJO and add its schema
        @SuppressWarnings("unchecked")
        Map<String, Schema<?>> schemas = (Map<String, Schema<?>>) (Map) ModelConverters.getInstance().read(com.taskmanager.exception.ErrorResponse.class);
        if (schemas != null) {
            schemas.forEach(components::addSchemas);
        }

        openAPI.setComponents(components);

        // Add API information
        openAPI.info(new Info()
            .title("Task Management Application API")
            .description("RESTful API for managing tasks with user authentication using JWT tokens")
            .version("1.0.0")
            .contact(new Contact()
                .name("TaskManager Team")
                .email("taskmanager@example.com")));

        return openAPI;
    }
}
