package com.taskmanager.domain.model;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.io.Serializable;

@Entity @Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Tasks implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long taskId;

	private String title;
	private String description;
	private String status;

}