package com.example.java.product.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.product.dto.ProductDto;
import com.example.java.product.entity.Product;
import com.example.java.product.repository.ProductDetailRepository;

import lombok.RequiredArgsConstructor;

/*
    ProductDetailService

    상품 등록, 상세 조회, 수정, 삭제(CRUD) 등 상세 화면과 상품 관리 기능을 담당합니다.
*/
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductDetailService {

    private final ProductDetailRepository productDetailRepository;

    /*
        상품 등록
    */
    @Transactional
    public ProductDto createProduct(ProductDto dto) {

        Product product = dto.toEntity();
        Product saved = productDetailRepository.save(product);

        return convertToDtoWithDetails(saved);
    }

    /*
        상품 상세 조회 (기본)
    */
    @Transactional
    public ProductDto getProductDetail(Long seq) {

        return getProductDetail(seq, null);
    }

    /*
        상품 상세 조회 (회원 찜 여부 및 조회수 증가 포함)
    */
    @Transactional
    public ProductDto getProductDetail(Long seq, Long memberSeq) {

        productDetailRepository.updateProductReviewStats(seq);

        Product product = productDetailRepository.findById(seq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. 번호: " + seq));

        if ("STOPPED".equals(product.getSaleStatus())
                || "Y".equals(product.getHideYn())
                || "DELETED".equals(product.getStatus())) {

            throw new IllegalStateException("해당 상품은 접근이 불가능합니다.");
        }

        Long currentViewCount = product.getViewCount() == null ? 0L : product.getViewCount();
        product.setViewCount(currentViewCount + 1);

        Product updated = productDetailRepository.save(product);
        ProductDto dto = convertToDtoWithDetails(updated);

        if (memberSeq != null) {
            dto.setWished(productDetailRepository.existsWish(seq, memberSeq));
        }

        return dto;
    }

    /*
        조회수 증가 없는 상품 조회
    */
    public ProductDto getProductWithoutViewCount(Long seq) {

        Product product = productDetailRepository.findById(seq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. 번호: " + seq));

        return convertToDtoWithDetails(product);
    }

    /*
        상품 수정
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
        상품 삭제
    */
    @Transactional
    public boolean deleteProduct(Long seq) {

        int result = productDetailRepository.deleteProduct(seq);

        return result > 0;
    }

    private ProductDto convertToDtoWithDetails(Product product) {

        ProductDto dto = product.toDto();

        List<ProductDto.ProductImageDto> imageList = productDetailRepository.findProductImages(product.getSeq());
        dto.setImageList(imageList);

        String thumbnailUrl = imageList.stream()
                .filter(img -> "Y".equals(img.getThumbnailYn()))
                .map(ProductDto.ProductImageDto::getImageUrl)
                .findFirst()
                .orElse("/src/images/product/default.png");

        dto.setImage(thumbnailUrl);
        dto.setThumbnailUrl(thumbnailUrl);

        List<ProductDto.ProductOptionDto> optionList = productDetailRepository.findProductOptions(product.getSeq());
        dto.setOptionList(optionList);

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
}
