package com.example.java.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReviewEligibilityDto {

    private boolean writable;
    private String message;
}