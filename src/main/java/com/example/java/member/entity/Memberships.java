package com.example.java.member.entity;

import java.time.LocalDateTime;

import com.example.java.member.repository.MembershipsRepository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "memberships")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Memberships {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "memberships_seq")
	@SequenceGenerator(name = "memberships_seq", sequenceName = "memberships_seq",  allocationSize = 1)
	private Long seq;
	
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_seq", nullable = false)
	private Member memberSeq;
	
	@Column(name = "billing_key", length = 200)
	private String billingKey;
	
	@Column(name = "status", length = 20)
	private String status;
	
	@Column(name = "started_at")
	private LocalDateTime startedAt;
	
	@Column(name = "expire_at")
	private LocalDateTime expireAt;
	
	@Column(name = "next_billing_at")
	private LocalDateTime nextBillingAt;
	
	@Column(name = "canceled_at")
	private LocalDateTime canceledAt;
	
}
