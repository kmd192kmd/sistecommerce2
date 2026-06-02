package com.example.java.storefront;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 스토어프론트(고객) 화면 라우팅.
 * 현재는 SampleProducts 더미 데이터로 템플릿을 렌더링한다. (DB 연동 시 Service 로 교체)
 * 메인("/") 은 기존 MainController 가 담당한다.
 */
@Controller
public class StorefrontController {

    // 쇼핑몰 목록 (SHOP-PRD-01)
    @GetMapping("/products")
    public String products(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        model.addAttribute("keyword", keyword);
        model.addAttribute("products", SampleProducts.products());
        return "product/list";
    }

    // 상품 상세 (SHOP-PRD-02)
    @GetMapping("/products/{seq}")
    public String productDetail(@PathVariable long seq, Model model) {
        model.addAttribute("product", SampleProducts.product(seq));
        return "product/detail";
    }

    // 공동구매 목록 (GB-01)
    @GetMapping("/group-buys")
    public String groupBuys(Model model) {
        model.addAttribute("groupBuys", SampleProducts.groupBuys());
        return "groupbuy/list";
    }

    // 공동구매 상세 (GB-02) — 동적 영역은 React mount (A방식)
    @GetMapping("/group-buys/{seq}")
    public String groupBuyDetail(@PathVariable long seq, Model model) {
        model.addAttribute("groupBuy", SampleProducts.groupBuy(seq));
        return "groupbuy/detail";
    }

    // 장바구니 (CART-MAIN-01)
    @GetMapping("/cart")
    public String cart(Model model) {
        model.addAttribute("cartItems", SampleProducts.cartItems());
        return "cart/cart";
    }

    // 결제
    @GetMapping("/order/checkout")
    public String checkout(Model model) {
        model.addAttribute("cartItems", SampleProducts.cartItems());
        return "order/checkout";
    }

    // 마이페이지 메인 = 주문목록 (MYP-MAIN-01 / MEM-ORD-01)
    @GetMapping("/mypage/orders")
    public String mypageOrders(Model model) {
        model.addAttribute("orders", SampleProducts.orders());
        return "mypage/orders";
    }
}
