package com.restapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restapp.exception.BadRequest;
import com.restapp.exception.ErrorResponse;
import com.restapp.exception.InternalServerError;
import com.restapp.exception.NotFound;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SwaggerConfiguration {

	public static final ApiResponse internalServerApiResponse;
	public static final ApiResponse badRequestApiResponse;
	public static final ApiResponse notFoundServerApiResponse;
	
	static {
		ErrorResponse error500 = new ErrorResponse();
		error500.setCode(500);
		error500.setMessage("string");
		
		ErrorResponse error400 = new ErrorResponse();
		error400.setCode(400);
		error400.setMessage("string");
		
		ErrorResponse error404 = new ErrorResponse();
		error404.setCode(404);
		error404.setMessage("string");
		
		StringSchema internalServerStringSchema = new StringSchema();
		internalServerStringSchema.setType("json");
		
		StringSchema badRequestStringSchema = new StringSchema();
		badRequestStringSchema.setType("json");
		
		StringSchema  notFoundStringSchema = new StringSchema();
		notFoundStringSchema.setType("json");
		
		try {
			internalServerStringSchema.setExample(new ObjectMapper().writeValueAsString(error500));
			badRequestStringSchema.setExample(new ObjectMapper().writeValueAsString(error400));
			notFoundStringSchema.setExample(new ObjectMapper().writeValueAsString(error404));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		internalServerApiResponse = new ApiResponse().description("Internal Server Error").
				content(new Content().addMediaType("application/json", new MediaType().schema(internalServerStringSchema)));
		
		badRequestApiResponse = new ApiResponse().description("Bad request").
				content(new Content().addMediaType("application/json", new MediaType().schema(badRequestStringSchema)));
		
		notFoundServerApiResponse = new ApiResponse().description("Not Found").
				content(new Content().addMediaType("application/json", new MediaType().schema(notFoundStringSchema)));
	}
	
	@Bean
	public OperationCustomizer customize() {
		return (operation, handlerMethod) -> customize(operation, handlerMethod);
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
	
}
