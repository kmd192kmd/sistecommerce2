package com.example.java.product.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.product.dto.ProductDetailDto;
import com.example.java.product.repository.ProductDetailRepository;
import com.example.java.product.dto.ProductPageResponseDto;
import java.util.List;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductDetailService {

    private final ProductDetailRepository productRepository;


    /*
        상품 상세 조회

        1. 조회수 증가
        2. 상품 기본 정보 조회
        3. 상품 이미지 목록 조회
        4. 상품 옵션 목록 조회
        5. 로그인 회원이 있으면 찜 여부 확인
    */
    @Transactional
    public ProductDetailDto getProductDetail(Long productSeq, Long memberSeq) {

        /*
            상세 페이지에 들어왔으므로 조회수를 1 증가시킵니다.
        */
        productRepository.increaseViewCount(productSeq);

        /*
            상품 기본 정보를 조회합니다.

            상품이 없으면 예외를 발생시킵니다.
            예외가 발생하면 Controller까지 전달되고,
            기본적으로 오류 페이지가 출력될 수 있습니다.
        */
        ProductDetailDto product = productRepository.findProductDetail(productSeq)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        /*
            상품 이미지 목록을 조회해서 DTO에 넣습니다.
        */
        product.setImageList(productRepository.findProductImages(productSeq));

        /*
            상품 옵션 목록을 조회해서 DTO에 넣습니다.
        */
        product.setOptionList(productRepository.findProductOptions(productSeq));

        /*
            로그인한 회원인 경우에만 찜 여부를 확인합니다.

            비회원이면 memberSeq가 null이므로 찜 여부 확인을 하지 않습니다.
        */
        if (memberSeq != null) {
            product.setWished(productRepository.existsWish(productSeq, memberSeq));
        }

        return product;
    }
    
    /*
	    상품 목록 페이징 조회
	
	    page와 size를 받아서 상품 목록을 조회합니다.
	
	    page는 1부터 시작합니다.
	    size는 한 페이지에 보여줄 상품 개수입니다.
	*/
	public ProductPageResponseDto getProductsByPaging(int page, int size) {
	
	    /*
	        page 값이 1보다 작으면 기본값 1로 보정합니다.
	    */
	    if (page < 1) {
	        page = 1;
	    }
	
	    /*
	        size 값이 1보다 작으면 기본값 10으로 보정합니다.
	    */
	    if (size < 1) {
	        size = 10;
	    }
	
	    /*
	        너무 많은 데이터를 한 번에 가져오지 않도록 최대 50개로 제한합니다.
	    */
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
	
	    /*
	        전체 상품 개수 조회
	    */
	    int totalCount = productRepository.countProducts();
	
	    /*
	        현재 페이지의 상품 목록 조회
	    */
	    List<ProductDetailDto> products = productRepository.findProductsByPaging(offset, size);
	
	    /*
	        전체 페이지 수 계산
	    */
	    int totalPages = (int) Math.ceil((double) totalCount / size);
	
	    /*
	        상품 목록 페이징 응답 DTO 생성
	    */
	    return ProductPageResponseDto.builder()
	            .page(page)
	            .size(size)
	            .totalCount(totalCount)
	            .totalPages(totalPages)
	            .products(products)
	            .build();
	}
    
    /*
	    상품 삭제
	
	    Repository에서 상품 상태를 DELETED로 변경합니다.
	
	    삭제 대상 상품이 없으면 false를 반환하고,
	    삭제가 정상 처리되면 true를 반환합니다.
	*/
	public boolean deleteProduct(Long productSeq) {
	
	    /*
	        deleteProduct()
	        - 영향을 받은 행의 개수를 반환합니다.
	        - 1이면 삭제 성공
	        - 0이면 삭제할 상품 없음
	    */
	    int result = productRepository.deleteProduct(productSeq);
	
	    return result > 0;
	}
}