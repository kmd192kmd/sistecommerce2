package com.example.java.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.java.admin.dto.AdminChartDto;
import com.example.java.admin.dto.AdminStatisticsDto;
import com.example.java.admin.service.AdminStatisticsService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final AdminStatisticsService adminStatisticsService;

    @GetMapping("/admin/statistics")
    public String dashboard(Model model) {
        AdminStatisticsDto stats = adminStatisticsService.getStatistics();
        model.addAttribute("stats", stats);
        return "admin/statistics/dashboard";
    }

    @GetMapping("/admin/statistics/chart")
    @ResponseBody
    public AdminChartDto getChartData(@RequestParam(value = "days", defaultValue = "14") int days) {
        return adminStatisticsService.getChartData(days);
    }
}
