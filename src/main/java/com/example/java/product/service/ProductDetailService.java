package com.example.java.product.service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.product.dto.CategoryDto;
import com.example.java.product.dto.ProductDto;
import com.example.java.product.entity.Product;
import com.example.java.product.repository.ProductDetailRepository;

import lombok.RequiredArgsConstructor;

/*
    ProductDetailService

    상품 등록, 상세 조회, 수정, 삭제(CRUD) 등
    상세 화면과 상품 관리 기능을 담당합니다.
*/
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductDetailService {

    /*
        ProductDetailRepository

        상품 상세, 등록, 수정, 삭제에 필요한 DB 작업을 담당합니다.

        Controller는 DB에 직접 접근하지 않고,
        Service를 통해 상품 관련 기능을 실행합니다.

        흐름:
        Controller → Service → Repository → DB
    */
    private final ProductDetailRepository productDetailRepository;
    private final StringRedisTemplate redisTemplate;

    /*
        CategoryService

        상품 등록 화면에서
        대분류 / 중분류 / 소분류 카테고리 목록을 가져오기 위해 사용합니다.
    */
    private final CategoryService categoryService;
    
    /*
	    ProductWishService
	
	    상품 상세 화면에서
	    현재 로그인한 회원이 해당 상품을 찜했는지 확인하기 위해 사용합니다.
	*/
	private final ProductWishService productWishService;

    /*
        1. 상품 등록

        ProductDto를 Product 엔티티로 변환한 뒤 DB에 저장합니다.
    */
    @Transactional
    public ProductDto createProduct(ProductDto dto) {

        Product product = dto.toEntity();

        Product saved = productDetailRepository.save(product);

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
        1. 상품 리뷰 통계 갱신
        2. 상품 엔티티 조회
        3. 접근 불가 상품 검증
        4. 조회수 증가
        5. DTO 변환
        6. 이미지 목록 조회
        7. 옵션 목록 조회
        8. 로그인 회원이 있으면 찜 여부 확인
    */
    @Transactional
    public ProductDto getProductDetail(Long seq, Long memberSeq) {

        // 1. 상품 리뷰 통계 갱신 (기존 유지)
        productDetailRepository.updateProductReviewStats(seq);

        Product product = productDetailRepository.findById(seq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. 번호: " + seq));

        /*
            💡 2. Redis를 이용한 조회수 중복 방지 및 카운트 증가 로직
        */
        String userKey = (memberSeq != null) ? memberSeq.toString() : "GUEST"; // 비로그인은 IP 등으로 대체 가능
        String viewSetKey = "view:product:" + seq;       // 이 상품을 본 유저 Set
        String countKey = "product:view:count:" + seq;   // 이 상품의 실제 조회수 카운트

        // Redis Set에 유저 키 추가 시도 (기존에 없었으면 1 반환 -> 최초 조회)
        Long addedCount = redisTemplate.opsForSet().add(viewSetKey, userKey);
        
        if (addedCount != null && addedCount > 0) {
            // 처음 조회하는 유저라면 Redis 카운트 1 증가
            redisTemplate.opsForValue().increment(countKey);
            // Set의 만료시간 설정 (예: 24시간 후 소멸하여 다음날 재집계 가능하게 함)
            redisTemplate.expire(viewSetKey, 24, TimeUnit.HOURS);
        }

        // ❌ 기존 DB의 viewCount를 직접 올리고 save하던 코드 삭제!
        // Long currentViewCount = product.getViewCount() == null ? 0L : product.getViewCount();
        // product.setViewCount(currentViewCount + 1);
        // Product updated = productDetailRepository.save(product);

        /*
            💡 화면에 보여줄 조회수 계산
            DB에 있던 기본 조회수 + 아직 DB에 반영 안 된 Redis의 증가분을 합쳐서 Dto에 세팅합니다.
         */
        String redisCount = redisTemplate.opsForValue().get(countKey);
        long additionalViews = (redisCount != null) ? Long.parseLong(redisCount) : 0L;
        long totalViewCount = (product.getViewCount() == null ? 0L : product.getViewCount()) + additionalViews;

        ProductDto dto = convertToDtoWithDetails(product);
        dto.setViewCount(totalViewCount); // Dto에 최종 합산 조회수 바인딩

        // 3. 찜 여부 확인 (기존 유지)
        if (memberSeq != null) {
            dto.setWished(productWishService.isWished(memberSeq, seq));
        } else {
            dto.setWished(false);
        }

        return dto;
    }

    /*
        조회수 증가 없이 상품 조회

        수정 화면, 최근 조회 상품, 추천 상품 등
        조회수를 올리지 않고 상품 정보만 필요할 때 사용합니다.
    */
    public ProductDto getProductWithoutViewCount(Long seq) {

        Product product = productDetailRepository.findById(seq)
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

        Product product = productDetailRepository.findById(seq)
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

        Product updated = productDetailRepository.save(product);

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

        int result = productDetailRepository.deleteProduct(seq);

        return result > 0;
    }

    /*
        상품 등록 화면용 전체 카테고리 목록 조회

        category 테이블 구조:
        - seq
        - category_name
        - depth_level
        - parent_seq

        화면에서는 이 목록을 이용해서
        대분류 → 중분류 → 소분류 순서로 선택합니다.
    */
    public List<CategoryDto> getAllCategories() {

        return categoryService.getAllCategories();
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
        List<ProductDto.ProductImageDto> imageList =
                productDetailRepository.findProductImages(product.getSeq());

        dto.setImageList(imageList);

        /*
            대표 이미지 찾기
        */
        String thumbnailUrl = imageList.stream()
                .filter(img -> "Y".equals(img.getThumbnailYn()))
                .map(ProductDto.ProductImageDto::getImageUrl)
                .findFirst()
                .orElse("https://images.unsplash.com/photo-1546069901-ba9599a7e63c?q=80&w=400&auto=format&fit=crop");

        dto.setImage(thumbnailUrl);
        dto.setThumbnailUrl(thumbnailUrl);

        // 핫딜 정보 조회
        List<ProductDetailRepository.HotDealInfoDto> hotDeals =
                productDetailRepository.findProductHotDeals(product.getSeq());

        boolean hasHotDeal = !hotDeals.isEmpty();
        dto.setHotDeal(hasHotDeal);
        if (hasHotDeal) {
            dto.setOriginalPrice(product.getPrice());
            ProductDetailRepository.HotDealInfoDto firstHotDeal = hotDeals.get(0);
            if (firstHotDeal.getDiscountRate() != null) {
                dto.setDiscountRate(firstHotDeal.getDiscountRate());
                dto.setPrice(product.getPrice() * (100 - firstHotDeal.getDiscountRate()) / 100);
            } else if (firstHotDeal.getDiscountPrice() != null) {
                int discountPrice = firstHotDeal.getDiscountPrice();
                dto.setPrice(Math.max(0, product.getPrice() - discountPrice));
                int calculatedRate = (int) Math.round(((double) discountPrice / product.getPrice()) * 100);
                dto.setDiscountRate(calculatedRate);
            }
        }

        /*
            상품 옵션 목록 조회
        */
        List<ProductDto.ProductOptionDto> optionList =
                productDetailRepository.findProductOptions(product.getSeq());

        if (hasHotDeal) {
            java.util.Map<Long, ProductDetailRepository.HotDealInfoDto> hotDealMap = hotDeals.stream()
                    .collect(Collectors.toMap(ProductDetailRepository.HotDealInfoDto::getOptionsSeq, h -> h, (h1, h2) -> h1));

            for (ProductDto.ProductOptionDto optDto : optionList) {
                ProductDetailRepository.HotDealInfoDto hd = hotDealMap.get(optDto.getSeq());
                if (hd != null) {
                    if (optDto.getStock() == null || optDto.getStock() <= 0) {
                        String baseOptName = makeOptionNameWithoutPrice(optDto);
                        optDto.setOptionName(baseOptName + " (품절)");
                    } else {
                        int originalOptPrice = product.getPrice() + (optDto.getAdditionalPrice() != null ? optDto.getAdditionalPrice() : 0);
                        int discountedOptPrice = originalOptPrice;
                        if (hd.getDiscountRate() != null) {
                            discountedOptPrice = originalOptPrice * (100 - hd.getDiscountRate()) / 100;
                        } else if (hd.getDiscountPrice() != null) {
                            discountedOptPrice = Math.max(0, originalOptPrice - hd.getDiscountPrice());
                        }
                        String baseOptName = makeOptionNameWithoutPrice(optDto);
                        optDto.setOptionName(baseOptName + " (핫딜가: " + String.format("%,d", discountedOptPrice) + "원)");
                    }
                }
            }
        }

        dto.setOptionList(optionList);

        /*
            옵션명을 문자열 목록으로 변환합니다.
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

    private String makeOptionNameWithoutPrice(ProductDto.ProductOptionDto dto) {
        StringBuilder sb = new StringBuilder();
        appendOptionStr(sb, dto.getColor());
        appendOptionStr(sb, dto.getOptionsSize());
        appendOptionStr(sb, dto.getVolumeWeight());
        appendOptionStr(sb, dto.getTaste());
        appendOptionStr(sb, dto.getStorageType());
        appendOptionStr(sb, dto.getScentIngredient());
        appendOptionStr(sb, dto.getVoltage());
        appendOptionStr(sb, dto.getQuantitySet());
        appendOptionStr(sb, dto.getSizeSpec());
        appendOptionStr(sb, dto.getStorageCapacity());
        appendOptionStr(sb, dto.getMemory());
        appendOptionStr(sb, dto.getSwitchAxis());
        appendOptionStr(sb, dto.getConnectionType());
        appendOptionStr(sb, dto.getWearableSpec());
        appendOptionStr(sb, dto.getMaterialType());
        appendOptionStr(sb, dto.getOptionsType());

        if (sb.length() == 0) {
            sb.append("기본 옵션");
        }
        return sb.toString();
    }

    private void appendOptionStr(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(" / ");
        }
        sb.append(value);
    }
}