package com.taskmanager;

import com.taskmanager.util.ProfileUtil;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
@SecurityScheme(name = "Bearer Authentication", scheme = "bearer", bearerFormat = "JWT", type = SecuritySchemeType.HTTP, in = SecuritySchemeIn.HEADER)
@Slf4j
@RequiredArgsConstructor
public class TaskManagementApplication implements ApplicationRunner {

	private final ProfileUtil profileUtil;

	public static void main(String[] args) {
        SpringApplication.run(TaskManagementApplication.class, args);
	}

    @Override
    public void run(ApplicationArguments args) {
        List<Runnable> startupTasks = List.of(profileUtil::displayProfileInformation);
        startupTasks.forEach(Runnable::run);
    }
}
