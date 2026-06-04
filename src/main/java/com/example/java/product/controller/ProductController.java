package com.example.java.product.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.java.product.dto.ProductDto;
import com.example.java.product.service.ProductService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    /*
        ProductService

        Controller는 DB에 직접 접근하지 않고,
        Service를 통해 상품 관련 기능을 실행합니다.

        흐름:
        Controller → Service → Repository → DB
    */
    private final ProductService productService;


    /*
        상품 목록 조회 및 검색
    */
    @GetMapping("/list")
    public String getProducts(
            @RequestParam(value = "categorySeq", required = false) Long categorySeq,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        Page<ProductDto> products = productService.getProductList(categorySeq, keyword, sortBy, page);

        model.addAttribute("products", products);
        model.addAttribute("categorySeq", categorySeq);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("currentPage", page);

        return "product/list";
    }


    /*
        상품 상세 조회 1

        접속 주소:
        GET /product/{seq}

        예:
        /product/1

        상품 상세 화면으로 이동합니다.
        로그인한 회원 번호가 있으면 찜 여부 조회에도 사용할 수 있습니다.
    */
    @GetMapping("/view/{seq}")
    public String getProductDetail(
            @PathVariable("seq") Long seq,
            HttpSession session,
            Model model) {

        Long memberSeq = getLoginMemberSeq(session);

        ProductDto product = productService.getProductDetail(seq, memberSeq);

        model.addAttribute("product", product);

        return "product/detail";
    }


    /*
        상품 상세 조회 2

        접속 주소:
        GET /product/detail?seq=1

        RequestParam 방식입니다.
        기존 ProductDetailController에 있던 기능입니다.
    */
    @GetMapping("/detail")
    public String detail(
            @RequestParam("seq") Long productSeq,
            HttpSession session,
            Model model) {

        Long memberSeq = getLoginMemberSeq(session);

        ProductDto product = productService.getProductDetail(productSeq, memberSeq);

        model.addAttribute("product", product);

        return "product/detail";
    }


    /*
        상품 상세 조회 3

        접속 주소:
        GET /product/detail/1

        PathVariable 방식입니다.
        기존 ProductDetailController에 있던 기능입니다.
    */
    @GetMapping("/detail/{seq}")
    public String detailPath(
            @PathVariable("seq") Long productSeq,
            HttpSession session,
            Model model) {

        Long memberSeq = getLoginMemberSeq(session);

        ProductDto product = productService.getProductDetail(productSeq, memberSeq);

        model.addAttribute("product", product);

        return "product/detail";
    }


    /*
        상품 등록 폼

        접속 주소:
        GET /product/register
    */
    @GetMapping("/register")
    public String registerForm(Model model) {

        model.addAttribute("productDto", new ProductDto());

        return "product/register";
    }


    /*
        상품 등록 처리

        접속 주소:
        POST /product/register
    */
    @PostMapping("/register")
    public String createProduct(ProductDto dto) {

        productService.createProduct(dto);

        return "redirect:/product/list";
    }


    /*
        상품 수정 폼

        접속 주소:
        GET /product/edit/{seq}

        수정 화면에서는 조회수가 증가하면 안 되므로
        getProductWithoutViewCount()를 사용합니다.
    */
    @GetMapping("/edit/{seq}")
    public String editForm(
            @PathVariable("seq") Long seq,
            Model model) {

        ProductDto product = productService.getProductWithoutViewCount(seq);

        model.addAttribute("productDto", product);

        return "product/edit";
    }


    /*
        상품 수정 처리

        접속 주소:
        POST /product/edit/{seq}
    */
    @PostMapping("/edit/{seq}")
    public String updateProduct(
            @PathVariable("seq") Long seq,
            ProductDto dto) {

        productService.updateProduct(seq, dto);

        return "redirect:/product/view/" + seq;
    }


    /*
        상품 삭제 처리

        접속 주소:
        POST /product/delete/{seq}

        화면 form에서 삭제 버튼을 눌렀을 때 사용합니다.
    */
    @PostMapping("/delete/{seq}")
    public String deleteProduct(
            @PathVariable("seq") Long seq) {

        productService.deleteProduct(seq);

        return "redirect:/product/list";
    }


    /*
        상품 삭제 API

        접속 주소:
        DELETE /product/api/product/{productSeq}

        예:
        DELETE /product/api/product/1494

        실제 DELETE가 아니라
        status = 'DELETED',
        hide_yn = 'Y'
        로 변경하는 논리 삭제 방식입니다.
    */
    @DeleteMapping("/api/product/{productSeq}")
    public ResponseEntity<?> deleteProductApi(
            @PathVariable(name = "productSeq") Long productSeq) {

        boolean result = productService.deleteProduct(productSeq);

        if (!result) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok("상품이 삭제되었습니다.");
    }


    /*
        세션에서 로그인 회원번호 가져오기

        session에 "memberSeq"라는 이름으로 회원번호가 저장되어 있다고 가정합니다.

        만약 프로젝트에서 다른 이름을 사용하고 있다면
        "memberSeq" 부분만 바꾸면 됩니다.

        예:
        session.getAttribute("userSeq")
        session.getAttribute("loginMemberSeq")
        session.getAttribute("member")
    */
    private Long getLoginMemberSeq(HttpSession session) {

        Object value = session.getAttribute("memberSeq");

        if (value == null) {
            return null;
        }

        if (value instanceof Long) {
            return (Long) value;
        }

        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }

        if (value instanceof String) {
            return Long.parseLong((String) value);
        }

        return null;
    }
}