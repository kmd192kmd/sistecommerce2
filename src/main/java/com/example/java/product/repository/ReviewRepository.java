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
                    seq,
                    product_seq,
                    member_seq,
                    order_item_seq,
                    rating,
                    content,
                    created_date,
                    updated_date,
                    status
                FROM review
                WHERE status = 'NORMAL'
                  AND (:lastReviewSeq IS NULL OR seq < :lastReviewSeq)
                ORDER BY seq DESC
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
                    seq,
                    product_seq,
                    member_seq,
                    order_item_seq,
                    rating,
                    content,
                    created_date,
                    updated_date,
                    status
                FROM review
                WHERE product_seq = :productSeq
                  AND status = 'NORMAL'
                  AND (:lastReviewSeq IS NULL OR seq < :lastReviewSeq)
                ORDER BY seq DESC
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
                        .fileSize(rs.getLong("file_size"))
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
}