package com.restapp.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;


@Entity
@Getter @Setter @EqualsAndHashCode 
public class Tasks implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer taskId;

	private String title;
	private String description;
	private String status;

}