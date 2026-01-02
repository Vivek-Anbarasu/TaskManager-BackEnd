package com.restapp;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;


import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.sql.DataSource;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@SpringBootApplication
@SecurityScheme(name = "Bearer Authentication", scheme = "bearer", bearerFormat = "JWT", type = SecuritySchemeType.HTTP, in = SecuritySchemeIn.HEADER)
public class TaskManagementApplication {
	public static void main(String[] args) {
        SpringApplication.run(TaskManagementApplication.class, args);
	}
}
