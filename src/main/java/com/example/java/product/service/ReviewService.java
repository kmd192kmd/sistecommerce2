package com.example.java.product.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.java.product.dto.ReviewImageDto;
import com.example.java.product.dto.ReviewResponseDto;
import com.example.java.product.dto.ReviewScrollResponseDto;
import com.example.java.product.repository.ProductRepository;
import com.example.java.product.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

    // 리뷰 DB 작업을 담당하는 Repository
    private final ReviewRepository reviewRepository;

    // 상품 DB 작업을 담당하는 Repository
    // 리뷰 등록, 수정, 삭제 후 상품의 평균 별점과 리뷰 수를 갱신할 때 사용
    private final ProductRepository productRepository;

    // 전체 리뷰 무한스크롤 조회
    public ReviewScrollResponseDto getAllReviewsByScroll(Long lastReviewSeq, int size) {

        // size 값이 1보다 작으면 기본값 10으로 보정
        if (size < 1) {
            size = 10;
        }

        // 너무 많은 데이터를 한 번에 가져오지 않도록 최대 50개로 제한
        if (size > 50) {
            size = 50;
        }

        // 다음 데이터 존재 여부를 판단하기 위해 요청 개수보다 1개 더 조회
        int limit = size + 1;

        // 전체 리뷰 목록을 무한스크롤 방식으로 조회
        List<ReviewResponseDto> reviews =
                reviewRepository.findAllReviewsByScroll(lastReviewSeq, limit);

        // 조회 결과를 무한스크롤 응답 형태로 변환
        return buildScrollResponse(reviews, size);
    }

    // 특정 상품의 리뷰 무한스크롤 조회
    public ReviewScrollResponseDto getProductReviewsByScroll(Long productSeq, Long lastReviewSeq, int size) {

        // 상품이 존재하지 않으면 404 응답
        if (!reviewRepository.existsProduct(productSeq)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다.");
        }

        // size 값이 1보다 작으면 기본값 10으로 보정
        if (size < 1) {
            size = 10;
        }

        // 너무 많은 데이터를 한 번에 가져오지 않도록 최대 50개로 제한
        if (size > 50) {
            size = 50;
        }

        // 다음 데이터 존재 여부를 판단하기 위해 요청 개수보다 1개 더 조회
        int limit = size + 1;

        // 특정 상품의 리뷰 목록을 무한스크롤 방식으로 조회
        List<ReviewResponseDto> reviews =
                reviewRepository.findProductReviewsByScroll(productSeq, lastReviewSeq, limit);

        // 조회 결과를 무한스크롤 응답 형태로 변환
        return buildScrollResponse(reviews, size);
    }

    // 상품 리뷰 통계 수동 갱신
    //
    // 특정 상품의 평균 별점과 리뷰 수를 review 테이블 기준으로 다시 계산합니다.
    //
    // 사용 예:
    // - 기존 리뷰 데이터가 있는데 product.avg_rating, product.review_count가 0일 때
    // - 리뷰 등록, 수정, 삭제 기능을 만들기 전에 임시로 갱신하고 싶을 때
    public void refreshProductReviewStats(Long productSeq) {

        // 상품이 존재하지 않으면 404 응답
        if (!reviewRepository.existsProduct(productSeq)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다.");
        }

        // 상품 리뷰 통계 갱신
        productRepository.updateProductReviewStats(productSeq);
    }

    // 리뷰 번호 기준 상품 리뷰 통계 갱신
    //
    // 리뷰 수정 또는 삭제 후 사용합니다.
    //
    // 리뷰 번호만 알고 있을 때,
    // 해당 리뷰가 어떤 상품에 속한 리뷰인지 조회한 뒤
    // 그 상품의 평균 별점과 리뷰 수를 다시 계산합니다.
    public void refreshProductReviewStatsByReviewSeq(Long reviewSeq) {

        // 리뷰 번호로 상품 번호 조회
        Long productSeq = reviewRepository.findProductSeqByReviewSeq(reviewSeq);

        // 리뷰가 없으면 404 응답
        if (productSeq == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다.");
        }

        // 상품 리뷰 통계 갱신
        productRepository.updateProductReviewStats(productSeq);
    }

    // 무한스크롤 조회 결과를 응답 DTO로 변환
    private ReviewScrollResponseDto buildScrollResponse(List<ReviewResponseDto> reviews, int size) {

        // 조회된 개수가 요청한 size보다 많으면 다음 데이터가 있다는 의미
        boolean hasNext = reviews.size() > size;

        // size + 1개를 조회한 경우 실제 응답에는 size개만 내려줌
        if (hasNext) {
            reviews = reviews.subList(0, size);
        }

        // 리뷰 번호만 따로 추출
        List<Long> reviewSeqs = reviews.stream()
                .map(ReviewResponseDto::getSeq)
                .collect(Collectors.toList());

        // 리뷰 번호 목록에 해당하는 이미지들을 한 번에 조회
        List<ReviewImageDto> images =
                reviewRepository.findImagesByReviewSeqs(reviewSeqs);

        // reviewSeq 기준으로 이미지들을 그룹화
        Map<Long, List<ReviewImageDto>> imageMap = images.stream()
                .collect(Collectors.groupingBy(ReviewImageDto::getReviewSeq));

        // 리뷰 DTO에 이미지 목록을 넣어서 다시 생성
        List<ReviewResponseDto> result = reviews.stream()
                .map(review -> ReviewResponseDto.builder()
                        .seq(review.getSeq())
                        .productSeq(review.getProductSeq())
                        .memberSeq(review.getMemberSeq())
                        .orderItemSeq(review.getOrderItemSeq())
                        .rating(review.getRating())
                        .content(review.getContent())
                        .createdDate(review.getCreatedDate())
                        .updatedDate(review.getUpdatedDate())
                        .status(review.getStatus())
                        .images(imageMap.getOrDefault(review.getSeq(), List.of()))
                        .build())
                .collect(Collectors.toList());

        // 다음 요청에 사용할 마지막 리뷰 번호
        Long nextLastReviewSeq = null;

        if (!result.isEmpty()) {
            nextLastReviewSeq = result.get(result.size() - 1).getSeq();
        }

        // 무한스크롤 응답 DTO 생성
        return ReviewScrollResponseDto.builder()
                .reviews(result)
                .nextLastReviewSeq(nextLastReviewSeq)
                .hasNext(hasNext)
                .build();
    }
}