package com.example.java.member.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.java.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long>{
	Optional<Member> findByUsername(String username);

	boolean existsByUsername(String username);

	boolean existsByNickname(String nickname);

	List<Member> findByNameAndEmail(String name, String email);

	Optional<Member> findByUsernameAndEmail(String username, String email);
}
