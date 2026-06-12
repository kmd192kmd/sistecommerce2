package com.example.java.product.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.java.product.entity.Options;

import jakarta.persistence.LockModeType;

public interface OptionsRepository extends JpaRepository<Options, Long> {
    // Paging version for product options
    Page<Options> findByProductSeq(Long productSeq, Pageable pageable);
    
    /**
     * 결제 승인 시 재고 차감을 안전하게 처리하기 위한 잠금 조회.
     * 동시에 여러 사용자가 같은 옵션을 구매할 때 재고가 음수가 되는 것을 방지한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Options o where o.seq = :seq")
    Optional<Options> findBySeqForUpdate(@Param("seq") Long seq);
}
