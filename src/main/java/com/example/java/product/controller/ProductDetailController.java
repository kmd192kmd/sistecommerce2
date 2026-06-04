package com.example.java.product.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.java.product.dto.ProductDetailDto;
import com.example.java.product.dto.ProductPageResponseDto;
import com.example.java.product.service.ProductDetailService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ProductDetailController {

    private final ProductDetailService productService;
    
    /*
        상품 목록 페이징 조회
    
        page와 size를 기준으로 상품 목록을 조회합니다.
    
        예:
        GET /api/products
        GET /api/products?page=1&size=10
    */
    @GetMapping("/api/products")
    public ResponseEntity<ProductPageResponseDto> getProducts(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
    
        /*
            상품 목록 페이징 조회
        */
        ProductPageResponseDto result = productService.getProductsByPaging(page, size);
    
        /*
            조회 결과를 JSON으로 응답
        */
        return ResponseEntity.ok(result);
    }


    /*
        상품 상세 화면 요청 1

        접속 주소 예:
        /product/detail?seq=1

        RequestParam 방식입니다.
        주소의 ?seq=1 값을 productSeq로 받습니다.
    */
    @GetMapping("/product/detail")
    public String detail(@RequestParam("seq") Long productSeq,
                         HttpSession session,
                         Model model) {

        /*
            세션에서 로그인 회원번호를 가져옵니다.

            로그인하지 않은 경우 null이 됩니다.
        */
        Long memberSeq = getLoginMemberSeq(session);

        /*
            Service를 통해 상품 상세 정보를 조회합니다.
        */
        ProductDetailDto product = productService.getProductDetail(productSeq, memberSeq);

        /*
            조회한 상품 정보를 화면으로 넘깁니다.

            detail.html에서는 ${product}로 이 데이터를 사용할 수 있습니다.
        */
        model.addAttribute("product", product);

        /*
            templates.product/detail.html 파일을 화면으로 보여줍니다.

            네 폴더 구조가 templates.product 이므로
            return 값은 "product/detail" 입니다.
        */
        return "product/detail";
    }


    /*
        상품 상세 화면 요청 2

        접속 주소 예:
        /product/detail/1

        PathVariable 방식입니다.
        주소 경로에 있는 1을 productSeq로 받습니다.
    */
    @GetMapping("/product/detail/{seq}")
    public String detailPath(@PathVariable("seq") Long productSeq,
                             HttpSession session,
                             Model model) {

        Long memberSeq = getLoginMemberSeq(session);

        ProductDetailDto product = productService.getProductDetail(productSeq, memberSeq);

        model.addAttribute("product", product);

        return "product/detail";
    }


    /*
        세션에서 로그인 회원번호 가져오기

        프로젝트마다 로그인 정보를 세션에 저장하는 방식이 다를 수 있습니다.

        여기서는 session에 "memberSeq"라는 이름으로 회원번호가 들어있다고 가정했습니다.

        만약 네 프로젝트에서 로그인 정보를 다른 이름으로 저장하고 있다면
        "memberSeq" 부분을 바꿔야 합니다.
    */
    private Long getLoginMemberSeq(HttpSession session) {

        Object value = session.getAttribute("memberSeq");

        /*
            로그인하지 않았거나 세션에 회원번호가 없으면 null 반환
        */
        if (value == null) {
            return null;
        }

        /*
            세션 값이 Long 타입인 경우
        */
        if (value instanceof Long) {
            return (Long) value;
        }

        /*
            세션 값이 Integer 타입인 경우
        */
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }

        /*
            세션 값이 String 타입인 경우
        */
        if (value instanceof String) {
            return Long.parseLong((String) value);
        }

        return null;
    }
    
    /*
        상품 삭제
    
        상품번호를 기준으로 상품을 삭제합니다.
    
        실제 DELETE가 아니라
        status = 'DELETED', hide_yn = 'Y'로 변경하는 방식입니다.
    
        예:
        DELETE /api/product/1494
    */
    @DeleteMapping("/api/product/{productSeq}")
    public ResponseEntity<?> deleteProduct(
            @PathVariable(name = "productSeq") Long productSeq) {
    
        /*
            상품 삭제 처리
        */
        boolean result = productService.deleteProduct(productSeq);
    
        /*
            삭제할 상품이 없으면 404 응답
        */
        if (!result) {
            return ResponseEntity.notFound().build();
        }
    
        /*
            삭제 성공 시 200 응답
        */
        return ResponseEntity.ok("상품이 삭제되었습니다.");
    }
}