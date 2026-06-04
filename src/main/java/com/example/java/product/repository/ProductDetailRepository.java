package com.example.java.product.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.java.product.dto.ProductDetailDto;
import com.example.java.product.dto.ProductDetailDto.ProductImageDto;
import com.example.java.product.dto.ProductDetailDto.ProductOptionDto;

import lombok.RequiredArgsConstructor;

/*
    Repository 계층

    DB에 직접 접근하는 클래스입니다.

    흐름:
    Controller → Service → Repository → DB

    여기서는 JdbcTemplate을 사용해서 SQL을 직접 작성합니다.
*/
@Repository
@RequiredArgsConstructor
public class ProductDetailRepository {

    /*
        JdbcTemplate

        SQL을 실행하기 위해 사용하는 Spring의 DB 도구입니다.
        SELECT, INSERT, UPDATE, DELETE SQL을 직접 실행할 수 있습니다.
    */
    private final JdbcTemplate jdbcTemplate;


    /*
        상품 상세 기본 정보 조회

        상품 상세 화면 상단에 필요한 데이터를 조회합니다.

        정상 상품이면서 관리자 승인 완료된 상품만 조회됩니다.
    */
    public Optional<ProductDetailDto> findProductDetail(Long productSeq) {

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
                WHERE p.seq = ?
                  AND p.status = 'NORMAL'
                  AND p.approval_status = 'APPROVED'
                  AND p.hide_yn = 'N'
                """;

        /*
            query()
            - SELECT 결과가 여러 줄일 수 있을 때 사용합니다.

            this::mapProductDetail
            - ResultSet 결과를 ProductDto로 변환하는 메서드입니다.

            productSeq
            - SQL의 ? 자리에 들어가는 값입니다.
        */
        List<ProductDetailDto> list = jdbcTemplate.query(sql, this::mapProductDetail, productSeq);

        /*
            조회 결과가 없으면 빈 Optional 반환
        */
        if (list.isEmpty()) {
            return Optional.empty();
        }

        /*
            상품번호는 PK이기 때문에 결과는 1개만 있다고 보면 됩니다.
        */
        return Optional.of(list.get(0));
    }
    
    /*
	    상품 목록 개수 조회
	
	    페이징 처리를 위해 전체 상품 개수를 조회합니다.
	
	    정상 상품이면서 관리자 승인 완료된 상품만 개수에 포함됩니다.
	    숨김 처리된 상품은 제외됩니다.
	*/
	public int countProducts() {
	
	    String sql = """
	            SELECT COUNT(*)
	            FROM product
	            WHERE status = 'NORMAL'
	              AND approval_status = 'APPROVED'
	              AND hide_yn = 'N'
	            """;
	
	    /*
	        queryForObject()
	        - 결과가 하나만 나오는 SQL에 사용합니다.
	        - 여기서는 COUNT(*) 값 하나만 가져옵니다.
	    */
	    Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
	
	    /*
	        count가 null이면 0으로 반환합니다.
	    */
	    return count == null ? 0 : count;
	}
	
	
	/*
	    상품 목록 페이징 조회
	
	    상품 목록 화면에서 사용할 상품 데이터를 조회합니다.
	
	    page, size 값을 이용해서 필요한 구간의 상품만 가져옵니다.
	
	    정상 상품이면서 관리자 승인 완료된 상품만 조회됩니다.
	    숨김 처리된 상품은 제외됩니다.
	
	    OFFSET
	    - 앞에서 몇 개의 데이터를 건너뛸지 지정합니다.
	
	    FETCH NEXT
	    - 몇 개의 데이터를 가져올지 지정합니다.
	*/
	public List<ProductDetailDto> findProductsByPaging(int offset, int size) {
	
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
	            OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
	            """;
	
	    /*
	        query()
	        - SELECT 결과가 여러 줄일 수 있을 때 사용합니다.
	
	        this::mapProductDetail
	        - ResultSet 결과를 ProductDto로 변환하는 메서드입니다.
	
	        offset
	        - 앞에서 건너뛸 상품 개수입니다.
	
	        size
	        - 가져올 상품 개수입니다.
	    */
	    return jdbcTemplate.query(sql, this::mapProductDetail, offset, size);
	}


    /*
        상품 조회수 조회

        상품 상세 전체 정보가 아니라
        view_count 값만 따로 조회할 때 사용합니다.

        정상 상품이면서 관리자 승인 완료된 상품만 조회됩니다.

        NVL(view_count, 0)
        - Oracle 함수입니다.
        - view_count가 null이면 0으로 바꿔서 반환합니다.
    */
    public Optional<Integer> findViewCountByProductSeq(Long productSeq) {

        String sql = """
                SELECT NVL(view_count, 0)
                FROM product
                WHERE seq = ?
                  AND status = 'NORMAL'
                  AND approval_status = 'APPROVED'
                  AND hide_yn = 'N'
                """;

        /*
            query()
            - 조회 결과가 없을 수도 있으므로 List로 먼저 받습니다.
            - 상품이 없으면 빈 Optional을 반환합니다.
        */
        List<Integer> list = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getInt(1),
                productSeq
        );

        /*
            조회 결과가 없으면 빈 Optional 반환
        */
        if (list.isEmpty()) {
            return Optional.empty();
        }

        /*
            조회된 조회수 반환
        */
        return Optional.of(list.get(0));
    }


    /*
        상품 이미지 목록 조회

        product_image 테이블에서 해당 상품에 연결된 이미지를 조회합니다.

        대표 이미지뿐 아니라 일반 이미지도 함께 가져옵니다.
        추후 상세 이미지 영역이나 썸네일 목록에 사용할 수 있습니다.
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
                WHERE product_seq = ?
                  AND status = 'NORMAL'
                ORDER BY thumbnail_yn DESC, image_order ASC, seq ASC
                """;

        return jdbcTemplate.query(sql, this::mapProductImage, productSeq);
    }


    /*
        상품 옵션 목록 조회

        options 테이블에서 해당 상품의 옵션을 조회합니다.

        현재는 stock > 0 조건을 넣어서
        재고가 있는 옵션만 화면에 출력되게 했습니다.

        품절 옵션까지 보여주고 싶으면
        AND stock > 0 조건을 제거하면 됩니다.
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
                WHERE product_seq = ?
                  AND stock > 0
                ORDER BY seq ASC
                """;

        return jdbcTemplate.query(sql, this::mapProductOption, productSeq);
    }


    /*
        찜 여부 확인

        로그인한 회원이 현재 상품을 찜했는지 확인합니다.

        product_wish 테이블에는 product_seq가 직접 있는 것이 아니라
        options_seq가 들어가 있으므로,
        options 테이블을 이용해서 현재 상품의 옵션번호들을 먼저 찾습니다.

        그 옵션번호들 중 회원이 찜한 데이터가 있으면 true입니다.
    */
    public boolean existsWish(Long productSeq, Long memberSeq) {

        String sql = """
                SELECT COUNT(*)
                FROM product_wish
                WHERE options_seq IN (
                    SELECT seq
                    FROM options
                    WHERE product_seq = ?
                )
                AND member_seq = ?
                AND status = 0
                """;

        /*
            queryForObject()
            - 결과가 하나만 나오는 SQL에 사용합니다.
            - 여기서는 COUNT(*) 값 하나만 가져옵니다.
        */
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, productSeq, memberSeq);

        /*
            count가 1 이상이면 이미 찜한 상태입니다.
        */
        return count != null && count > 0;
    }


    /*
        조회수 증가

        상품 상세 화면에 들어올 때마다 조회수를 1 증가시킵니다.

        NVL(view_count, 0)
        - Oracle 함수입니다.
        - view_count가 null이면 0으로 바꿔서 계산합니다.
    */
    public void increaseViewCount(Long productSeq) {

        String sql = """
                UPDATE product
                SET view_count = NVL(view_count, 0) + 1
                WHERE seq = ?
                """;

        jdbcTemplate.update(sql, productSeq);
    }
    
    
    /*
	    상품 삭제
	
	    실제 DB에서 상품 데이터를 DELETE로 지우는 것이 아니라
	    status 값을 DELETED로 변경하고 hide_yn 값을 Y로 변경합니다.
	
	    이렇게 처리하면 기존 상품 상세 조회 조건에서 제외됩니다.
	
	    기존 상품 상세 조회 조건:
	    - status = 'NORMAL'
	    - hide_yn = 'N'
	
	    updated_date는 삭제 시점으로 변경합니다.
	*/
	public int deleteProduct(Long productSeq) {
	
	    String sql = """
	            UPDATE product
	            SET status = 'DELETED',
	                hide_yn = 'Y',
	                updated_date = SYSDATE
	            WHERE seq = ?
	              AND status = 'NORMAL'
	            """;
	
	    /*
	        update()
	        - INSERT, UPDATE, DELETE SQL에 사용합니다.
	        - 반환값은 영향을 받은 행의 개수입니다.
	        - 1이면 삭제 성공, 0이면 삭제할 상품이 없다는 뜻입니다.
	    */
	    return jdbcTemplate.update(sql, productSeq);
	}


    /*
        상품 상세 정보 매핑

        DB에서 조회한 한 줄의 결과를 ProductDto 객체에 담습니다.
    */
    private ProductDetailDto mapProductDetail(ResultSet rs, int rowNum) throws SQLException {

        ProductDetailDto dto = new ProductDetailDto();

        dto.setSeq(rs.getLong("seq"));
        dto.setSellerSeq(rs.getLong("seller_seq"));
        dto.setCategorySeq(rs.getLong("category_seq"));

        dto.setProductName(rs.getString("product_name"));
        dto.setPrice(rs.getInt("price"));
        dto.setContent(rs.getString("content"));

        dto.setSaleStatus(rs.getString("sale_status"));
        dto.setApprovalStatus(rs.getString("approval_status"));
        dto.setHideYn(rs.getString("hide_yn"));

        dto.setViewCount(rs.getInt("view_count"));
        dto.setAvgRating(rs.getDouble("avg_rating"));
        dto.setReviewCount(rs.getInt("review_count"));
        dto.setSalesCount(rs.getInt("sales_count"));

        dto.setCreatedDate(String.valueOf(rs.getDate("created_date")));

        if (rs.getDate("updated_date") != null) {
            dto.setUpdatedDate(String.valueOf(rs.getDate("updated_date")));
        }

        dto.setStatus(rs.getString("status"));
        dto.setThumbnailUrl(rs.getString("thumbnail_url"));

        return dto;
    }


    /*
        상품 이미지 정보 매핑

        product_image 테이블의 조회 결과를 ProductImageDto에 담습니다.
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
        상품 옵션 정보 매핑

        options 테이블의 조회 결과를 ProductOptionDto에 담습니다.
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

        options 테이블은 상품 종류에 따라 사용하는 컬럼이 다릅니다.
        그래서 값이 있는 컬럼만 골라서 하나의 문자열로 합칩니다.
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

        /*
            옵션 컬럼에 아무 값도 없으면 기본 옵션으로 표시합니다.
        */
        if (sb.length() == 0) {
            sb.append("기본 옵션");
        }

        /*
            추가금액이 있으면 옵션명 뒤에 붙입니다.
        */
        if (dto.getAdditionalPrice() != null && dto.getAdditionalPrice() > 0) {
            sb.append(" (+").append(dto.getAdditionalPrice()).append("원)");
        }

        return sb.toString();
    }


    /*
        옵션값 추가 메서드

        값이 null이거나 빈 문자열이면 추가하지 않습니다.
        값이 있으면 기존 문자열 뒤에 " / "로 구분해서 붙입니다.
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