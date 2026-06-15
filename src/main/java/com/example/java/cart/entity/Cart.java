package com.example.java.cart.entity;

import com.example.java.member.entity.Member;
import com.example.java.product.entity.Options;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "cart")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cart_seq")
    @SequenceGenerator(name = "cart_seq", sequenceName = "cart_seq", allocationSize = 1)
    private Long seq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_seq", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "options_seq", nullable = false)
    private Options options;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "created_date", nullable = false)
    private LocalDate createdDate;

    
    /*
	    같은 회원이 같은 옵션을 이미 장바구니에 담은 경우,
	    새 row를 INSERT하지 않고 기존 장바구니 수량만 증가시키기 위해 사용합니다.
	
	    예:
	    기존 수량 2개
	    추가 수량 1개
	    결과 수량 3개
	*/
	public void increaseQuantity(int quantity) {

	    if (this.quantity == null) {
	        this.quantity = 0;
	    }

	    this.quantity += quantity;
	}

	/** 수량을 지정값으로 덮어씁니다. */
	public void updateQuantity(int quantity) {
	    this.quantity = quantity;
	}

}
