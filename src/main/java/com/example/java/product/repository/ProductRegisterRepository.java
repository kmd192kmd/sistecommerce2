package com.example.java.product.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.java.product.dto.ProductCreateRequestDto;
import com.example.java.product.dto.ProductCreateRequestDto.ProductImageRequestDto;
import com.example.java.product.dto.ProductCreateRequestDto.ProductOptionRequestDto;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductRegisterRepository {

    private final JdbcTemplate jdbcTemplate;


    /*
        현재 테이블의 다음 seq 값을 구합니다.

        지금 DB에 시퀀스가 따로 없다는 전제로 만든 테스트용 방식입니다.

        예:
        product 테이블의 seq가 1, 2, 3까지 있으면
        다음 값은 4를 반환합니다.

        실무에서는 MAX(seq) + 1보다 Oracle Sequence 사용을 추천합니다.
    */
    public Long getNextSeq(String tableName) {

        String sql = "SELECT NVL(MAX(seq), 0) + 1 FROM " + tableName;

        return jdbcTemplate.queryForObject(sql, Long.class);
    }


    /*
        상품 등록

        product 테이블에 상품 기본 정보를 저장합니다.

        상품 등록 직후에는 관리자 승인이 필요하므로
        approval_status는 PENDING으로 저장합니다.

        sale_status는 일단 ON_SALE로 넣지만,
        승인 전에는 화면에 노출되지 않게 approval_status로 제어합니다.

        수정된 부분:
        - product 테이블에 thumbnail_url 컬럼이 추가되었기 때문에
          대표 이미지 URL도 같이 저장합니다.
        - thumbnailUrl 값은 Cloudinary에서 업로드 후 반환받은 secure_url입니다.
    */
    public void insertProduct(Long productSeq,
                              ProductCreateRequestDto dto,
                              String thumbnailUrl) {

        String sql = """
                INSERT INTO product (
                    seq,
                    seller_seq,
                    category_seq,
                    product_name,
                    price,
                    content,
                    sale_status,
                    approval_status,
                    hide_yn,
                    view_count,
                    avg_rating,
                    review_count,
                    sales_count,
                    created_date,
                    updated_date,
                    status,
                    thumbnail_url
                ) VALUES (
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    'ON_SALE',
                    'PENDING',
                    'N',
                    0,
                    0,
                    0,
                    0,
                    SYSDATE,
                    NULL,
                    'NORMAL',
                    ?
                )
                """;

        jdbcTemplate.update(
                sql,
                productSeq,
                dto.getSellerSeq(),
                dto.getCategorySeq(),
                dto.getProductName(),
                dto.getPrice(),
                dto.getContent(),
                thumbnailUrl
        );
    }


    /*
        상품 이미지 등록

        product_image 테이블에 상품 이미지를 저장합니다.

        image_url:
        - Cloudinary 이미지 URL

        public_id:
        - Cloudinary에서 이미지 수정/삭제할 때 사용하는 값

        thumbnail_yn:
        - 대표 이미지 여부
        - Y 또는 N
    */
    public void insertProductImage(Long imageSeq,
                                   Long productSeq,
                                   ProductImageRequestDto imageDto) {

        String sql = """
                INSERT INTO product_image (
                    seq,
                    product_seq,
                    image_url,
                    public_id,
                    thumbnail_yn,
                    image_order,
                    created_date,
                    status
                ) VALUES (
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    SYSDATE,
                    'NORMAL'
                )
                """;

        jdbcTemplate.update(
                sql,
                imageSeq,
                productSeq,
                imageDto.getImageUrl(),
                imageDto.getPublicId(),
                imageDto.getThumbnailYn(),
                imageDto.getImageOrder()
        );
    }


    /*
        상품 옵션 등록

        options 테이블에 상품 옵션을 저장합니다.

        옵션은 상품 카테고리마다 사용하는 항목이 다를 수 있으므로
        값이 없는 컬럼은 null로 저장됩니다.

        예:
        식품이면 taste, volume_weight 사용
        의류면 color, size 사용
        전자제품이면 memory, storage_capacity 사용
    */
    public void insertProductOption(Long optionSeq,
                                    Long productSeq,
                                    ProductOptionRequestDto optionDto) {

        String sql = """
                INSERT INTO options (
                    seq,
                    product_seq,
                    color,
                    options_size,
                    volume_weight,
                    taste,
                    storage_type,
                    scent_ingredient,
                    voltage,
                    quantity_set,
                    size_spec,
                    storage_capacity,
                    memory,
                    switch_axis,
                    connection_type,
                    wearable_spec,
                    material_type,
                    options_type,
                    stock,
                    safety_stock,
                    additional_price
                ) VALUES (
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?
                )
                """;

        jdbcTemplate.update(
                sql,
                optionSeq,
                productSeq,
                optionDto.getColor(),
                optionDto.getOptionsSize(),
                optionDto.getVolumeWeight(),
                optionDto.getTaste(),
                optionDto.getStorageType(),
                optionDto.getScentIngredient(),
                optionDto.getVoltage(),
                optionDto.getQuantitySet(),
                optionDto.getSizeSpec(),
                optionDto.getStorageCapacity(),
                optionDto.getMemory(),
                optionDto.getSwitchAxis(),
                optionDto.getConnectionType(),
                optionDto.getWearableSpec(),
                optionDto.getMaterialType(),
                optionDto.getOptionsType(),
                optionDto.getStock(),
                optionDto.getSafetyStock(),
                optionDto.getAdditionalPrice()
        );
    }


    /*
        상품 등록 요청 생성

        product_request 테이블에 관리자 승인 요청 데이터를 저장합니다.

        request_type:
        - REGISTER : 상품 등록 요청
        - UPDATE   : 상품 수정 요청

        request_status:
        - PENDING  : 승인 대기
        - APPROVED : 승인
        - REJECTED : 반려
    */
    public void insertProductRequest(Long requestSeq,
                                     Long productSeq,
                                     ProductCreateRequestDto dto) {

        String sql = """
                INSERT INTO product_request (
                    seq,
                    product_seq,
                    seller_seq,
                    admin_seq,
                    request_type,
                    request_status,
                    reject_reason,
                    request_date,
                    process_date
                ) VALUES (
                    ?,
                    ?,
                    ?,
                    ?,
                    'REGISTER',
                    'PENDING',
                    NULL,
                    SYSDATE,
                    NULL
                )
                """;

        jdbcTemplate.update(
                sql,
                requestSeq,
                productSeq,
                dto.getSellerSeq(),
                dto.getAdminSeq()
        );
    }


    /*
        판매자 존재 여부 확인

        seller_seq가 실제 seller 테이블에 있는지 확인합니다.
    */
    public boolean existsSeller(Long sellerSeq) {

        String sql = """
                SELECT COUNT(*)
                FROM seller
                WHERE seq = ?
                  AND status = 1
                """;

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, sellerSeq);

        return count != null && count > 0;
    }
}