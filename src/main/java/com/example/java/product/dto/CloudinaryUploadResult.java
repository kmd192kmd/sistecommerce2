package com.example.java.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CloudinaryUploadResult {

    private String imageUrl;

    private String publicId;
}