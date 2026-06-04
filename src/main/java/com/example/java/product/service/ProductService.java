package com.example.java.product.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.product.dto.ProductDto;
import com.example.java.product.dto.ProductPageResponseDto;
import com.example.java.product.entity.Product;
import com.example.java.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    /*
        ProductRepository

        상품 DB 작업을 담당합니다.

        현재 ProductRepository는 JpaRepository interface가 아니라
        직접 만든 class입니다.

        내부에서 EntityManager와 NamedParameterJdbcTemplate을 사용합니다.
    */
    private final ProductRepository productRepository;

    /*
        CategoryService

        상품 목록 조회 시
        선택한 카테고리의 하위 카테고리 번호 목록을 가져오기 위해 사용합니다.
    */
    private final CategoryService categoryService;


    /*
        1. 상품 등록

        ProductDto를 Product 엔티티로 변환한 뒤 DB에 저장합니다.
    */
    @Transactional
    public ProductDto createProduct(ProductDto dto) {

        Product product = dto.toEntity();

        Product saved = productRepository.save(product);

        return convertToDtoWithDetails(saved);
    }


    /*
        2. 상품 상세 조회

        로그인 회원 정보가 필요 없는 경우 사용합니다.
    */
    @Transactional
    public ProductDto getProductDetail(Long seq) {

        return getProductDetail(seq, null);
    }


    /*
        2-1. 상품 상세 조회

        처리 순서:
        1. 상품 엔티티 조회
        2. 접근 불가 상품 검증
        3. 조회수 증가
        4. DTO 변환
        5. 이미지 목록 조회
        6. 옵션 목록 조회
        7. 로그인 회원이 있으면 찜 여부 확인
    */
    @Transactional
    public ProductDto getProductDetail(Long seq, Long memberSeq) {

        /*
            상품 리뷰 통계 갱신

            review 테이블 기준으로 평균 별점과 리뷰 수를 다시 계산해서
            product 테이블의 avg_rating, review_count에 저장합니다.

            상품 상세 화면에서 별점과 리뷰 수가 최신 상태로 보이도록
            상품 엔티티를 조회하기 전에 먼저 갱신합니다.
        */
        productRepository.updateProductReviewStats(seq);

        Product product = productRepository.findById(seq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. 번호: " + seq));

        /*
            접근 불가 상품 검증

            STOPPED : 판매 중지
            hideYn = Y : 숨김 상품
            DELETED : 삭제 상품
        */
        if ("STOPPED".equals(product.getSaleStatus())
                || "Y".equals(product.getHideYn())
                || "DELETED".equals(product.getStatus())) {

            throw new IllegalStateException("해당 상품은 접근이 불가능합니다.");
        }

        /*
            상세 페이지에 들어왔으므로 조회수를 1 증가시킵니다.
        */
        Long currentViewCount = product.getViewCount() == null ? 0L : product.getViewCount();
        product.setViewCount(currentViewCount + 1);

        Product updated = productRepository.save(product);

        /*
            Product 엔티티를 화면용 ProductDto로 변환합니다.
        */
        ProductDto dto = convertToDtoWithDetails(updated);

        /*
            로그인한 회원인 경우에만 찜 여부를 확인합니다.
        */
        if (memberSeq != null) {
            dto.setWished(productRepository.existsWish(seq, memberSeq));
        }

        return dto;
    }


    /*
        조회수 증가 없이 상품 조회

        수정 화면, 최근 조회 상품, 추천 상품 등
        조회수를 올리지 않고 상품 정보만 필요할 때 사용합니다.
    */
    public ProductDto getProductWithoutViewCount(Long seq) {

        Product product = productRepository.findById(seq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. 번호: " + seq));

        return convertToDtoWithDetails(product);
    }


    /*
        3. 상품 수정

        상품 번호로 기존 상품을 찾은 뒤
        전달받은 DTO 값으로 수정합니다.
    */
    @Transactional
    public ProductDto updateProduct(Long seq, ProductDto dto) {

        Product product = productRepository.findById(seq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. 번호: " + seq));

        product.setProductName(dto.getProductName());
        product.setPrice(dto.getPrice());
        product.setContent(dto.getContent());

        if (dto.getSaleStatus() != null) {
            product.setSaleStatus(dto.getSaleStatus());
        }

        if (dto.getApprovalStatus() != null) {
            product.setApprovalStatus(dto.getApprovalStatus());
        }

        if (dto.getHideYn() != null) {
            product.setHideYn(dto.getHideYn());
        }

        if (dto.getProductStatus() != null) {
            product.setStatus(dto.getProductStatus());
        }

        Product updated = productRepository.save(product);

        return convertToDtoWithDetails(updated);
    }


    /*
        4. 상품 논리 삭제

        실제 DELETE가 아니라
        Repository에서 status = 'DELETED', hide_yn = 'Y'로 변경합니다.

        반환값:
        true  : 삭제 성공
        false : 삭제할 상품 없음
    */
    @Transactional
    public boolean deleteProduct(Long seq) {

        int result = productRepository.deleteProduct(seq);

        return result > 0;
    }


    /*
        상품 목록 페이징 API 조회

        page는 1부터 시작합니다.
        size는 한 페이지에 보여줄 상품 개수입니다.
    */
    public ProductPageResponseDto getProductsByPaging(int page, int size) {

        if (page < 1) {
            page = 1;
        }

        if (size < 1) {
            size = 10;
        }

        if (size > 50) {
            size = 50;
        }

        /*
            Oracle 페이징에 사용할 offset 계산

            예:
            page = 1, size = 10 → offset = 0
            page = 2, size = 10 → offset = 10
            page = 3, size = 10 → offset = 20
        */
        int offset = (page - 1) * size;

        int totalCount = productRepository.countProducts();

        List<ProductDto> products = productRepository.findProductsByPaging(offset, size);

        int totalPages = (int) Math.ceil((double) totalCount / size);

        return ProductPageResponseDto.builder()
                .page(page)
                .size(size)
                .totalCount(totalCount)
                .totalPages(totalPages)
                .products(products)
                .build();
    }


    /*
        5. 상품 목록 조회 및 검색
    */
    public Page<ProductDto> getProductList(
            Long categorySeq,
            String keyword,
            String sortBy,
            int page) {

        return getProductList(categorySeq, keyword, sortBy, page, null, null, null, false);
    }


    /*
        5-1. 필터링 조건이 추가된 상품 목록 조회

        최소 가격, 최대 가격, 최소 평점 조건을 받을 수 있습니다.
    */
    public Page<ProductDto> getProductList(
            Long categorySeq,
            String keyword,
            String sortBy,
            int page,
            Integer minPrice,
            Integer maxPrice,
            Double minRating) {

        return getProductList(categorySeq, keyword, sortBy, page, minPrice, maxPrice, minRating, false);
    }


    /*
        5-2. 최종 상품 목록 조회

        기능:
        - 카테고리 필터
        - 검색어
        - 정렬
        - 가격 필터
        - 평점 필터
        - 품절 상품 숨김 여부
    */
    public Page<ProductDto> getProductList(
            Long categorySeq,
            String keyword,
            String sortBy,
            int page,
            Integer minPrice,
            Integer maxPrice,
            Double minRating,
            boolean hideOutOfStock) {

        Pageable pageable = PageRequest.of(page, 20, getSortOption(sortBy));

        Integer finalMinPrice = minPrice != null ? minPrice : 0;
        Integer finalMaxPrice = maxPrice != null ? maxPrice : 999999999;
        Double finalMinRating = minRating != null ? minRating : 0.0;
        String finalKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword : null;
        String saleStatus = hideOutOfStock ? "ON_SALE" : null;

        List<Long> categorySeqs = categoryService.getDescendantCategorySeqs(categorySeq);

        Page<Product> productPage = productRepository.findWithFilters(
                categorySeqs,
                finalKeyword,
                finalMinPrice,
                finalMaxPrice,
                finalMinRating,
                saleStatus,
                pageable
        );

        return productPage.map(this::convertToDtoWithDetails);
    }


    /*
        Product 엔티티를 ProductDto로 변환하면서
        화면에 필요한 추가 정보도 함께 넣습니다.

        포함 정보:
        - 상품 기본 정보
        - 대표 이미지
        - 전체 이미지 목록
        - 옵션 목록
        - 옵션 문자열 목록
    */
    private ProductDto convertToDtoWithDetails(Product product) {

        ProductDto dto = product.toDto();

        /*
            상품 이미지 목록 조회
        */
        List<ProductDto.ProductImageDto> imageList = productRepository.findProductImages(product.getSeq());

        dto.setImageList(imageList);

        /*
            대표 이미지 찾기

            thumbnail_yn = 'Y'인 이미지를 우선 사용합니다.
            없으면 기본 이미지를 사용합니다.
        */
        String thumbnailUrl = imageList.stream()
                .filter(img -> "Y".equals(img.getThumbnailYn()))
                .map(ProductDto.ProductImageDto::getImageUrl)
                .findFirst()
                .orElse("https://images.unsplash.com/photo-1546069901-ba9599a7e63c?q=80&w=400&auto=format&fit=crop");

        dto.setImage(thumbnailUrl);
        dto.setThumbnailUrl(thumbnailUrl);

        /*
            상품 옵션 목록 조회
        */
        List<ProductDto.ProductOptionDto> optionList = productRepository.findProductOptions(product.getSeq());

        dto.setOptionList(optionList);

        /*
            옵션명을 문자열 목록으로 변환합니다.

            예:
            optionList의 optionName만 뽑아서
            ["블랙 / M", "화이트 / L"] 형태로 만듭니다.
        */
        List<String> optionStrings = optionList.stream()
                .map(ProductDto.ProductOptionDto::getOptionName)
                .filter(str -> str != null && !str.isBlank())
                .collect(Collectors.toList());

        if (optionStrings.isEmpty()) {
            optionStrings.add("기본 옵션");
        }

        dto.setOptions(optionStrings);

        return dto;
    }


    /*
        정렬 옵션 매핑

        sortBy 값에 따라 정렬 기준을 다르게 적용합니다.
    */
    private Sort getSortOption(String sortBy) {

        if (sortBy == null) {
            return Sort.by(Sort.Direction.DESC, "salesCount");
        }

        return switch (sortBy.toLowerCase()) {
            case "price_asc", "priceasc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc", "pricedesc" -> Sort.by(Sort.Direction.DESC, "price");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdDate");
            case "rating", "ratingdesc" -> Sort.by(Sort.Direction.DESC, "avgRating");
            default -> Sort.by(Sort.Direction.DESC, "salesCount");
        };
    }
}