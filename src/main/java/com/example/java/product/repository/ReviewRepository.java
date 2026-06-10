package com.example.java.product.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.java.product.dto.ReviewImageDto;
import com.example.java.product.dto.ReviewResponseDto;
import com.example.java.product.dto.PurchasedOrderItemDto;
import com.example.java.product.dto.ReviewCreateRequestDto;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ReviewRepository {

    // 이름 기반 파라미터를 사용할 수 있는 JDBC 객체
    // 예: :productSeq, :reviewSeq 같은 방식으로 SQL에 값을 넣을 수 있음
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    // 전체 정상 리뷰 목록을 무한스크롤 방식으로 조회
    public List<ReviewResponseDto> findAllReviewsByScroll(Long lastReviewSeq, int limit) {

        // 처음 조회일 때는 lastReviewSeq가 null
        // 다음 조회부터는 마지막으로 본 리뷰 seq보다 작은 리뷰만 조회
    	String sql = """
    	        SELECT
    	            r.seq,
    	            r.product_seq,
    	            r.member_seq,
    	            m.nickname AS nickname,
    	            r.order_item_seq,
    	            r.rating,
    	            r.content,
    	            r.created_date,
    	            r.updated_date,
    	            r.status
    	        FROM review r
    	        LEFT JOIN member m
    	            ON r.member_seq = m.seq
    	        WHERE r.status = 'NORMAL'
    	          AND (:lastReviewSeq IS NULL OR r.seq < :lastReviewSeq)
    	        ORDER BY r.seq DESC
    	        FETCH NEXT :limit ROWS ONLY
    	        """;

        // HashMap은 null 값을 넣을 수 있어서 lastReviewSeq가 null이어도 안전함
        Map<String, Object> params = new HashMap<>();
        params.put("lastReviewSeq", lastReviewSeq);
        params.put("limit", limit);

        return namedParameterJdbcTemplate.query(sql, params, reviewRowMapper());
    }

    // 특정 상품의 정상 리뷰 목록을 무한스크롤 방식으로 조회
    public List<ReviewResponseDto> findProductReviewsByScroll(Long productSeq, Long lastReviewSeq, int limit) {

        // 처음 조회일 때는 lastReviewSeq가 null
        // 다음 조회부터는 마지막으로 본 리뷰 seq보다 작은 리뷰만 조회
    	String sql = """
    	        SELECT
    	            r.seq,
    	            r.product_seq,
    	            r.member_seq,
    	            m.nickname AS nickname,
    	            r.order_item_seq,
    	            r.rating,
    	            r.content,
    	            r.created_date,
    	            r.updated_date,
    	            r.status
    	        FROM review r
    	        LEFT JOIN member m
    	            ON r.member_seq = m.seq
    	        WHERE r.product_seq = :productSeq
    	          AND r.status = 'NORMAL'
    	          AND (:lastReviewSeq IS NULL OR r.seq < :lastReviewSeq)
    	        ORDER BY r.seq DESC
    	        FETCH NEXT :limit ROWS ONLY
    	        """;

        // HashMap은 null 값을 넣을 수 있어서 lastReviewSeq가 null이어도 안전함
        Map<String, Object> params = new HashMap<>();
        params.put("productSeq", productSeq);
        params.put("lastReviewSeq", lastReviewSeq);
        params.put("limit", limit);

        return namedParameterJdbcTemplate.query(sql, params, reviewRowMapper());
    }

    // 리뷰 번호로 상품 번호 조회
    //
    // 리뷰를 수정하거나 삭제한 뒤
    // 어떤 상품의 평균 별점과 리뷰 수를 다시 계산해야 하는지 알기 위해 사용합니다.
    //
    // 예:
    // 리뷰번호 10번이 상품번호 401번에 달린 리뷰라면
    // 401을 반환합니다.
    public Long findProductSeqByReviewSeq(Long reviewSeq) {

        String sql = """
                SELECT product_seq
                FROM review
                WHERE seq = :reviewSeq
                """;

        Map<String, Object> params = Map.of("reviewSeq", reviewSeq);

        List<Long> list = namedParameterJdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> rs.getLong("product_seq")
        );

        // 리뷰가 없으면 null 반환
        if (list.isEmpty()) {
            return null;
        }

        // 조회된 상품번호 반환
        return list.get(0);
    }

    // 여러 리뷰 번호에 연결된 리뷰 이미지 목록을 한 번에 조회
    // 리뷰 목록 조회 시 리뷰마다 이미지 쿼리를 반복하지 않기 위해 사용
    public List<ReviewImageDto> findImagesByReviewSeqs(List<Long> reviewSeqs) {

        if (reviewSeqs == null || reviewSeqs.isEmpty()) {
            return Collections.emptyList();
        }

        /*
            ERD 기준 review_image 컬럼

            seq
            review_seq
            image_url
            public_id
            image_order
            file_type
            file_size
            status

            위 컬럼명에 맞춰 조회합니다.
        */
        String sql = """
                SELECT
                    seq,
                    review_seq,
                    image_url,
                    public_id,
                    image_order,
                    file_type,
                    file_size,
                    status
                FROM review_image
                WHERE review_seq IN (:reviewSeqs)
                  AND status = 'NORMAL'
                ORDER BY review_seq ASC, image_order ASC, seq ASC
                """;

        Map<String, Object> params = Map.of("reviewSeqs", reviewSeqs);

        return namedParameterJdbcTemplate.query(sql, params, reviewImageRowMapper());
    }

    // 상품이 실제로 존재하는지 확인
    // 삭제된 상품은 조회 대상에서 제외
    public boolean existsProduct(Long productSeq) {

        String sql = """
                SELECT COUNT(*)
                FROM product
                WHERE seq = :productSeq
                  AND status = 'NORMAL'
                """;

        Map<String, Object> params = Map.of("productSeq", productSeq);

        Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);

        return count != null && count > 0;
    }

    // review 테이블 조회 결과를 ReviewResponseDto로 변환
    private RowMapper<ReviewResponseDto> reviewRowMapper() {

        return new RowMapper<ReviewResponseDto>() {
            @Override
            public ReviewResponseDto mapRow(ResultSet rs, int rowNum) throws SQLException {

                return ReviewResponseDto.builder()
                        .seq(rs.getLong("seq"))
                        .productSeq(rs.getLong("product_seq"))
                        .memberSeq(rs.getLong("member_seq"))
                        .nickname(rs.getString("nickname"))
                        .orderItemSeq(getNullableLong(rs, "order_item_seq"))
                        .rating(rs.getInt("rating"))
                        .content(rs.getString("content"))
                        .createdDate(toLocalDateTime(rs.getTimestamp("created_date")))
                        .updatedDate(toLocalDateTime(rs.getTimestamp("updated_date")))
                        .status(rs.getString("status"))
                        .images(Collections.emptyList())
                        .build();
            }
        };
    }

    // review_image 테이블 조회 결과를 ReviewImageDto로 변환
    private RowMapper<ReviewImageDto> reviewImageRowMapper() {

        return new RowMapper<ReviewImageDto>() {
            @Override
            public ReviewImageDto mapRow(ResultSet rs, int rowNum) throws SQLException {

                return ReviewImageDto.builder()
                        .seq(rs.getLong("seq"))
                        .reviewSeq(rs.getLong("review_seq"))
                        .imageUrl(rs.getString("image_url"))
                        .publicId(rs.getString("public_id"))
                        .imageOrder(rs.getInt("image_order"))
                        .fileType(rs.getString("file_type"))
                        .fileSize(getNullableLong(rs, "file_size"))
                        .status(rs.getString("status"))
                        .build();
            }
        };
    }

    // DB의 Timestamp 값을 Java LocalDateTime으로 변환
    private LocalDateTime toLocalDateTime(Timestamp timestamp) {

        if (timestamp == null) {
            return null;
        }

        return timestamp.toLocalDateTime();
    }

    // DB 컬럼 값이 null이면 Java에서도 null로 변환
    private Long getNullableLong(ResultSet rs, String columnName) throws SQLException {

        long value = rs.getLong(columnName);

        if (rs.wasNull()) {
            return null;
        }

        return value;
    }

    /*
        해당 상품을 실제로 구매했는지 확인

        입력받은 orderItemSeq가
        현재 회원의 주문상품인지,
        현재 상품의 주문상품인지,
        배송완료 상태인지 확인합니다.

        item_status = 3 : 배송완료
    */
    public boolean existsPurchasedOrderItem(Long productSeq, Long memberSeq, Long orderItemSeq) {

        String sql = """
                SELECT COUNT(*)
                FROM order_item oi
                INNER JOIN orders o
                    ON oi.order_seq = o.seq
                INNER JOIN options opt
                    ON oi.options_seq = opt.seq
                WHERE oi.seq = :orderItemSeq
                  AND o.member_seq = :memberSeq
                  AND opt.product_seq = :productSeq
                  AND oi.item_status = 3
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("productSeq", productSeq);
        params.put("memberSeq", memberSeq);
        params.put("orderItemSeq", orderItemSeq);

        Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);

        return count != null && count > 0;
    }

    /*
        해당 상품을 구매 완료했는지 확인

        리뷰 등록 버튼을 클릭했을 때
        입력폼을 보여주기 전에 먼저 확인하기 위해 사용합니다.

        이 메서드는 orderItemSeq를 아직 입력받기 전에도 사용할 수 있습니다.

        현재 회원이 해당 상품을 구매했고,
        주문상품 상태가 배송완료인 경우 true를 반환합니다.

        item_status = 3 : 배송완료
    */
    public boolean existsPurchasedProduct(Long productSeq, Long memberSeq) {

        String sql = """
                SELECT COUNT(*)
                FROM order_item oi
                INNER JOIN orders o
                    ON oi.order_seq = o.seq
                INNER JOIN options opt
                    ON oi.options_seq = opt.seq
                WHERE o.member_seq = :memberSeq
                  AND opt.product_seq = :productSeq
                  AND oi.item_status = 3
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("productSeq", productSeq);
        params.put("memberSeq", memberSeq);

        Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);

        return count != null && count > 0;
    }

    /*
	    이미 리뷰를 작성했는지 확인
	
	    같은 회원이 같은 상품에 대해
	    NORMAL 상태의 리뷰를 이미 작성했으면 true를 반환합니다.
	
	    주의:
	    삭제된 리뷰는 status = 'DELETED' 상태이므로
	    다시 리뷰를 작성할 수 있어야 합니다.
	
	    그래서 status = 'NORMAL'인 리뷰만 중복으로 판단합니다.
	*/
	public boolean existsReviewByProductSeqAndMemberSeq(Long productSeq, Long memberSeq) {
	
	    String sql = """
	            SELECT COUNT(*)
	            FROM review
	            WHERE product_seq = :productSeq
	              AND member_seq = :memberSeq
	              AND status = 'NORMAL'
	            """;
	
	    Map<String, Object> params = new HashMap<>();
	    params.put("productSeq", productSeq);
	    params.put("memberSeq", memberSeq);
	
	    Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
	
	    return count != null && count > 0;
	}

    // 리뷰 등록
    //
    // 상품번호, 회원번호, 주문상품번호, 별점, 리뷰 내용을 받아
    // review 테이블에 새로운 리뷰를 등록합니다.
    //
    // order_item_seq는 리뷰 작성 가능한 주문상품 번호가 들어가야 합니다.
    //
    // status는 정상 리뷰를 의미하는 NORMAL로 저장합니다.
    public Long insertReview(ReviewCreateRequestDto dto) {

        String seqSql = """
                SELECT review_seq.NEXTVAL
                FROM dual
                """;

        Long reviewSeq = namedParameterJdbcTemplate.queryForObject(
                seqSql,
                new HashMap<>(),
                Long.class
        );

        String sql = """
                INSERT INTO review (
                    seq,
                    product_seq,
                    member_seq,
                    order_item_seq,
                    rating,
                    content,
                    created_date,
                    updated_date,
                    status
                ) VALUES (
                    :reviewSeq,
                    :productSeq,
                    :memberSeq,
                    :orderItemSeq,
                    :rating,
                    :content,
                    SYSDATE,
                    NULL,
                    'NORMAL'
                )
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("reviewSeq", reviewSeq);
        params.put("productSeq", dto.getProductSeq());
        params.put("memberSeq", dto.getMemberSeq());
        params.put("orderItemSeq", dto.getOrderItemSeq());
        params.put("rating", dto.getRating());
        params.put("content", dto.getContent());

        namedParameterJdbcTemplate.update(sql, params);

        return reviewSeq;
    }

    /*
        리뷰 이미지 등록

        Cloudinary에 업로드된 이미지 URL을
        review_image 테이블에 저장합니다.

        ERD 기준:
        - image_url은 NOT NULL
        - public_id, file_type, file_size는 NULL 가능
        - image_order 기본값은 1이지만, 여러 장 순서를 위해 직접 넣습니다.
        - status는 NORMAL로 저장합니다.
    */
    public void insertReviewImage(Long reviewSeq, String imageUrl, int imageOrder) {

        String sql = """
                INSERT INTO review_image (
                    seq,
                    review_seq,
                    image_url,
                    image_order,
                    created_date,
                    status
                ) VALUES (
                    review_image_seq.NEXTVAL,
                    :reviewSeq,
                    :imageUrl,
                    :imageOrder,
                    SYSDATE,
                    'NORMAL'
                )
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("reviewSeq", reviewSeq);
        params.put("imageUrl", imageUrl);
        params.put("imageOrder", imageOrder);

        namedParameterJdbcTemplate.update(sql, params);
    }

    /*
        리뷰 수정

        본인이 작성한 정상 리뷰만 수정할 수 있습니다.

        product_seq와 member_seq를 같이 조건에 넣어서
        다른 상품의 리뷰나 다른 회원의 리뷰가 수정되지 않도록 막습니다.
    */
    public int updateReview(Long reviewSeq, Long productSeq, Long memberSeq, Integer rating, String content) {

        String sql = """
                UPDATE review
                SET rating = :rating,
                    content = :content,
                    updated_date = SYSDATE
                WHERE seq = :reviewSeq
                  AND product_seq = :productSeq
                  AND member_seq = :memberSeq
                  AND status = 'NORMAL'
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("reviewSeq", reviewSeq);
        params.put("productSeq", productSeq);
        params.put("memberSeq", memberSeq);
        params.put("rating", rating);
        params.put("content", content);

        return namedParameterJdbcTemplate.update(sql, params);
    }

    /*
        리뷰 작성 가능한 주문상품 목록 조회

        현재 회원이 해당 상품을 구매했고,
        주문상품 상태가 배송완료인 주문상품만 조회합니다.

        화면에서는 주문상품번호를 직접 보여주지 않고,
        상품명과 옵션명을 보여주기 위해 사용합니다.

        item_status = 3 : 배송완료

        주의:
        order_item 테이블에 product_name이 없고,
        options 테이블에도 option_name이 없는 구조라서
        product 테이블과 options 테이블을 조인해서 상품명과 옵션명을 만듭니다.
    */
    public List<PurchasedOrderItemDto> findPurchasedOrderItems(Long productSeq, Long memberSeq) {

        String sql = """
                SELECT
                    oi.seq AS order_item_seq,
                    p.product_name AS product_name,
                    opt.color,
                    opt.options_size,
                    opt.volume_weight,
                    opt.taste,
                    opt.storage_type,
                    opt.scent_ingredient,
                    opt.voltage,
                    opt.quantity_set,
                    opt.size_spec,
                    opt.storage_capacity,
                    opt.memory,
                    opt.switch_axis,
                    opt.connection_type,
                    opt.wearable_spec,
                    opt.material_type,
                    opt.options_type,
                    opt.additional_price
                FROM order_item oi
                INNER JOIN orders o
                    ON oi.order_seq = o.seq
                INNER JOIN options opt
                    ON oi.options_seq = opt.seq
                INNER JOIN product p
                    ON opt.product_seq = p.seq
                WHERE o.member_seq = :memberSeq
                  AND opt.product_seq = :productSeq
                  AND oi.item_status = 3
                ORDER BY oi.seq DESC
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("productSeq", productSeq);
        params.put("memberSeq", memberSeq);

        return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> {
            PurchasedOrderItemDto dto = new PurchasedOrderItemDto();

            dto.setOrderItemSeq(rs.getLong("order_item_seq"));
            dto.setProductName(rs.getString("product_name"));
            dto.setOptionName(makeOptionName(rs));

            return dto;
        });
    }

    /*
        옵션명 생성

        options 테이블에는 option_name 컬럼이 없기 때문에
        실제 옵션 컬럼들 중 값이 있는 것만 모아서 화면용 옵션명을 만듭니다.

        예:
        블랙 / M / 저소음축 / +1000원
    */
    private String makeOptionName(ResultSet rs) throws SQLException {

        StringBuilder optionName = new StringBuilder();

        appendOption(optionName, rs.getString("color"));
        appendOption(optionName, rs.getString("options_size"));
        appendOption(optionName, rs.getString("volume_weight"));
        appendOption(optionName, rs.getString("taste"));
        appendOption(optionName, rs.getString("storage_type"));
        appendOption(optionName, rs.getString("scent_ingredient"));
        appendOption(optionName, rs.getString("voltage"));
        appendOption(optionName, rs.getString("quantity_set"));
        appendOption(optionName, rs.getString("size_spec"));
        appendOption(optionName, rs.getString("storage_capacity"));
        appendOption(optionName, rs.getString("memory"));
        appendOption(optionName, rs.getString("switch_axis"));
        appendOption(optionName, rs.getString("connection_type"));
        appendOption(optionName, rs.getString("wearable_spec"));
        appendOption(optionName, rs.getString("material_type"));
        appendOption(optionName, rs.getString("options_type"));

        int additionalPrice = rs.getInt("additional_price");

        if (!rs.wasNull() && additionalPrice > 0) {
            appendOption(optionName, "+" + additionalPrice + "원");
        }

        if (optionName.length() == 0) {
            return "기본 옵션";
        }

        return optionName.toString();
    }

    /*
        옵션 문자열 추가

        값이 비어 있지 않을 때만
        " / " 구분자로 이어 붙입니다.
    */
    private void appendOption(StringBuilder optionName, String value) {

        if (value == null || value.isBlank()) {
            return;
        }

        if (optionName.length() > 0) {
            optionName.append(" / ");
        }

        optionName.append(value);
    }

    /*
        리뷰 삭제

        실제 DELETE가 아니라 status 값을 DELETED로 변경합니다.

        본인이 작성한 정상 리뷰만 삭제할 수 있습니다.
    */
    public int deleteReview(Long reviewSeq, Long productSeq, Long memberSeq) {

        String sql = """
                UPDATE review
                SET status = 'DELETED',
                    updated_date = SYSDATE
                WHERE seq = :reviewSeq
                  AND product_seq = :productSeq
                  AND member_seq = :memberSeq
                  AND status = 'NORMAL'
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("reviewSeq", reviewSeq);
        params.put("productSeq", productSeq);
        params.put("memberSeq", memberSeq);

        return namedParameterJdbcTemplate.update(sql, params);
    }

    /*
        리뷰 이미지 삭제

        리뷰를 수정하면서 새 이미지를 다시 등록할 때,
        기존 이미지를 숨김 처리합니다.
    */
    public void deleteReviewImages(Long reviewSeq) {

        String sql = """
                UPDATE review_image
                SET status = 'DELETED',
                    updated_date = SYSDATE
                WHERE review_seq = :reviewSeq
                  AND status = 'NORMAL'
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("reviewSeq", reviewSeq);

        namedParameterJdbcTemplate.update(sql, params);
    }

    /*
        내가 쓴 리뷰 목록 조회 (마이페이지용)

        회원 번호로 본인이 작성한 NORMAL 상태의 리뷰를 최신순으로 조회합니다.
        상품명과 thumbnail_yn = 'Y' 인 대표 이미지를 함께 가져옵니다.
    */
    public List<com.example.java.mypage.dto.MyReviewDto> findMyReviews(Long memberSeq) {

        String sql = """
                SELECT
                    r.seq           AS review_seq,
                    r.product_seq,
                    p.product_name,
                    (SELECT pi.image_url
                       FROM product_image pi
                      WHERE pi.product_seq = p.seq
                        AND pi.thumbnail_yn = 'Y'
                        AND pi.status = 'NORMAL'
                        AND ROWNUM = 1) AS product_image_url,
                    r.rating,
                    r.content,
                    r.created_date,
                    r.updated_date
                FROM review r
                INNER JOIN product p ON r.product_seq = p.seq
                WHERE r.member_seq = :memberSeq
                  AND r.status = 'NORMAL'
                ORDER BY r.seq DESC
                """;

        Map<String, Object> params = Map.of("memberSeq", memberSeq);

        return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) ->
                com.example.java.mypage.dto.MyReviewDto.builder()
                        .reviewSeq(rs.getLong("review_seq"))
                        .productSeq(rs.getLong("product_seq"))
                        .productName(rs.getString("product_name"))
                        .productImageUrl(rs.getString("product_image_url"))
                        .rating(rs.getInt("rating"))
                        .content(rs.getString("content"))
                        .createdDate(toLocalDateTime(rs.getTimestamp("created_date")))
                        .updatedDate(toLocalDateTime(rs.getTimestamp("updated_date")))
                        .build()
        );
    }
}