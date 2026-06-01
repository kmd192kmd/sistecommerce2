package com.example.java.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, String>{

}
