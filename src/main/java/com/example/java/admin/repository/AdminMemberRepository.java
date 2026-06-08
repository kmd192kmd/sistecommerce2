package com.example.java.admin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.java.member.entity.Member;

// 1. class가 아니라 interface로 만듭니다.
// 2. JpaRepository<엔티티 클래스, PK의 데이터 타입> 을 상속받습니다.
@Repository
public interface AdminMemberRepository extends JpaRepository<Member, Long> {



    // 2번: 검색용 쿼리 메서드 (이름만 적어두면 구현은 스프링이 알아서 합니다!)
    

	List<Member> findByUsernameContainingOrNameContainingOrNicknameContaining(String username, String name, String
			  nickname);

}