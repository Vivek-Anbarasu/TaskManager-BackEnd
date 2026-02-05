package com.taskmanager.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;


@Slf4j
@RequiredArgsConstructor
@Component
public class ProfileUtil {

    private final Environment environment;

    public void displayProfileInformation() {
        String[] activeProfiles = environment.getActiveProfiles();
        String[] defaultProfiles = environment.getDefaultProfiles();

        log.info("================== PROFILE INFORMATION ==================");

        if (activeProfiles.length > 0) {
            log.info("Active Profiles: {}", Arrays.asList(activeProfiles));
        } else {
            log.info("Active Profiles: None");
        }

        if (defaultProfiles.length > 0) {
            log.info("Default Profiles: {}", Arrays.asList(defaultProfiles));
        }

        log.info("=========================================================");
    }
}
