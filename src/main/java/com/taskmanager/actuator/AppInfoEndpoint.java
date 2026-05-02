package com.taskmanager.actuator;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.SpringBootVersion;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custom Actuator Endpoint — exposes application metadata at /actuator/app-info
 *
 * Accessible via: GET http://localhost:8080/actuator/app-info
 *
 * Shows:
 *  - Java version (runtime)
 *  - Spring Boot version
 *  - Application name  (spring.application.name)
 *  - Application version (project.version from pom.xml via @Value)
 *  - Database URL       (spring.datasource.url  — masked for security)
 */
@Component
@Endpoint(id = "app-info")
@RequiredArgsConstructor
public class AppInfoEndpoint {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    /**
     * The project version is injected from the Maven artifact version.
     * Requires the following in application.yaml / build config or relies
     * on /actuator/info with build-info.  Here we read it from the manifest.
     */
    @Value("${app.version:1.0-RELEASE}")
    private String applicationVersion;

    @ReadOperation
    public Map<String, Object> appInfo() {

        Map<String, Object> info = new LinkedHashMap<>();

        // --- Java Runtime ---
        info.put("java-version", System.getProperty("java.version"));

        // --- Spring Boot ---
        info.put("spring-boot-version", SpringBootVersion.getVersion());

        // --- Application ---
        info.put("application-name", applicationName);
        info.put("application-version", applicationVersion);

        // --- Database (mask credentials, keep host+db) ---
        info.put("db-url", maskDbUrl(datasourceUrl));

        return info;
    }

    /**
     * Strips username/password tokens that may appear in JDBC URLs and
     * returns only the connection coordinates (host, port, database name).
     *
     * Example:
     *   in  → jdbc:postgresql://127.0.0.1:5432/taskdb
     *   out → jdbc:postgresql://127.0.0.1:5432/taskdb   (already safe)
     *
     *   in  → jdbc:mysql://user:pass@localhost:3306/mydb
     *   out → jdbc:mysql://localhost:3306/mydb           (credentials removed)
     */
    private String maskDbUrl(String url) {
        if (url == null) {
            return "N/A";
        }
        // Remove embedded user:pass@ patterns (MySQL / MariaDB style)
        return url.replaceAll("(//)[^@]+@", "$1");
    }
}

