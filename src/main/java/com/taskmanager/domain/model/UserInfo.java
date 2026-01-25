package com.taskmanager.domain.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @EqualsAndHashCode
@EntityListeners(AuditingEntityListener.class)
public class UserInfo implements Serializable {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String password;
    private String country;
    private String role;
    private String firstname;
    private String lastname;

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
