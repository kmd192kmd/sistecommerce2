package com.example.java.product.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /*
        리뷰 이미지 업로드

        사용자가 선택한 이미지 파일을 Cloudinary에 업로드하고,
        DB에 저장할 이미지 URL을 반환합니다.
    */
    public String uploadReviewImage(MultipartFile file) {

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "gold-market/reviews",
                            "resource_type", "image"
                    )
            );

            return result.get("secure_url").toString();

        } catch (Exception e) {
            throw new RuntimeException("리뷰 이미지 업로드에 실패했습니다.", e);
        }
    }
}