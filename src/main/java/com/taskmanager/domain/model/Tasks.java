package com.taskmanager.domain.model;


import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity @Data @Builder @NoArgsConstructor @AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Tasks implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long taskId;

	private String title;
	private String description;
	private String status;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime created_date;

    @CreatedBy
    @Column(updatable = false, nullable = false)
    private String created_by;

    @LastModifiedDate
    @Column(nullable = true)
    private LocalDateTime last_modified_date;

    @LastModifiedBy
    @Column(nullable = true)
    private String last_modified_by;

}