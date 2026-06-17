package com.example.java.product.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.java.product.dto.CloudinaryUploadResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CloudinaryUploadService {

    private final Cloudinary cloudinary;

    /*
        상품 이미지를 Cloudinary에 업로드합니다.

        화면에서 사용자가 선택한 이미지 파일을 MultipartFile로 받은 뒤,
        Cloudinary에 업로드하고 이미지 URL과 public_id를 반환합니다.

        imageUrl:
        - 브라우저에서 이미지를 불러올 수 있는 Cloudinary URL

        publicId:
        - Cloudinary에서 이미지 수정/삭제 시 사용하는 고유 ID
    */
    public CloudinaryUploadResult uploadProductImage(MultipartFile file) {

        try {
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "product",
                            "resource_type", "image"
                    )
            );

            String imageUrl = String.valueOf(uploadResult.get("secure_url"));
            String publicId = String.valueOf(uploadResult.get("public_id"));

            return new CloudinaryUploadResult(imageUrl, publicId);

        } catch (IOException e) {
            throw new IllegalArgumentException("이미지 업로드 중 오류가 발생했습니다.");
        }
    }
}