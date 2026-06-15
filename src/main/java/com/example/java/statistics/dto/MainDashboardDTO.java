package com.example.java.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MainDashboardDTO {
	private Long memberTotalCount;
	private Long productTotalCount;
	private Long ordersTotalCount;
}
