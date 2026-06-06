package com.example.java.product.repository;

import static com.example.java.product.entity.QProduct.product;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

/*
    ProductListRepository

    상품 목록 조회, 필터링, 검색 등 읽기 전용 목록 관련 DB 작업을 담당합니다.
*/
@Repository
@RequiredArgsConstructor
public class ProductListRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JPAQueryFactory queryFactory;

    /*
        상품 필터 검색 (QueryDSL 사용)
    */
    public Page<Product> findWithFilters(
            List<Long> categorySeqs,
            String keyword,
            Integer minPrice,
            Integer maxPrice,
            Double minRating,
            String saleStatus,
            Pageable pageable) {

        BooleanExpression condition = product.hideYn.eq("N")
                .and(product.saleStatus.ne("STOPPED"))
                .and(product.status.eq("NORMAL"));

        if (categorySeqs != null && !categorySeqs.isEmpty()) {
            condition = condition.and(product.categorySeq.in(categorySeqs));
        }

        if (keyword != null && !keyword.isBlank()) {
            String cleanKeyword = keyword.replace(" ", "").toLowerCase();
            condition = condition.and(
                Expressions.stringTemplate("REPLACE(LOWER({0}), ' ', '')", product.productName)
                           .contains(cleanKeyword)
            );
        }

        if (minPrice != null) {
            condition = condition.and(product.price.goe(minPrice));
        }

        if (maxPrice != null) {
            condition = condition.and(product.price.loe(maxPrice));
        }

        if (minRating != null) {
            condition = condition.and(product.avgRating.goe(minRating));
        }

        if (saleStatus != null && !saleStatus.isBlank()) {
            condition = condition.and(product.saleStatus.eq(saleStatus));
        }

        List<Product> products = queryFactory
                .selectFrom(product)
                .where(condition)
                .orderBy(product.seq.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = queryFactory
                .select(product.count())
                .from(product)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(
                products,
                pageable,
                totalCount == null ? 0 : totalCount
        );
    }

    /*
        상품 목록 전체 개수 조회
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
