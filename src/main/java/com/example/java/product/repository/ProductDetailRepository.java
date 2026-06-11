package com.example.java.product.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

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
    ProductDetailRepository

    상품 등록, 조회, 수정, 삭제(CRUD) 및 상세 화면에 필요한 DB 작업을 담당합니다.
*/
@Repository
@RequiredArgsConstructor
public class ProductDetailRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @PersistenceContext
    private EntityManager entityManager;

    /*
        상품 저장 / 수정
    */
    public Product save(Product product) {
        Product saved;
        if (product.getSeq() == null) {
            entityManager.persist(product);
            saved = product;
        } else {
            saved = entityManager.merge(product);
        }
        eventPublisher.publishEvent(new com.example.java.product.event.ProductUpdatedEvent(saved.getSeq()));
        return saved;
    }

    /*
        상품 번호로 상품 엔티티 조회
    */
    public Optional<Product> findById(Long seq) {

        return Optional.ofNullable(entityManager.find(Product.class, seq));
    }

    /*
        상품 상세 기본 정보 조회
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

    /*
	    찜 여부 확인
	*/
    public boolean existsWish(Long productSeq, Long memberSeq) {

        System.out.println("existsWish productSeq = " + productSeq);
        System.out.println("existsWish memberSeq = " + memberSeq);

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

        int result = jdbcTemplate.update(sql, params);
        if (result > 0) {
            eventPublisher.publishEvent(new com.example.java.product.event.ProductUpdatedEvent(productSeq));
        }
        return result;
    }

    /*
        상품 리뷰 통계 갱신
    */
    public void updateProductReviewStats(Long productSeq) {

        String sql = """
                UPDATE product p
                SET
                    avg_rating = (
                        SELECT NVL(ROUND(AVG(r.rating), 1), 0)
                        FROM review r
                        WHERE r.product_seq = p.seq
                          AND r.status = 'NORMAL'
                    ),
                    review_count = (
                        SELECT COUNT(*)
                        FROM review r
                        WHERE r.product_seq = p.seq
                          AND r.status = 'NORMAL'
                    ),
                    updated_date = SYSDATE
                WHERE p.seq = :productSeq
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("productSeq", productSeq);

        jdbcTemplate.update(sql, params);
        eventPublisher.publishEvent(new com.example.java.product.event.ProductUpdatedEvent(productSeq));
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
