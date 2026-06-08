package com.example.java.product.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.java.product.dto.ReviewEligibilityDto;
import com.example.java.product.dto.ReviewImageDto;
import com.example.java.product.dto.ReviewResponseDto;
import com.example.java.product.dto.ReviewScrollResponseDto;
import com.example.java.product.repository.ProductDetailRepository;
import com.example.java.product.repository.ReviewRepository;
import com.example.java.product.dto.PurchasedOrderItemDto;
import com.example.java.product.dto.ReviewCreateRequestDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {
	
	/*
	    리뷰 이미지 최대 업로드 개수
	*/
	private static final int MAX_REVIEW_IMAGE_COUNT = 5;
	
	/*
	    리뷰 이미지 개수 검증
	
	    리뷰 등록/수정 시 이미지는 최대 5개까지만 허용합니다.
	*/
	private void validateReviewImageCount(List<MultipartFile> images) {
	
	    if (images == null || images.isEmpty()) {
	        return;
	    }
	
	    long imageCount = images.stream()
	            .filter(image -> image != null && !image.isEmpty())
	            .count();
	
	    if (imageCount > MAX_REVIEW_IMAGE_COUNT) {
	        throw new ResponseStatusException(
	                HttpStatus.BAD_REQUEST,
	                "리뷰 이미지는 최대 5장까지 등록할 수 있습니다."
	        );
	    }
	}

    /*
        리뷰 작성 가능 여부 확인

        리뷰 등록 버튼을 눌렀을 때 먼저 호출됩니다.

        1. 로그인한 회원인지 확인
        2. 상품이 존재하는지 확인
        3. 이미 같은 상품에 리뷰를 작성했는지 확인
        4. 해당 상품을 구매 완료했는지 확인
        5. 모두 통과하면 리뷰 작성 가능
    */
    public ReviewEligibilityDto checkReviewWritable(Long productSeq, Long memberSeq) {

        if (memberSeq == null) {
            return new ReviewEligibilityDto(false, "로그인 후 리뷰를 작성할 수 있습니다.");
        }

        if (!reviewRepository.existsProduct(productSeq)) {
            return new ReviewEligibilityDto(false, "상품을 찾을 수 없습니다.");
        }

        /*
            이미 같은 상품에 리뷰를 작성한 회원인지 확인합니다.
        */
        if (reviewRepository.existsReviewByProductSeqAndMemberSeq(productSeq, memberSeq)) {
            return new ReviewEligibilityDto(false, "이미 이 상품에 리뷰를 작성했습니다.");
        }

        /*
            해당 상품을 구매 완료한 회원인지 확인합니다.

            리뷰 등록 버튼을 클릭하는 시점에는 아직 orderItemSeq를 입력받기 전이므로,
            productSeq와 memberSeq만으로 구매 완료 여부를 확인합니다.
        */
        if (!reviewRepository.existsPurchasedProduct(productSeq, memberSeq)) {
            return new ReviewEligibilityDto(false, "구매 완료한 상품만 리뷰를 작성할 수 있습니다.");
        }

        return new ReviewEligibilityDto(true, "리뷰를 작성할 수 있습니다.");
    }
	
	/*
	    리뷰 등록 + 리뷰 이미지 업로드
	
	    1. 리뷰 기본 정보를 review 테이블에 등록
	    2. 이미지가 있으면 Cloudinary 업로드
	    3. 업로드된 이미지 URL을 review_image 테이블에 저장
	    4. product 테이블의 평균 별점, 리뷰 수 갱신
	*/
	@Transactional
	public void createReviewWithImages(ReviewCreateRequestDto dto, List<MultipartFile> images) {
	
		/*
		    이미 같은 상품에 리뷰를 작성한 회원인지 확인합니다.
		*/
		if (reviewRepository.existsReviewByProductSeqAndMemberSeq(dto.getProductSeq(), dto.getMemberSeq())) {
		    throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 이 상품에 리뷰를 작성했습니다.");
		}
	
		/*
		    해당 상품을 실제로 구매한 회원인지 확인합니다.
		*/
		if (!reviewRepository.existsPurchasedOrderItem(
		        dto.getProductSeq(),
		        dto.getMemberSeq(),
		        dto.getOrderItemSeq())) {
	
		    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "구매 완료한 상품만 리뷰를 작성할 수 있습니다.");
		}
		
	    if (!reviewRepository.existsProduct(dto.getProductSeq())) {
	        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다.");
	    }
	
	    if (dto.getMemberSeq() == null) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "회원번호가 필요합니다.");
	    }
	
	    if (dto.getOrderItemSeq() == null) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "주문상품번호가 필요합니다.");
	    }
	
	    if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "별점은 1점부터 5점까지만 가능합니다.");
	    }
	
	    if (dto.getContent() == null || dto.getContent().isBlank()) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "리뷰 내용을 입력해주세요.");
	    }
	    
	    /*
		    리뷰 이미지 개수 검증
	
		    이미지는 최대 5개까지만 등록할 수 있습니다.
		*/
		validateReviewImageCount(images);
	    
	    /*
		    이미 같은 상품에 리뷰를 작성한 회원인지 확인합니다.
		*/
		if (reviewRepository.existsReviewByProductSeqAndMemberSeq(dto.getProductSeq(), dto.getMemberSeq())) {
		    throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 이 상품에 리뷰를 작성했습니다.");
		}
	
		/*
		    해당 회원이 실제로 이 상품을 구매했는지 확인합니다.
	
		    orderItemSeq가 현재 회원의 주문상품이고,
		    현재 상품의 주문상품이며,
		    배송완료 상태여야 리뷰를 작성할 수 있습니다.
		*/
		if (!reviewRepository.existsPurchasedOrderItem(
		        dto.getProductSeq(),
		        dto.getMemberSeq(),
		        dto.getOrderItemSeq())) {
	
		    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "구매 완료한 상품만 리뷰를 작성할 수 있습니다.");
		}
	
	    Long reviewSeq = reviewRepository.insertReview(dto);
	
	    if (images != null && !images.isEmpty()) {
	
	        int sortOrder = 1;
	
	        for (MultipartFile image : images) {
	
	            if (image == null || image.isEmpty()) {
	                continue;
	            }
	
	            String imageUrl = cloudinaryService.uploadReviewImage(image);
	
	            reviewRepository.insertReviewImage(reviewSeq, imageUrl, sortOrder);
	
	            sortOrder++;
	        }
	    }
	
	    productDetailRepository.updateProductReviewStats(dto.getProductSeq());
	}
	
	/*
	    리뷰 수정 + 이미지 재등록
	
	    1. 별점, 내용을 수정합니다.
	    2. 새 이미지가 있으면 기존 이미지를 숨김 처리합니다.
	    3. 새 이미지를 Cloudinary에 업로드합니다.
	    4. review_image 테이블에 새 이미지 URL을 저장합니다.
	    5. 상품 평균 별점과 리뷰 수를 다시 계산합니다.
	*/
	@Transactional
	public void updateReviewWithImages(
	        Long reviewSeq,
	        Long productSeq,
	        Long memberSeq,
	        Integer rating,
	        String content,
	        List<MultipartFile> images) {
	
	    if (memberSeq == null) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "회원번호가 필요합니다.");
	    }
	
	    if (rating == null || rating < 1 || rating > 5) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "별점은 1점부터 5점까지만 가능합니다.");
	    }
	
	    if (content == null || content.isBlank()) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "리뷰 내용을 입력해주세요.");
	    }
	    
	    /*
		    리뷰 이미지 개수 검증
	
		    이미지는 최대 5개까지만 등록할 수 있습니다.
		*/
		validateReviewImageCount(images);
	
	    int result = reviewRepository.updateReview(reviewSeq, productSeq, memberSeq, rating, content);
	
	    if (result == 0) {
	        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정할 수 없는 리뷰입니다.");
	    }
	
	    /*
	        새 이미지가 들어온 경우에만 기존 이미지를 숨김 처리하고
	        새 이미지를 다시 등록합니다.
	    */
	    if (images != null && !images.isEmpty()) {
	
	        reviewRepository.deleteReviewImages(reviewSeq);
	
	        int imageOrder = 1;
	
	        for (MultipartFile image : images) {
	
	            if (image == null || image.isEmpty()) {
	                continue;
	            }
	
	            String imageUrl = cloudinaryService.uploadReviewImage(image);
	
	            reviewRepository.insertReviewImage(reviewSeq, imageUrl, imageOrder);
	
	            imageOrder++;
	        }
	    }
	
	    productDetailRepository.updateProductReviewStats(productSeq);
	}
	
	/*
	    리뷰 작성 가능한 주문상품 목록 조회
	
	    리뷰 등록 폼에서 주문상품번호를 직접 입력하지 않고,
	    상품명과 옵션명을 선택할 수 있도록 목록을 내려줍니다.
	*/
	public List<PurchasedOrderItemDto> getPurchasedOrderItems(Long productSeq, Long memberSeq) {
	
	    if (memberSeq == null) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "회원번호가 필요합니다.");
	    }
	
	    if (!reviewRepository.existsProduct(productSeq)) {
	        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다.");
	    }
	
	    return reviewRepository.findPurchasedOrderItems(productSeq, memberSeq);
	}
	
	/*
	    리뷰 삭제
	
	    본인이 작성한 리뷰만 삭제할 수 있습니다.
	
	    삭제 후 상품 평균 별점과 리뷰 수를 다시 계산합니다.
	*/
	@Transactional
	public void deleteReview(Long reviewSeq, Long productSeq, Long memberSeq) {
	
	    if (memberSeq == null) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "회원번호가 필요합니다.");
	    }
	
	    int result = reviewRepository.deleteReview(reviewSeq, productSeq, memberSeq);
	
	    if (result == 0) {
	        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제할 수 없는 리뷰입니다.");
	    }
	
	    productDetailRepository.updateProductReviewStats(productSeq);
	}
	
	private final CloudinaryService cloudinaryService;

    // 리뷰 DB 작업을 담당하는 Repository
    private final ReviewRepository reviewRepository;

    // 상품 DB 작업을 담당하는 Repository
    // 리뷰 등록, 수정, 삭제 후 상품의 평균 별점과 리뷰 수를 갱신할 때 사용
    private final ProductDetailRepository productDetailRepository;

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
        productDetailRepository.updateProductReviewStats(productSeq);
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
        productDetailRepository.updateProductReviewStats(productSeq);
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