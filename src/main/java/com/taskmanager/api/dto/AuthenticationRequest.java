package com.taskmanager.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AuthenticationRequest(
        @NotNull(message = "Email is mandatory")
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email should be valid")
        String email,

        @NotNull(message = "Password is mandatory")
        @NotBlank(message = "Password cannot be blank")
        String password
) {}
