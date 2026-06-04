package com.example.java.product.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.java.product.dto.ProductDto;
import com.example.java.product.dto.ProductDto.ProductImageDto;
import com.example.java.product.dto.ProductDto.ProductOptionDto;
import com.example.java.product.entity.Product;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

/*
    ProductRepository

    상품 관련 DB 작업을 담당하는 Repository 클래스입니다.

    주의:
    이 클래스는 interface가 아니라 class입니다.
    그래서 JpaRepository를 상속하지 않습니다.

    대신 EntityManager를 이용해서
    save(), findById()를 직접 구현했습니다.

    Controller → Service → Repository → DB
*/
@Repository
@RequiredArgsConstructor
public class ProductRepository {

    /*
        NamedParameterJdbcTemplate

        SQL을 직접 작성해서 조회할 때 사용합니다.

        일반 JdbcTemplate은 ? 순서에 맞춰 값을 넣어야 하지만,
        NamedParameterJdbcTemplate은 :productSeq처럼 이름으로 값을 넣을 수 있습니다.
    */
    private final NamedParameterJdbcTemplate jdbcTemplate;

    /*
        EntityManager

        JpaRepository를 사용하지 않기 때문에
        save(), findById() 기능을 직접 구현하기 위해 사용합니다.
    */
    @PersistenceContext
    private EntityManager entityManager;


    /*
        상품 저장 / 수정

        product.getSeq()가 null이면 신규 등록입니다.
        product.getSeq()가 있으면 기존 데이터 수정입니다.
    */
    public Product save(Product product) {

        if (product.getSeq() == null) {
            entityManager.persist(product);
            return product;
        }

        return entityManager.merge(product);
    }


    /*
        상품 번호로 상품 엔티티 조회

        JpaRepository의 findById() 역할을 직접 구현한 메서드입니다.
    */
    public Optional<Product> findById(Long seq) {

        return Optional.ofNullable(entityManager.find(Product.class, seq));
    }


    /*
        상품 필터 검색

        기존 목록 조회에서 사용하는 메서드입니다.

        기능:
        - 카테고리 필터
        - 검색어 필터
        - 최소 가격
        - 최대 가격
        - 최소 평점
        - 판매 상태
        - 숨김 상품 제외
        - 판매중지 상품 제외
        - 삭제 상품 제외
        - 페이징 처리
    */
    public Page<Product> findWithFilters(
            List<Long> categorySeqs,
            String keyword,
            Integer minPrice,
            Integer maxPrice,
            Double minRating,
            String saleStatus,
            Pageable pageable) {

        StringBuilder where = new StringBuilder("""
                WHERE p.hide_yn = 'N'
                  AND p.sale_status <> 'STOPPED'
                  AND p.status = 'NORMAL'
                """);

        MapSqlParameterSource params = new MapSqlParameterSource();

        /*
            카테고리 조건

            categorySeqs가 null이 아니고 비어 있지 않을 때만
            IN 조건을 추가합니다.
        */
        if (categorySeqs != null && !categorySeqs.isEmpty()) {
            where.append(" AND p.category_seq IN (:categorySeqs) ");
            params.addValue("categorySeqs", categorySeqs);
        }

        /*
            검색어 조건

            상품명과 검색어의 공백을 제거하고 비교합니다.
        */
        if (keyword != null && !keyword.isBlank()) {
            where.append("""
                    AND LOWER(REPLACE(p.product_name, ' ', ''))
                        LIKE LOWER('%' || REPLACE(:keyword, ' ', '') || '%')
                    """);
            params.addValue("keyword", keyword);
        }

        if (minPrice != null) {
            where.append(" AND p.price >= :minPrice ");
            params.addValue("minPrice", minPrice);
        }

        if (maxPrice != null) {
            where.append(" AND p.price <= :maxPrice ");
            params.addValue("maxPrice", maxPrice);
        }

        if (minRating != null) {
            where.append(" AND p.avg_rating >= :minRating ");
            params.addValue("minRating", minRating);
        }

        if (saleStatus != null && !saleStatus.isBlank()) {
            where.append(" AND p.sale_status = :saleStatus ");
            params.addValue("saleStatus", saleStatus);
        }

        /*
            전체 개수 조회

            Page 객체를 만들려면 현재 페이지 데이터뿐 아니라
            전체 상품 개수도 필요합니다.
        */
        String countSql = """
                SELECT COUNT(*)
                FROM product p
                """ + where;

        Integer totalCount = jdbcTemplate.queryForObject(countSql, params, Integer.class);

        /*
            페이징 값 추가
        */
        params.addValue("offset", pageable.getOffset());
        params.addValue("size", pageable.getPageSize());

        /*
            실제 상품 목록 조회 SQL
        */
        String sql = """
                SELECT
                    p.seq,
                    p.seller_seq,
                    p.category_seq,
                    p.product_name,
                    p.price,
                    p.content,
                    p.sale_status,
                    p.approval_status,
                    p.hide_yn,
                    p.view_count,
                    p.avg_rating,
                    p.review_count,
                    p.sales_count,
                    p.created_date,
                    p.updated_date,
                    p.status
                FROM product p
                """ + where + """
                ORDER BY p.seq DESC
                OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY
                """;

        List<Product> products = jdbcTemplate.query(sql, params, this::mapProduct);

        return new PageImpl<>(
                products,
                pageable,
                totalCount == null ? 0 : totalCount
        );
    }


    /*
        상품 상세 기본 정보 조회

        상품 상세 화면 상단에 필요한 데이터를 조회합니다.

        조회 조건:
        - 상품 번호 일치
        - 정상 상품
        - 관리자 승인 완료
        - 숨김 아님

        대표 이미지 thumbnail_url도 함께 조회합니다.
    */
    public Optional<ProductDto> findProductDetail(Long productSeq) {

        String sql = """
                SELECT
                    p.seq,
                    p.seller_seq,
                    p.category_seq,
                    p.product_name,
                    p.price,
                    p.content,
                    p.sale_status,
                    p.approval_status,
                    p.hide_yn,
                    p.view_count,
                    p.avg_rating,
                    p.review_count,
                    p.sales_count,
                    p.created_date,
                    p.updated_date,
                    p.status,
                    (
                        SELECT pi.image_url
                        FROM product_image pi
                        WHERE pi.product_seq = p.seq
                          AND pi.thumbnail_yn = 'Y'
                          AND pi.status = 'NORMAL'
                        ORDER BY pi.image_order
                        FETCH FIRST 1 ROWS ONLY
                    ) AS thumbnail_url
                FROM product p
                WHERE p.seq = :productSeq
                  AND p.status = 'NORMAL'
                  AND p.approval_status = 'APPROVED'
                  AND p.hide_yn = 'N'
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("productSeq", productSeq);

        List<ProductDto> list = jdbcTemplate.query(sql, params, this::mapProductDetail);

        if (list.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(list.get(0));
    }


    /*
        상품 목록 전체 개수 조회

        API 페이징 조회에서 사용합니다.
    */
    public int countProducts() {

        String sql = """
                SELECT COUNT(*)
                FROM product
                WHERE status = 'NORMAL'
                  AND approval_status = 'APPROVED'
                  AND hide_yn = 'N'
                """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource(),
                Integer.class
        );

        return count == null ? 0 : count;
    }


    /*
        상품 목록 페이징 조회

        API용 상품 목록 조회입니다.

        offset:
        - 앞에서 몇 개를 건너뛸지

        size:
        - 몇 개를 가져올지
    */
    public List<ProductDto> findProductsByPaging(int offset, int size) {

        String sql = """
                SELECT
                    p.seq,
                    p.seller_seq,
                    p.category_seq,
                    p.product_name,
                    p.price,
                    p.content,
                    p.sale_status,
                    p.approval_status,
                    p.hide_yn,
                    p.view_count,
                    p.avg_rating,
                    p.review_count,
                    p.sales_count,
                    p.created_date,
                    p.updated_date,
                    p.status,
                    (
                        SELECT pi.image_url
                        FROM product_image pi
                        WHERE pi.product_seq = p.seq
                          AND pi.thumbnail_yn = 'Y'
                          AND pi.status = 'NORMAL'
                        ORDER BY pi.image_order
                        FETCH FIRST 1 ROWS ONLY
                    ) AS thumbnail_url
                FROM product p
                WHERE p.status = 'NORMAL'
                  AND p.approval_status = 'APPROVED'
                  AND p.hide_yn = 'N'
                ORDER BY p.seq DESC
                OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("offset", offset)
                .addValue("size", size);

        return jdbcTemplate.query(sql, params, this::mapProductDetail);
    }


    /*
        상품 이미지 목록 조회

        product_image 테이블에서 해당 상품의 이미지를 조회합니다.
    */
    public List<ProductImageDto> findProductImages(Long productSeq) {

        String sql = """
                SELECT
                    seq,
                    product_seq,
                    image_url,
                    public_id,
                    thumbnail_yn,
                    image_order,
                    status
                FROM product_image
                WHERE product_seq = :productSeq
                  AND status = 'NORMAL'
                ORDER BY thumbnail_yn DESC, image_order ASC, seq ASC
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("productSeq", productSeq);

        return jdbcTemplate.query(sql, params, this::mapProductImage);
    }


    /*
        상품 옵션 목록 조회

        options 테이블에서 해당 상품의 옵션을 조회합니다.

        현재 조건:
        - stock > 0

        즉 재고가 있는 옵션만 조회합니다.
    */
    public List<ProductOptionDto> findProductOptions(Long productSeq) {

        String sql = """
                SELECT
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
                FROM options
                WHERE product_seq = :productSeq
                  AND stock > 0
                ORDER BY seq ASC
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("productSeq", productSeq);

        return jdbcTemplate.query(sql, params, this::mapProductOption);
    }


    /*
        찜 여부 확인

        product_wish 테이블에는 product_seq가 아니라
        options_seq가 저장되어 있는 구조라고 가정했습니다.

        그래서 현재 상품의 옵션 번호 중 사용자가 찜한 것이 있는지 확인합니다.
    */
    public boolean existsWish(Long productSeq, Long memberSeq) {

        String sql = """
                SELECT COUNT(*)
                FROM product_wish
                WHERE options_seq IN (
                    SELECT seq
                    FROM options
                    WHERE product_seq = :productSeq
                )
                AND member_seq = :memberSeq
                AND status = 0
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("productSeq", productSeq)
                .addValue("memberSeq", memberSeq);

        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);

        return count != null && count > 0;
    }


    /*
        상품 삭제

        실제 DELETE가 아니라
        status와 hide_yn 값을 변경하는 소프트 삭제 방식입니다.
    */
    public int deleteProduct(Long productSeq) {

        String sql = """
                UPDATE product
                SET status = 'DELETED',
                    hide_yn = 'Y',
                    updated_date = SYSDATE
                WHERE seq = :productSeq
                  AND status = 'NORMAL'
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("productSeq", productSeq);

        return jdbcTemplate.update(sql, params);
    }


    /*
        Product 엔티티 매핑 메서드

        findWithFilters()에서 사용합니다.
    */
    private Product mapProduct(ResultSet rs, int rowNum) throws SQLException {

        return Product.builder()
                .seq(rs.getLong("seq"))
                .sellerSeq(rs.getLong("seller_seq"))
                .categorySeq(rs.getLong("category_seq"))
                .productName(rs.getString("product_name"))
                .price(rs.getInt("price"))
                .content(rs.getString("content"))
                .saleStatus(rs.getString("sale_status"))
                .approvalStatus(rs.getString("approval_status"))
                .hideYn(rs.getString("hide_yn"))
                .viewCount(rs.getLong("view_count"))
                .avgRating(rs.getDouble("avg_rating"))
                .reviewCount(rs.getLong("review_count"))
                .salesCount(rs.getLong("sales_count"))
                .createdDate(
                        rs.getTimestamp("created_date") != null
                                ? rs.getTimestamp("created_date").toLocalDateTime()
                                : null
                )
                .updatedDate(
                        rs.getTimestamp("updated_date") != null
                                ? rs.getTimestamp("updated_date").toLocalDateTime()
                                : null
                )
                .status(rs.getString("status"))
                .build();
    }


    /*
        상품 상세 DTO 매핑 메서드

        SQL 조회 결과를 ProductDto에 담습니다.
    */
    private ProductDto mapProductDetail(ResultSet rs, int rowNum) throws SQLException {

        ProductDto dto = new ProductDto();

        dto.setSeq(rs.getLong("seq"));
        dto.setSellerSeq(rs.getLong("seller_seq"));
        dto.setCategorySeq(rs.getLong("category_seq"));

        dto.setProductName(rs.getString("product_name"));
        dto.setPrice(rs.getInt("price"));
        dto.setContent(rs.getString("content"));

        dto.setSaleStatus(rs.getString("sale_status"));
        dto.setApprovalStatus(rs.getString("approval_status"));
        dto.setHideYn(rs.getString("hide_yn"));

        dto.setViewCount(rs.getLong("view_count"));
        dto.setAvgRating(rs.getDouble("avg_rating"));
        dto.setReviewCount(rs.getLong("review_count"));
        dto.setSalesCount(rs.getLong("sales_count"));

        dto.setCreatedDate(
                rs.getTimestamp("created_date") != null
                        ? rs.getTimestamp("created_date").toLocalDateTime()
                        : null
        );

        dto.setUpdatedDate(
                rs.getTimestamp("updated_date") != null
                        ? rs.getTimestamp("updated_date").toLocalDateTime()
                        : null
        );

        dto.setStatus(rs.getString("status"));
        dto.setThumbnailUrl(rs.getString("thumbnail_url"));
        dto.setImage(rs.getString("thumbnail_url"));

        return dto;
    }


    /*
        상품 이미지 DTO 매핑 메서드
    */
    private ProductImageDto mapProductImage(ResultSet rs, int rowNum) throws SQLException {

        ProductImageDto dto = new ProductImageDto();

        dto.setSeq(rs.getLong("seq"));
        dto.setProductSeq(rs.getLong("product_seq"));
        dto.setImageUrl(rs.getString("image_url"));
        dto.setPublicId(rs.getString("public_id"));
        dto.setThumbnailYn(rs.getString("thumbnail_yn"));
        dto.setImageOrder(rs.getInt("image_order"));
        dto.setStatus(rs.getString("status"));

        return dto;
    }


    /*
        상품 옵션 DTO 매핑 메서드
    */
    private ProductOptionDto mapProductOption(ResultSet rs, int rowNum) throws SQLException {

        ProductOptionDto dto = new ProductOptionDto();

        dto.setSeq(rs.getLong("seq"));
        dto.setProductSeq(rs.getLong("product_seq"));

        dto.setColor(rs.getString("color"));
        dto.setOptionsSize(rs.getString("options_size"));
        dto.setVolumeWeight(rs.getString("volume_weight"));
        dto.setTaste(rs.getString("taste"));
        dto.setStorageType(rs.getString("storage_type"));
        dto.setScentIngredient(rs.getString("scent_ingredient"));
        dto.setVoltage(rs.getString("voltage"));
        dto.setQuantitySet(rs.getString("quantity_set"));
        dto.setSizeSpec(rs.getString("size_spec"));
        dto.setStorageCapacity(rs.getString("storage_capacity"));
        dto.setMemory(rs.getString("memory"));
        dto.setSwitchAxis(rs.getString("switch_axis"));
        dto.setConnectionType(rs.getString("connection_type"));
        dto.setWearableSpec(rs.getString("wearable_spec"));
        dto.setMaterialType(rs.getString("material_type"));
        dto.setOptionsType(rs.getString("options_type"));

        dto.setStock(rs.getInt("stock"));
        dto.setSafetyStock(rs.getInt("safety_stock"));
        dto.setAdditionalPrice(rs.getInt("additional_price"));

        dto.setOptionName(makeOptionName(dto));

        return dto;
    }


    /*
        옵션명 조합 메서드

        값이 있는 옵션 컬럼만 골라서 하나의 문자열로 만듭니다.

        예:
        블랙 / M (+1000원)
    */
    private String makeOptionName(ProductOptionDto dto) {

        StringBuilder sb = new StringBuilder();

        appendOption(sb, dto.getColor());
        appendOption(sb, dto.getOptionsSize());
        appendOption(sb, dto.getVolumeWeight());
        appendOption(sb, dto.getTaste());
        appendOption(sb, dto.getStorageType());
        appendOption(sb, dto.getScentIngredient());
        appendOption(sb, dto.getVoltage());
        appendOption(sb, dto.getQuantitySet());
        appendOption(sb, dto.getSizeSpec());
        appendOption(sb, dto.getStorageCapacity());
        appendOption(sb, dto.getMemory());
        appendOption(sb, dto.getSwitchAxis());
        appendOption(sb, dto.getConnectionType());
        appendOption(sb, dto.getWearableSpec());
        appendOption(sb, dto.getMaterialType());
        appendOption(sb, dto.getOptionsType());

        if (sb.length() == 0) {
            sb.append("기본 옵션");
        }

        if (dto.getAdditionalPrice() != null && dto.getAdditionalPrice() > 0) {
            sb.append(" (+").append(dto.getAdditionalPrice()).append("원)");
        }

        return sb.toString();
    }


    /*
        옵션값 추가 메서드
    */
    private void appendOption(StringBuilder sb, String value) {

        if (value == null || value.isBlank()) {
            return;
        }

        if (sb.length() > 0) {
            sb.append(" / ");
        }

        sb.append(value);
    }
}