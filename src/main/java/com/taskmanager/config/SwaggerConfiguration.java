package com.taskmanager.config;

import com.taskmanager.exception.BadRequest;
import com.taskmanager.exception.InternalServerError;
import com.taskmanager.exception.NotFound;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.core.converter.ModelConverters;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.List;

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
        List<Class<?>> exceptions = Arrays.asList(handlerMethod.getMethod().getExceptionTypes());
        ApiResponses apiResponse = 	operation.getResponses();

        if(exceptions.contains(InternalServerError.class)) {
        apiResponse.addApiResponse("500", internalServerApiResponse);
        }

        if(exceptions.contains(NotFound.class)) {
        apiResponse.addApiResponse("404", notFoundServerApiResponse);
        }

        if(exceptions.contains(BadRequest.class)) {
        apiResponse.addApiResponse("400", badRequestApiResponse);
        }

        return operation;
    }

    // Add ErrorResponse schema to OpenAPI components so the $ref resolves in generated docs
    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI();
        Components components = new Components();

        // Use ModelConverters to read the ErrorResponse POJO and add its schema
        @SuppressWarnings("unchecked")
        Map<String, Schema<?>> schemas = (Map<String, Schema<?>>) (Map) ModelConverters.getInstance().read(com.taskmanager.exception.ErrorResponse.class);
        if (schemas != null) {
            schemas.forEach(components::addSchemas);
        }

        openAPI.setComponents(components);
        return openAPI;
    }
}
