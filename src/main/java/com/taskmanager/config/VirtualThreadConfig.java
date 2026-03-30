package com.taskmanager.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.util.concurrent.Executors;

/**
 * Configures virtual threads (Project Loom) for all request-handling threads.
 *
 * <p>When {@code spring.threads.virtual.enabled=true}:
 * <ul>
 *   <li>Spring Boot auto-configures Tomcat to use one virtual thread per request.</li>
 *   <li>This bean also applies virtual threads to {@code @Async} and Spring's task executor.</li>
 * </ul>
 *
 * <p>No changes are needed in controllers or services — virtual threads are
 * completely transparent to application code. They are ideal for I/O-bound
 * workloads (JPA queries, HTTP calls, etc.) because they park cheaply while
 * waiting, freeing the carrier thread for other work.
 */
@Configuration
@ConditionalOnProperty(name = "spring.threads.virtual.enabled", havingValue = "true")
public class VirtualThreadConfig {

    /**
     * Replaces Spring's default async task executor with a virtual-thread-per-task executor.
     * Used by {@code @Async} methods and Spring's internal task scheduling.
     */
    @Bean
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}
