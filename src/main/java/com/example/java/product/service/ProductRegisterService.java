package com.example.java.product.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.java.product.dto.CloudinaryUploadResult;
import com.example.java.product.dto.ProductCreateRequestDto;
import com.example.java.product.dto.ProductCreateRequestDto.ProductImageRequestDto;
import com.example.java.product.dto.ProductCreateRequestDto.ProductOptionRequestDto;
import com.example.java.product.dto.ProductCreateResponseDto;
import com.example.java.product.repository.ProductRegisterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductRegisterService {

    private final ProductRegisterRepository productRegisterRepository;
    private final CloudinaryUploadService cloudinaryUploadService;

    @Transactional
    public ProductCreateResponseDto createProduct(
            ProductCreateRequestDto dto,
            List<MultipartFile> images,
            List<MultipartFile> detailImages,
            int thumbnailIndex) {

        convertFormOptionsToOptionList(dto);

        validateCreateProduct(dto);
        validateImageFiles(images, thumbnailIndex);

        Long productSeq = productRegisterRepository.getNextSeq("product");
        Long requestSeq = productRegisterRepository.getNextSeq("product_request");

        String thumbnailUrl = null;
        List<ProductImageRequestDto> uploadedImageList = new ArrayList<>();

        for (int i = 0; i < images.size(); i++) {

            MultipartFile image = images.get(i);

            CloudinaryUploadResult uploadResult =
                    cloudinaryUploadService.uploadProductImage(image);

            ProductImageRequestDto imageDto = new ProductImageRequestDto();
            imageDto.setImageUrl(uploadResult.getImageUrl());
            imageDto.setPublicId(uploadResult.getPublicId());
            imageDto.setThumbnailYn(i == thumbnailIndex ? "Y" : "N");
            imageDto.setImageOrder(i + 1);

            uploadedImageList.add(imageDto);

            if (i == thumbnailIndex) {
                thumbnailUrl = uploadResult.getImageUrl();
            }
        }

        productRegisterRepository.insertProduct(productSeq, dto, thumbnailUrl);

        for (ProductImageRequestDto imageDto : uploadedImageList) {

            Long imageSeq = productRegisterRepository.getNextSeq("product_image");

            productRegisterRepository.insertProductImage(
                    imageSeq,
                    productSeq,
                    imageDto
            );
        }

        if (dto.getOptionList() != null && !dto.getOptionList().isEmpty()) {

            for (ProductOptionRequestDto optionDto : dto.getOptionList()) {

                Long optionSeq = productRegisterRepository.getNextSeq("options");

                productRegisterRepository.insertProductOption(
                        optionSeq,
                        productSeq,
                        optionDto
                );
            }

        } else {

            ProductOptionRequestDto defaultOption = new ProductOptionRequestDto();
            defaultOption.setStock(dto.getStock());
            defaultOption.setSafetyStock(0);
            defaultOption.setAdditionalPrice(0);

            Long optionSeq = productRegisterRepository.getNextSeq("options");

            productRegisterRepository.insertProductOption(
                    optionSeq,
                    productSeq,
                    defaultOption
            );
        }

        productRegisterRepository.insertProductRequest(
                requestSeq,
                productSeq,
                dto
        );

        return new ProductCreateResponseDto(
                productSeq,
                requestSeq,
                "상품 등록 요청이 관리자에게 전송되었습니다."
        );
    }

    /*
        옵션 조합 데이터를 ProductOptionRequestDto 목록으로 변환합니다.

        예:
        optionCombinationTypes = 색상||사이즈
        optionCombinationNames = 블랙||S

        결과:
        color = 블랙
        optionsSize = S
    */
    private void convertFormOptionsToOptionList(ProductCreateRequestDto dto) {

        if (dto.getOptionCombinationTypes() == null
                || dto.getOptionCombinationTypes().isEmpty()) {
            return;
        }

        List<ProductOptionRequestDto> optionList = new ArrayList<>();

        for (int i = 0; i < dto.getOptionCombinationTypes().size(); i++) {

            String typeText = getStringValue(dto.getOptionCombinationTypes(), i);
            String nameText = getStringValue(dto.getOptionCombinationNames(), i);

            Integer additionalPrice = getIntegerValue(dto.getOptionCombinationPrices(), i, 0);
            Integer stock = getIntegerValue(dto.getOptionCombinationStocks(), i, 0);
            Integer safetyStock = getIntegerValue(dto.getOptionCombinationSafeStocks(), i, 0);

            ProductOptionRequestDto optionDto = new ProductOptionRequestDto();

            setCombinationOptionValues(optionDto, typeText, nameText);

            optionDto.setAdditionalPrice(additionalPrice);
            optionDto.setStock(stock);
            optionDto.setSafetyStock(safetyStock);

            optionList.add(optionDto);
        }

        dto.setOptionList(optionList);
    }

    /*
        조합 문자열을 실제 options 테이블 컬럼에 맞게 넣습니다.
    */
    private void setCombinationOptionValues(ProductOptionRequestDto optionDto,
                                            String typeText,
                                            String nameText) {

        if (typeText == null || nameText == null) {
            return;
        }

        String[] types = typeText.split("\\|\\|");
        String[] names = nameText.split("\\|\\|");

        for (int i = 0; i < types.length; i++) {

            if (i >= names.length) {
                continue;
            }

            String optionType = types[i].trim();
            String optionName = names[i].trim();

            setOptionValue(optionDto, optionType, optionName);
        }
    }

    private String getStringValue(List<String> list, int index) {

        if (list == null || index >= list.size()) {
            return null;
        }

        String value = list.get(index);

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private Integer getIntegerValue(List<Integer> list, int index, int defaultValue) {

        if (list == null || index >= list.size() || list.get(index) == null) {
            return defaultValue;
        }

        return list.get(index);
    }

    private void setOptionValue(ProductOptionRequestDto optionDto,
                                String optionType,
                                String optionName) {

        if (optionType == null || optionName == null) {
            return;
        }

        switch (optionType) {
            case "색상" -> optionDto.setColor(optionName);
            case "사이즈" -> optionDto.setOptionsSize(optionName);
            case "용량/중량" -> optionDto.setVolumeWeight(optionName);
            case "맛" -> optionDto.setTaste(optionName);
            case "보관방식" -> optionDto.setStorageType(optionName);
            case "향/성분" -> optionDto.setScentIngredient(optionName);
            case "전력" -> optionDto.setVoltage(optionName);
            case "수량/구성" -> optionDto.setQuantitySet(optionName);
            case "크기/규격" -> optionDto.setSizeSpec(optionName);
            case "저장 용량" -> optionDto.setStorageCapacity(optionName);
            case "메모리" -> optionDto.setMemory(optionName);
            case "스위치/축" -> optionDto.setSwitchAxis(optionName);
            case "연결 방식" -> optionDto.setConnectionType(optionName);
            case "케어제품 구성" -> optionDto.setWearableSpec(optionName);
            case "재질" -> optionDto.setMaterialType(optionName);
            case "기타 유형" -> optionDto.setOptionsType(optionName);
        }
    }

    private void validateCreateProduct(ProductCreateRequestDto dto) {

        if (dto.getSellerSeq() == null) {
            throw new IllegalArgumentException("판매자 번호는 필수입니다.");
        }

        if (!productRegisterRepository.existsSeller(dto.getSellerSeq())) {
            throw new IllegalArgumentException("존재하지 않거나 활성 상태가 아닌 판매자입니다.");
        }

        if (dto.getCategorySeq() == null) {
            throw new IllegalArgumentException("카테고리 번호는 필수입니다.");
        }

        if (dto.getProductName() == null || dto.getProductName().isBlank()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }

        if (dto.getProductName().length() > 60) {
            throw new IllegalArgumentException("상품명은 최대 60자까지 입력할 수 있습니다.");
        }

        if (dto.getPrice() == null || dto.getPrice() < 0) {
            throw new IllegalArgumentException("상품 가격은 0원 이상이어야 합니다.");
        }

        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new IllegalArgumentException("상품 설명은 필수입니다.");
        }

        if (dto.getOptionList() != null && !dto.getOptionList().isEmpty()) {
            validateOptions(dto);
        } else {
            if (dto.getStock() == null || dto.getStock() < 0) {
                throw new IllegalArgumentException("상품 수량은 0개 이상이어야 합니다.");
            }
        }
    }

    private void validateImageFiles(List<MultipartFile> images, int thumbnailIndex) {

        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("상품 이미지는 1장 이상 등록해야 합니다.");
        }

        if (thumbnailIndex < 0 || thumbnailIndex >= images.size()) {
            throw new IllegalArgumentException("대표 이미지 선택값이 올바르지 않습니다.");
        }

        for (MultipartFile image : images) {

            if (image == null || image.isEmpty()) {
                throw new IllegalArgumentException("비어 있는 이미지 파일은 등록할 수 없습니다.");
            }

            String contentType = image.getContentType();

            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("이미지 파일만 등록할 수 있습니다.");
            }
        }
    }

    private void validateOptions(ProductCreateRequestDto dto) {

        for (ProductOptionRequestDto optionDto : dto.getOptionList()) {

            if (optionDto.getStock() == null || optionDto.getStock() < 0) {
                throw new IllegalArgumentException("옵션 재고는 0개 이상이어야 합니다.");
            }

            if (optionDto.getSafetyStock() == null || optionDto.getSafetyStock() < 0) {
                throw new IllegalArgumentException("옵션 안전재고는 0개 이상이어야 합니다.");
            }

            if (optionDto.getAdditionalPrice() == null || optionDto.getAdditionalPrice() < 0) {
                throw new IllegalArgumentException("옵션 추가금액은 0원 이상이어야 합니다.");
            }

            if (!hasAnyOptionValue(optionDto)) {
                throw new IllegalArgumentException("옵션명을 입력해주세요.");
            }
        }
    }

    private boolean hasAnyOptionValue(ProductOptionRequestDto optionDto) {

        return hasText(optionDto.getColor())
                || hasText(optionDto.getOptionsSize())
                || hasText(optionDto.getVolumeWeight())
                || hasText(optionDto.getTaste())
                || hasText(optionDto.getStorageType())
                || hasText(optionDto.getScentIngredient())
                || hasText(optionDto.getVoltage())
                || hasText(optionDto.getQuantitySet())
                || hasText(optionDto.getSizeSpec())
                || hasText(optionDto.getStorageCapacity())
                || hasText(optionDto.getMemory())
                || hasText(optionDto.getSwitchAxis())
                || hasText(optionDto.getConnectionType())
                || hasText(optionDto.getWearableSpec())
                || hasText(optionDto.getMaterialType())
                || hasText(optionDto.getOptionsType());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}