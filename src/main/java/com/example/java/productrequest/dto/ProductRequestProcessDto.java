package com.example.java.productrequest.dto;

import java.util.List;

import lombok.Data;

@Data
public class ProductRequestProcessDto {
	private List<Long> requestSeqList;
    private Long adminSeq;
    private String rejectReason;
}
