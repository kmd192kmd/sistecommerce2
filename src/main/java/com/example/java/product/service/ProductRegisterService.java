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


    /*
        상품 등록 처리

        한 번의 상품 등록에서 처리하는 작업:

        1. 입력값 검증
        2. Cloudinary 이미지 업로드
        3. product 테이블 INSERT
           - thumbnail_url 컬럼에 대표 이미지 Cloudinary URL 저장
        4. product_image 테이블 INSERT
           - Cloudinary image_url, public_id 저장
        5. options 테이블 INSERT
           - 옵션은 없어도 됨
           - 옵션이 있을 때만 저장
        6. product_request 테이블 INSERT

        @Transactional
        - 중간에 하나라도 실패하면 전체 INSERT가 취소됩니다.
    */
    @Transactional
    public ProductCreateResponseDto createProduct(
            ProductCreateRequestDto dto,
            List<MultipartFile> images,
            List<MultipartFile> detailImages,
            int thumbnailIndex) {

        /*
            추가된 부분

            현재 HTML에서는 optionList[0].color 같은 구조로 옵션을 보내지 않고,
            optionTypes, optionNames, optionPrices, optionStocks, optionSafeStocks라는
            각각의 배열 형태로 옵션 값을 보냅니다.

            그래서 검증하기 전에 먼저 화면에서 넘어온 옵션 배열을
            ProductOptionRequestDto 목록으로 변환합니다.
        */
        convertFormOptionsToOptionList(dto);

        validateCreateProduct(dto);
        validateImageFiles(images, thumbnailIndex);

        Long productSeq = productRegisterRepository.getNextSeq("product");

        /*
            수정된 부분

            product_request 테이블에도 INSERT 해야 하므로
            product_request의 다음 seq 값을 구합니다.

            기존에는 admin 데이터가 없다는 이유로 주석 처리되어 있었지만,
            product_request.admin_seq는 NULL 허용이므로
            adminSeq가 없어도 요청 생성이 가능합니다.
        */
        Long requestSeq = productRegisterRepository.getNextSeq("product_request");

        /*
            이미지 업로드

            화면에서 전달받은 이미지 파일들을 Cloudinary에 업로드합니다.

            업로드 후 Cloudinary가 반환하는 값:
            - secure_url → DB의 image_url / thumbnail_url에 저장
            - public_id  → DB의 public_id에 저장

            thumbnailIndex에 해당하는 이미지를 대표 이미지로 지정합니다.
        */
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

        /*
            상품 등록

            product 테이블에 상품 기본 정보와 대표 이미지 URL을 저장합니다.

            thumbnailUrl:
            - Cloudinary에서 가져온 대표 이미지 URL
            - product.thumbnail_url 컬럼에 들어갑니다.
        */
        productRegisterRepository.insertProduct(productSeq, dto, thumbnailUrl);

        /*
            이미지 등록

            Cloudinary에 업로드된 이미지 정보를 product_image 테이블에 저장합니다.
        */
        for (ProductImageRequestDto imageDto : uploadedImageList) {

            Long imageSeq = productRegisterRepository.getNextSeq("product_image");

            productRegisterRepository.insertProductImage(
                    imageSeq,
                    productSeq,
                    imageDto
            );
        }

        /*
            옵션 등록

            수정된 부분:
            옵션은 없어도 됩니다.

            dto.getOptionList()가 null이거나 비어 있으면
            옵션 저장 과정을 건너뜁니다.

            옵션 목록이 있을 때만 반복문을 돌면서 options 테이블에 저장합니다.
        */
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

            /*
                추가된 부분

                옵션을 하나도 선택하지 않은 경우에도 재고는 저장되어야 합니다.

                현재 product 테이블에는 상품 수량 컬럼이 없고,
                options 테이블에 stock 컬럼이 있습니다.

                그래서 옵션을 선택하지 않은 상품은
                options 테이블에 "기본 옵션" 1개를 자동으로 저장합니다.

                예:
                product_seq       = 방금 등록된 상품 번호
                stock             = 화면에서 입력한 상품 수량
                safety_stock      = 0
                additional_price  = 0
                나머지 옵션 컬럼 = NULL

                이렇게 저장하면 기존 상품 상세 조회에서도
                옵션명이 비어 있는 경우 "기본 옵션"으로 표시할 수 있습니다.
            */
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

        /*
            수정된 부분

            상품 등록 요청을 product_request 테이블에 저장합니다.

            이 INSERT가 있어야 관리자 페이지에서
            "새 상품 등록 요청"을 조회할 수 있습니다.

            request_type   = REGISTER
            request_status = PENDING
            admin_seq      = null 가능
        */
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
        추가된 부분

        화면에서 넘어온 옵션 배열을 ProductOptionRequestDto 목록으로 변환합니다.

        현재 HTML 구조:
        - optionTypes      : 옵션 종류
        - optionNames      : 옵션 값
        - optionPrices     : 추가금액
        - optionStocks     : 재고
        - optionSafeStocks : 안전재고

        변환 예:
        optionType = "색상", optionName = "블랙"
        → optionDto.color = "블랙"

        optionType = "사이즈", optionName = "M"
        → optionDto.optionsSize = "M"

        이렇게 변환해야 ProductRegisterRepository.insertProductOption()에서
        options 테이블의 각 컬럼에 정상적으로 INSERT할 수 있습니다.
    */
    private void convertFormOptionsToOptionList(ProductCreateRequestDto dto) {

        if (dto.getOptionTypes() == null || dto.getOptionTypes().isEmpty()) {
            return;
        }

        List<ProductOptionRequestDto> optionList = new ArrayList<>();

        for (int i = 0; i < dto.getOptionTypes().size(); i++) {

            String optionType = dto.getOptionTypes().get(i);
            String optionName = getStringValue(dto.getOptionNames(), i);

            Integer additionalPrice = getIntegerValue(dto.getOptionPrices(), i, 0);
            Integer stock = getIntegerValue(dto.getOptionStocks(), i, 0);
            Integer safetyStock = getIntegerValue(dto.getOptionSafeStocks(), i, 0);

            ProductOptionRequestDto optionDto = new ProductOptionRequestDto();

            setOptionValue(optionDto, optionType, optionName);

            optionDto.setAdditionalPrice(additionalPrice);
            optionDto.setStock(stock);
            optionDto.setSafetyStock(safetyStock);

            optionList.add(optionDto);
        }

        dto.setOptionList(optionList);
    }


    /*
        추가된 부분

        List<String>에서 index 위치의 문자열을 안전하게 꺼냅니다.

        옵션명 입력칸이 비어 있으면 null로 반환해서
        options 테이블의 해당 옵션 컬럼에 null이 들어가게 합니다.
    */
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


    /*
        추가된 부분

        List<Integer>에서 index 위치의 숫자를 안전하게 꺼냅니다.

        값이 없으면 defaultValue를 반환합니다.

        예:
        추가금액이 비어 있으면 0
        재고가 비어 있으면 0
        안전재고가 비어 있으면 0
    */
    private Integer getIntegerValue(List<Integer> list, int index, int defaultValue) {

        if (list == null || index >= list.size() || list.get(index) == null) {
            return defaultValue;
        }

        return list.get(index);
    }


    /*
        추가된 부분

        화면에서 선택한 옵션 종류에 따라
        ProductOptionRequestDto의 알맞은 필드에 옵션 값을 넣습니다.

        options 테이블에는 옵션 종류별로 컬럼이 나뉘어 있습니다.

        예:
        색상       → color
        사이즈     → options_size
        용량/중량  → volume_weight
        맛         → taste
    */
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


    /*
        상품 등록 입력값 검증

        이미지 검증은 validateImageFiles()에서 처리합니다.
        이유:
        - 이제 이미지 URL을 JSON으로 받는 방식이 아니라,
          화면에서 MultipartFile로 이미지 파일을 받기 때문입니다.
    */
    private void validateCreateProduct(ProductCreateRequestDto dto) {

        if (dto.getSellerSeq() == null) {
            throw new IllegalArgumentException("판매자 번호는 필수입니다.");
        }

        if (!productRegisterRepository.existsSeller(dto.getSellerSeq())) {
            throw new IllegalArgumentException("존재하지 않거나 활성 상태가 아닌 판매자입니다.");
        }

        /*
            현재 테스트에서는 사용하지 않음
            - admin 데이터가 없어서 관리자 검증을 임시 제외함
        */
        /*
        if (dto.getAdminSeq() == null) {
            throw new IllegalArgumentException("관리자 번호는 필수입니다.");
        }

        if (!productRegisterRepository.existsAdmin(dto.getAdminSeq())) {
            throw new IllegalArgumentException("존재하지 않거나 비활성 상태인 관리자입니다.");
        }
        */

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

        /*
            수정된 부분:
            옵션은 없어도 되므로 optionList 필수 검증은 하지 않습니다.

            단, 옵션 목록이 전달된 경우에는
            재고, 안전재고, 추가금액 값이 정상인지 검증합니다.
        */
        if (dto.getOptionList() != null && !dto.getOptionList().isEmpty()) {
            validateOptions(dto);
        } else {

            /*
                추가된 부분

                옵션을 선택하지 않은 경우에는
                상품 수량 입력칸의 stock 값을 필수로 검사합니다.

                이 stock 값은 이후 options 테이블에
                기본 옵션 1개로 저장됩니다.
            */
            if (dto.getStock() == null || dto.getStock() < 0) {
                throw new IllegalArgumentException("상품 수량은 0개 이상이어야 합니다.");
            }
        }
    }


    /*
        이미지 파일 검증

        화면에서 사용자가 선택한 이미지 파일을 검증합니다.

        조건:
        - 이미지는 최소 1개 이상 필요합니다.
        - thumbnailIndex는 이미지 목록 범위 안에 있어야 합니다.
        - 빈 파일은 등록할 수 없습니다.
        - image 타입 파일만 등록할 수 있습니다.
    */
    private void validateImageFiles(List<MultipartFile> images, int thumbnailIndex) {

        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("상품 이미지는 최소 1개 이상 필요합니다.");
        }

        if (thumbnailIndex < 0 || thumbnailIndex >= images.size()) {
            throw new IllegalArgumentException("대표 이미지 순서가 올바르지 않습니다.");
        }

        for (MultipartFile image : images) {

            if (image == null || image.isEmpty()) {
                throw new IllegalArgumentException("비어 있는 이미지는 등록할 수 없습니다.");
            }

            String contentType = image.getContentType();

            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("이미지 파일만 등록할 수 있습니다.");
            }
        }
    }


    /*
        옵션 검증

        옵션이 있을 때만 실행됩니다.

        옵션 자체는 선택사항이지만,
        옵션을 입력했다면 재고, 안전재고, 추가금액 값은 정상이어야 합니다.
    */
    private void validateOptions(ProductCreateRequestDto dto) {

        for (ProductOptionRequestDto optionDto : dto.getOptionList()) {

            if (optionDto.getStock() == null || optionDto.getStock() < 0) {
                throw new IllegalArgumentException("재고는 0개 이상이어야 합니다.");
            }

            if (optionDto.getSafetyStock() == null || optionDto.getSafetyStock() < 0) {
                throw new IllegalArgumentException("안전재고는 0개 이상이어야 합니다.");
            }

            if (optionDto.getAdditionalPrice() == null) {
                optionDto.setAdditionalPrice(0);
            }

            if (optionDto.getAdditionalPrice() < 0) {
                throw new IllegalArgumentException("추가 금액은 0원 이상이어야 합니다.");
            }

            /*
                추가된 부분

                옵션을 선택했는데 옵션명 자체가 비어 있으면
                나중에 화면에서 옵션 구분이 어려워질 수 있습니다.

                예:
                색상 옵션을 체크했는데 옵션명을 비워두면
                color, options_size, taste 등 모든 옵션 컬럼이 null인 상태로 저장됩니다.

                그래서 옵션 컬럼 중 하나라도 값이 있는지 검사합니다.
            */
            if (!hasAnyOptionValue(optionDto)) {
                throw new IllegalArgumentException("옵션명을 입력해주세요.");
            }
        }
    }


    /*
        추가된 부분

        옵션 DTO 안에 실제 옵션 값이 하나라도 들어 있는지 확인합니다.

        색상, 사이즈, 맛, 메모리 등 옵션 컬럼 중
        하나라도 값이 있으면 true를 반환합니다.
    */
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


    /*
        추가된 부분

        문자열이 null이 아니고, 공백만 있는 값도 아닌지 확인합니다.
    */
    private boolean hasText(String value) {

        return value != null && !value.isBlank();
    }
}