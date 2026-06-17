package com.example.java.cart.repository;

import com.example.java.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    List<Cart> findByMember_Seq(Long memberSeq);
    
    /*
	    같은 회원이 같은 옵션을 이미 장바구니에 담았는지 확인할 때 사용합니다.
	
	    장바구니 담기 버튼을 여러 번 눌렀을 때
	    같은 상품 옵션이 여러 줄로 생기지 않게 하고,
	    기존 row의 quantity만 증가시키기 위한 조회입니다.
	*/
	Optional<Cart> findByMember_SeqAndOptions_Seq(Long memberSeq, Long optionsSeq);
	
	void deleteByMember_Seq(Long memberSeq);
	
	void deleteByMember_SeqAndOptions_SeqIn(Long memberSeq, List<Long> optionsSeqList);
}
