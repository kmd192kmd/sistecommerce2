package com.example.java.product.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import com.example.java.product.dto.PurchasedOrderItemDto;
import com.example.java.product.dto.ReviewCreateRequestDto;
import com.example.java.product.dto.ReviewScrollResponseDto;
import com.example.java.product.service.ReviewService;
import com.example.java.product.dto.ReviewEligibilityDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ReviewController {
	
	/*
	    리뷰 작성 가능 여부 확인
	
	    리뷰 등록 버튼을 눌렀을 때 먼저 호출됩니다.
	
	    예:
	    GET /products/1451/reviews/writable?memberSeq=11
	*/
	@GetMapping("/products/{productSeq}/reviews/writable")
	public ResponseEntity<ReviewEligibilityDto> checkReviewWritable(
	        @PathVariable(name = "productSeq") Long productSeq,
	        @RequestParam(name = "memberSeq") Long memberSeq) {
	
	    ReviewEligibilityDto result =
	            reviewService.checkReviewWritable(productSeq, memberSeq);
	
	    return ResponseEntity.ok(result);
	}
	
	/*
	    상품 상세 화면에서 리뷰 등록
	
	    SecurityConfig를 수정하지 않기 위해
	    /api/** 주소가 아니라 /products/{productSeq}/reviews 주소를 사용합니다.
	
	    multipart/form-data 방식으로 전송합니다.
	*/
	@PostMapping(value = "/products/{productSeq}/reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> createReviewFromProductPage(
	        @PathVariable(name = "productSeq") Long productSeq,
	        @RequestParam(name = "memberSeq") Long memberSeq,
	        @RequestParam(name = "orderItemSeq") Long orderItemSeq,
	        @RequestParam(name = "rating") Integer rating,
	        @RequestParam(name = "content") String content,
	        @RequestParam(name = "images", required = false) List<MultipartFile> images) {
	
	    ReviewCreateRequestDto dto = new ReviewCreateRequestDto();
	    dto.setProductSeq(productSeq);
	    dto.setMemberSeq(memberSeq);
	    dto.setOrderItemSeq(orderItemSeq);
	    dto.setRating(rating);
	    dto.setContent(content);
	
	    reviewService.createReviewWithImages(dto, images);
	
	    return ResponseEntity.ok("리뷰가 등록되었습니다.");
	}
	
	/*
	    상품 상세 화면에서 리뷰 수정
	
	    이미지 파일이 포함될 수 있으므로 multipart/form-data 방식으로 받습니다.
	
	    예:
	    POST /products/1451/reviews/10/edit
	*/
	@PostMapping(value = "/products/{productSeq}/reviews/{reviewSeq}/edit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> updateReviewFromProductPage(
	        @PathVariable(name = "productSeq") Long productSeq,
	        @PathVariable(name = "reviewSeq") Long reviewSeq,
	        @RequestParam(name = "memberSeq") Long memberSeq,
	        @RequestParam(name = "rating") Integer rating,
	        @RequestParam(name = "content") String content,
	        @RequestParam(name = "images", required = false) List<MultipartFile> images) {
	
	    reviewService.updateReviewWithImages(
	            reviewSeq,
	            productSeq,
	            memberSeq,
	            rating,
	            content,
	            images
	    );
	
	    return ResponseEntity.ok("리뷰가 수정되었습니다.");
	}
	
	/*
	    리뷰 작성 가능한 주문상품 목록 조회
	
	    리뷰 등록 폼에서 주문상품번호를 직접 입력하지 않고,
	    상품명과 옵션명을 선택할 수 있도록 목록을 내려줍니다.
	
	    예:
	    GET /products/1451/reviews/order-items?memberSeq=11
	*/
	@GetMapping("/products/{productSeq}/reviews/order-items")
	public ResponseEntity<List<PurchasedOrderItemDto>> getReviewWritableOrderItems(
	        @PathVariable(name = "productSeq") Long productSeq,
	        @RequestParam(name = "memberSeq") Long memberSeq) {
	
	    List<PurchasedOrderItemDto> result =
	            reviewService.getPurchasedOrderItems(productSeq, memberSeq);
	
	    return ResponseEntity.ok(result);
	}
	
	/*
	    상품 상세 화면에서 리뷰 삭제
	
	    예:
	    POST /products/1451/reviews/10/delete
	*/
	@PostMapping("/products/{productSeq}/reviews/{reviewSeq}/delete")
	public ResponseEntity<?> deleteReviewFromProductPage(
	        @PathVariable(name = "productSeq") Long productSeq,
	        @PathVariable(name = "reviewSeq") Long reviewSeq,
	        @RequestParam(name = "memberSeq") Long memberSeq) {
	
	    reviewService.deleteReview(reviewSeq, productSeq, memberSeq);
	
	    return ResponseEntity.ok("리뷰가 삭제되었습니다.");
	}
	
	/*
	    상품 상세 화면에서 리뷰 목록 조회
	
	    화면에서 fetch로 호출하는 주소입니다.
	
	    예:
	    GET /products/1451/reviews-data?size=10
	    GET /products/1451/reviews-data?lastReviewSeq=20&size=10
	*/
	@GetMapping("/products/{productSeq}/reviews-data")
	public ResponseEntity<ReviewScrollResponseDto> getProductReviewsForPage(
	        @PathVariable(name = "productSeq") Long productSeq,
	        @RequestParam(name = "lastReviewSeq", required = false) Long lastReviewSeq,
	        @RequestParam(name = "size", defaultValue = "10") int size) {
	
	    ReviewScrollResponseDto result =
	            reviewService.getProductReviewsByScroll(productSeq, lastReviewSeq, size);
	
	    return ResponseEntity.ok(result);
	}

    // 리뷰 관련 비즈니스 로직을 처리하는 Service
    private final ReviewService reviewService;

    // 전체 리뷰 무한스크롤 조회
    // 처음 조회: GET /api/reviews?size=10
    // 다음 조회: GET /api/reviews?lastReviewSeq=15&size=10
    @GetMapping("/api/reviews")
    public ResponseEntity<ReviewScrollResponseDto> getAllReviews(
            @RequestParam(name = "lastReviewSeq", required = false) Long lastReviewSeq,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        // 마지막으로 조회한 리뷰 번호를 기준으로 다음 리뷰 목록 조회
        ReviewScrollResponseDto result =
                reviewService.getAllReviewsByScroll(lastReviewSeq, size);

        // 조회 결과를 JSON으로 응답
        return ResponseEntity.ok(result);
    }

    // 특정 상품의 리뷰 무한스크롤 조회
    // 처음 조회: GET /api/product/1494/reviews?size=10
    // 다음 조회: GET /api/product/1494/reviews?lastReviewSeq=15&size=10
    @GetMapping("/api/product/{productSeq}/reviews")
    public ResponseEntity<ReviewScrollResponseDto> getProductReviews(
            @PathVariable(name = "productSeq") Long productSeq,
            @RequestParam(name = "lastReviewSeq", required = false) Long lastReviewSeq,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        // 상품 번호와 마지막 리뷰 번호를 기준으로 다음 리뷰 목록 조회
        ReviewScrollResponseDto result =
                reviewService.getProductReviewsByScroll(productSeq, lastReviewSeq, size);

        // 조회 결과를 JSON으로 응답
        return ResponseEntity.ok(result);
    }
}