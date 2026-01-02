package com.restapp.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Builder
public class GetTaskResponse {

  private Integer id;

  private String title;

  private String description;

  private String status;

 
}

