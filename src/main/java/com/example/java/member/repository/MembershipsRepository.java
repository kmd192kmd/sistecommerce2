package com.example.java.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.member.entity.Memberships;

public interface MembershipsRepository extends JpaRepository<Memberships, Long>{

}
