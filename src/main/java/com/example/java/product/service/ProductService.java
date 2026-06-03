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
import com.example.java.product.entity.Product;
import com.example.java.product.repository.OptionsRepository;
import com.example.java.product.repository.ProductImageRepository;
import com.example.java.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final OptionsRepository optionsRepository;
    private final CategoryService categoryService;

    // 1. 상품 등록
    @Transactional
    public ProductDto createProduct(ProductDto dto) {
        Product product = dto.toEntity();
        Product saved = productRepository.save(product);
        return convertToDtoWithDetails(saved);
    }

    // 2. 상품 상세 조회 (상세 조회 시 조회수 증가 적용)
    @Transactional
    public ProductDto getProductDetail(Long seq) {
        Product product = productRepository.findById(seq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. 번호: " + seq));

        // 단종(STOPPED)되었거나 숨김(hide_yn='Y') 처리되었거나 삭제된(DELETED) 상품은 일반 사용자가 접근할 수 없음
        if ("STOPPED".equals(product.getSaleStatus()) || "Y".equals(product.getHideYn()) || "DELETED".equals(product.getStatus())) {
            throw new IllegalStateException("해당 상품은 접근이 불가능합니다.");
        }

        // 조회수 추가 및 저장
        product.setViewCount(product.getViewCount() + 1);
        Product updated = productRepository.save(product);
        return convertToDtoWithDetails(updated);
    }

    // 조회수 증가 없이 상품 조회 (최근 조회 상품 등 서브 영역 표시용)
    public ProductDto getProductWithoutViewCount(Long seq) {
        Product product = productRepository.findById(seq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. 번호: " + seq));
        return convertToDtoWithDetails(product);
    }

    // 3. 상품 수정
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
        if (dto.getStatus() != null) {
            product.setStatus(dto.getStatus());
        }

        Product updated = productRepository.save(product);
        return convertToDtoWithDetails(updated);
    }

    // 4. 상품 논리 삭제 (상태를 DELETED로 변경)
    @Transactional
    public void deleteProduct(Long seq) {
        Product product = productRepository.findById(seq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. 번호: " + seq));
        product.setStatus("DELETED");
        productRepository.save(product);
    }

    // 5. 상품 목록 조회 및 검색 (기본 매개변수 위임)
    public Page<ProductDto> getProductList(Long categorySeq, String keyword, String sortBy, int page) {
        return getProductList(categorySeq, keyword, sortBy, page, null, null, null, false);
    }

    // 5-1. 필터링 조건(최소/최대 가격, 평점)이 추가된 상품 목록 조회 및 검색
    public Page<ProductDto> getProductList(Long categorySeq, String keyword, String sortBy, int page, Integer minPrice, Integer maxPrice, Double minRating) {
        return getProductList(categorySeq, keyword, sortBy, page, minPrice, maxPrice, minRating, false);
    }

    // 5-2. 품절 상품 숨기기 필터가 추가된 최종 상품 목록 조회 및 검색
    public Page<ProductDto> getProductList(Long categorySeq, String keyword, String sortBy, int page, Integer minPrice, Integer maxPrice, Double minRating, boolean hideOutOfStock) {
        // 기본 20개 페이징
        Pageable pageable = PageRequest.of(page, 20, getSortOption(sortBy));
        
        Integer finalMinPrice = minPrice != null ? minPrice : 0;
        Integer finalMaxPrice = maxPrice != null ? maxPrice : 999999999;
        Double finalMinRating = minRating != null ? minRating : 0.0;
        String finalKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword : null;
        String saleStatus = hideOutOfStock ? "ON_SALE" : null;

        List<Long> categorySeqs = categoryService.getDescendantCategorySeqs(categorySeq);

        Page<Product> productPage = productRepository.findWithFilters(
                categorySeqs, finalKeyword, finalMinPrice, finalMaxPrice, finalMinRating, saleStatus, pageable);

        return productPage.map(this::convertToDtoWithDetails);
    }

    // 6. DB 상품 정보를 DTO로 변환 시 썸네일 이미지 및 옵션 정보 목록을 함께 로드하여 세팅
    private ProductDto convertToDtoWithDetails(Product product) {
        ProductDto dto = product.toDto();

        // 썸네일 이미지 매핑 (기본값 설정)
        productImageRepository.findFirstByProductSeqAndThumbnailYnAndStatus(product.getSeq(), "Y", "NORMAL")
            .ifPresentOrElse(
                img -> dto.setImage(img.getImageUrl()),
                () -> dto.setImage("https://images.unsplash.com/photo-1546069901-ba9599a7e63c?q=80&w=400&auto=format&fit=crop") // 기본 이미지 예시
            );

        // 옵션 목록 조회 후 문자열 포맷팅 처리
        List<String> optionStrings = optionsRepository.findByProductSeq(product.getSeq()).stream()
            .map(opt -> {
                StringBuilder sb = new StringBuilder();
                if (opt.getColor() != null && !opt.getColor().trim().isEmpty()) {
                    sb.append(opt.getColor());
                }
                if (opt.getOptionsSize() != null && !opt.getOptionsSize().trim().isEmpty()) {
                    if (sb.length() > 0) sb.append(" / ");
                    sb.append(opt.getOptionsSize());
                }
                if (opt.getVolumeWeight() != null && !opt.getVolumeWeight().trim().isEmpty()) {
                    if (sb.length() > 0) sb.append(" / ");
                    sb.append(opt.getVolumeWeight());
                }
                if (opt.getAdditionalPrice() != null && opt.getAdditionalPrice() > 0) {
                    sb.append(" (+").append(opt.getAdditionalPrice()).append("원)");
                }
                return sb.toString();
            })
            .filter(str -> !str.isEmpty())
            .collect(Collectors.toList());

        if (optionStrings.isEmpty()) {
            optionStrings.add("기본 옵션");
        }
        dto.setOptions(optionStrings);

        return dto;
    }

    // 정렬 옵션 매핑 (판매량순, 가격순, 최신순, 별점순)
    private Sort getSortOption(String sortBy) {
        if (sortBy == null) {
            return Sort.by(Sort.Direction.DESC, "salesCount"); // 기본: 판매량순
        }
        return switch (sortBy.toLowerCase()) {
            case "price_asc", "priceasc" -> Sort.by(Sort.Direction.ASC, "price"); // 낮은가격순
            case "price_desc", "pricedesc" -> Sort.by(Sort.Direction.DESC, "price"); // 높은가격순
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdDate"); // 최신순
            case "rating", "ratingdesc" -> Sort.by(Sort.Direction.DESC, "avgRating"); // 별점순
            default -> Sort.by(Sort.Direction.DESC, "salesCount"); // 기본: 판매량순
        };
    }
}
