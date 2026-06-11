package com.example.java.member.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.java.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long>{
	Optional<Member> findByUsername(String username);

	boolean existsByUsername(String username);

	boolean existsByNickname(String nickname);

	boolean existsByEmail(String email);

	List<Member> findByNameAndEmail(String name, String email);

	Optional<Member> findByUsernameAndEmail(String username, String email);

	/**
	 * 휴면 사전 안내 대상: 마지막 접속(없으면 가입일)이 [start, end) 구간에 든 활성 회원.
	 * 스케줄러가 1일 윈도우로 호출 → 회원당 1회만 매칭.
	 */
	@Query("select m from Member m where m.status = :status "
			+ "and coalesce(m.lastLoginAt, m.joinedAt) >= :start "
			+ "and coalesce(m.lastLoginAt, m.joinedAt) < :end")
	List<Member> findDormantNoticeTargets(@Param("status") int status,
										  @Param("start") LocalDateTime start,
										  @Param("end") LocalDateTime end);

	/** 휴면 전환 대상: 마지막 접속(없으면 가입일)이 cutoff 이전인 활성 회원의 seq */
	@Query("select m.seq from Member m where m.status = :status "
			+ "and coalesce(m.lastLoginAt, m.joinedAt) < :cutoff")
	List<Long> findDormantTargetSeqs(@Param("status") int status,
									 @Param("cutoff") LocalDateTime cutoff);
}
