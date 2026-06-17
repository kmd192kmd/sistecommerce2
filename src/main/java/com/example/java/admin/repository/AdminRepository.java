package com.example.java.admin.repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.java.admin.entity.Admin;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    
    @Query("SELECT a FROM Admin a WHERE a.id = :id")
    Optional<Admin> findByAdminId(@Param("id") String id);
}
