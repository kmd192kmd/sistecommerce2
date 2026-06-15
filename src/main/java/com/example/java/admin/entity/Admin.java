package com.example.java.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "admin")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin implements java.io.Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "admin_seq")
    @SequenceGenerator(
            name = "admin_seq",
            sequenceName = "admin_seq",
            allocationSize = 1
    )
    private Long seq;

    @Column(name = "id", nullable = false, unique = true, length = 30)
    private String id;

    @Column(name = "password", nullable = false, length = 300)
    private String password;

    // 0: 권한없음, 1: 최고관리자, 2: CS관리자 ...
    @Column(name = "adm_role", nullable = false)
    private Integer admRole;

    // 0: 활성, 1: 비활성
    @Column(name = "adm_status", nullable = false)
    private Integer admStatus;

    @Column(name = "role", nullable = false, length = 250)
    private String role;
}