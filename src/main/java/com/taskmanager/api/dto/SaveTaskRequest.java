package com.taskmanager.api.dto;

import com.taskmanager.validation.StatusValidator;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SaveTaskRequest {

  @NotNull(message = "Title is mandatory")
  @NotEmpty(message = "Title is mandatory")
  private String title;

  @NotNull(message = "Description is mandatory")
  @NotEmpty(message = "Description is mandatory")
  private String description;

  @NotNull(message = "Status is mandatory")
  @NotEmpty(message = "Status is mandatory")
  @StatusValidator
  private String status;

}
