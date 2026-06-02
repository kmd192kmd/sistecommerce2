package com.example.java;

import com.example.java.storefront.SampleProducts;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    // 메인 화면 (COM-MAIN-01). auth 는 GlobalModelAdvice 가 주입한다.
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("groupBuys", SampleProducts.groupBuys());
        model.addAttribute("hotdeals", SampleProducts.hotdeals());
        model.addAttribute("populars", SampleProducts.populars());
        return "index";
    }
}
