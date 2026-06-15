package com.example.java.statistics.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.java.statistics.dto.SalesTrendDTO;
import com.example.java.statistics.service.StatisticsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class StatisticsApiController {

	private final StatisticsService statisticsService;

	@GetMapping("/admin/statistics/sales-trend")
	public List<SalesTrendDTO> getSalesTrend(
	        @RequestParam(value = "period") String period) {

	    return statisticsService.getSalesTrend(period);
	}
}
