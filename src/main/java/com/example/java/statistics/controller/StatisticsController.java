package com.example.java.statistics.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.java.statistics.dto.MainDashboardDTO;
import com.example.java.statistics.dto.SalesTrendDTO;
import com.example.java.statistics.service.StatisticsService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class StatisticsController {

	private final StatisticsService statisticsService;
	
	@GetMapping("/admin")
	public String getMainPage(Model model) {

		MainDashboardDTO dashboard = statisticsService.getDashboard();
		List<SalesTrendDTO> salesTrend = statisticsService.getSalesTrend("7d");
		
	    model.addAttribute("dashboard", dashboard);
	    model.addAttribute("salesTrend", salesTrend);
	    
	    model.addAttribute("productRating",
                statisticsService.getTopProductSales());
        model.addAttribute("categoryRating",
                statisticsService.getTopCategorySales());
        model.addAttribute("sellerRating",
                statisticsService.getTopSellerSales());

        model.addAttribute(
                "productStatusPie",
                statisticsService.getProductStatusStatistics());
        model.addAttribute(
                "deliveryStatusPie",
                statisticsService.getDeliveryStatusStatistics());
        
	    return "admin/statistics/main";
	}
}
