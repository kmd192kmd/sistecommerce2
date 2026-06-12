package com.example.java.product.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.java.product.dto.ProductManageDto;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductManageRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;


    /*
        상품 관리 화면의 검색 결과 개수를 조회합니다.

        조회 기준:
        - product_request 기준으로 조회합니다.
        - 상품 등록 요청이 생성된 상품만 상품 관리 목록에 표시됩니다.
        - product.status = 'NORMAL'인 상품만 조회합니다.
    */
    public int countProductRequests(String approvalStatus,
                                    String startDate,
                                    String endDate,
                                    String searchType,
                                    String keyword) {

        MapSqlParameterSource params = new MapSqlParameterSource();

        String whereSql = makeWhereSql(
                params,
                approvalStatus,
                startDate,
                endDate,
                searchType,
                keyword
        );

        String sql = """
                SELECT COUNT(*)
                FROM product_request pr
                JOIN product p
                  ON pr.product_seq = p.seq
                JOIN seller s
                  ON pr.seller_seq = s.seq
                """ + whereSql;

        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);

        return count == null ? 0 : count;
    }


    /*
        상품 관리 화면에 출력할 상품 요청 목록을 조회합니다.

        화면에 필요한 정보가 여러 테이블에 나뉘어 있으므로
        product_request, product, seller 테이블을 JOIN합니다.

        product_request:
        - 승인 요청 상태
        - 요청일
        - 반려 사유

        product:
        - 상품명
        - 대표 이미지
        - 상품 번호

        seller:
        - 판매처명
        - 이메일
        - 전화번호
    */
    public List<ProductManageDto> findProductRequests(String approvalStatus,
                                                      String startDate,
                                                      String endDate,
                                                      String searchType,
                                                      String keyword,
                                                      String sortType,
                                                      int offset,
                                                      int size) {

        MapSqlParameterSource params = new MapSqlParameterSource();

        String whereSql = makeWhereSql(
                params,
                approvalStatus,
                startDate,
                endDate,
                searchType,
                keyword
        );

        params.addValue("offset", offset);
        params.addValue("size", size);

        String sql = """
                SELECT
                    pr.seq AS product_request_seq,
                    pr.product_seq,
                    pr.seller_seq,
                    pr.admin_seq,
                    pr.request_type,
                    pr.request_status,
                    pr.reject_reason,
                    pr.request_date,
                    pr.process_date,

                    p.product_name,
                    p.price,
                    p.thumbnail_url,
                    p.approval_status,
                    p.sale_status,

                    p.sales_count,
                    p.avg_rating,

                    s.name AS seller_name,
                    s.email AS seller_email,
                    s.phone AS seller_phone
                FROM product_request pr
                JOIN product p
                  ON pr.product_seq = p.seq
                JOIN seller s
                  ON pr.seller_seq = s.seq
                """ + whereSql + makeOrderBySql(sortType) + """
                OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY
                """;

        return jdbcTemplate.query(sql, params, this::mapProductManageDto);
    }


    /*
        판매처 상품 관리 화면의 검색 결과 개수를 조회합니다.

        기존 countProductRequests()는 관리자 화면에서 전체 상품 요청을 조회할 때 사용합니다.
        이 메서드는 판매처 화면에서 현재 로그인한 판매처의 상품 요청만 조회합니다.
    */
    public int countSellerProductRequests(Long sellerSeq,
                                          String approvalStatus,
                                          String startDate,
                                          String endDate,
                                          String searchType,
                                          String keyword) {

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("sellerSeq", sellerSeq);

        String whereSql = makeWhereSql(
                params,
                approvalStatus,
                startDate,
                endDate,
                searchType,
                keyword
        );

        whereSql += " AND pr.seller_seq = :sellerSeq ";

        String sql = """
                SELECT COUNT(*)
                FROM product_request pr
                JOIN product p
                  ON pr.product_seq = p.seq
                JOIN seller s
                  ON pr.seller_seq = s.seq
                """ + whereSql;

        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);

        return count == null ? 0 : count;
    }


    /*
        판매처 상품 관리 화면에 출력할 상품 요청 목록을 조회합니다.

        기존 findProductRequests()는 관리자 화면에서 전체 상품 요청을 조회할 때 사용합니다.
        이 메서드는 판매처 화면에서 현재 로그인한 판매처의 상품 요청만 조회합니다.
    */
    public List<ProductManageDto> findSellerProductRequests(Long sellerSeq,
                                                            String approvalStatus,
                                                            String startDate,
                                                            String endDate,
                                                            String searchType,
                                                            String keyword,
                                                            String sortType,
                                                            int offset,
                                                            int size) {

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("sellerSeq", sellerSeq);

        String whereSql = makeWhereSql(
                params,
                approvalStatus,
                startDate,
                endDate,
                searchType,
                keyword
        );

        whereSql += " AND pr.seller_seq = :sellerSeq ";

        params.addValue("offset", offset);
        params.addValue("size", size);

        String sql = """
                SELECT
                    pr.seq AS product_request_seq,
                    pr.product_seq,
                    pr.seller_seq,
                    pr.admin_seq,
                    pr.request_type,
                    pr.request_status,
                    pr.reject_reason,
                    pr.request_date,
                    pr.process_date,

                    p.product_name,
                    p.price,
                    p.thumbnail_url,
                    p.approval_status,
                    p.sale_status,

                    p.sales_count,
                    p.avg_rating,

                    s.name AS seller_name,
                    s.email AS seller_email,
                    s.phone AS seller_phone
                FROM product_request pr
                JOIN product p
                  ON pr.product_seq = p.seq
                JOIN seller s
                  ON pr.seller_seq = s.seq
                """ + whereSql + makeOrderBySql(sortType) + """
                OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY
                """;

        return jdbcTemplate.query(sql, params, this::mapProductManageDto);
    }


    /*
        검색 조건을 동적으로 만드는 메서드입니다.

        승인 여부:
        - 현재 HTML에서 승인 대기 radio의 value가 빈 문자열입니다.
        - 빈 문자열 또는 null이면 PENDING으로 조회합니다.
        - ALL이면 전체 조회합니다.

        기간 검색:
        - product_request.request_date 기준으로 조회합니다.

        검색어:
        - 상품명, 판매처명, 상품번호 기준으로 검색합니다.
    */
    private String makeWhereSql(MapSqlParameterSource params,
                                String approvalStatus,
                                String startDate,
                                String endDate,
                                String searchType,
                                String keyword) {

        StringBuilder where = new StringBuilder("""
                WHERE p.status = 'NORMAL'
                """);

        if (approvalStatus == null || approvalStatus.isBlank()) {
            where.append(" AND pr.request_status = 'PENDING' ");
        } else if (!"ALL".equals(approvalStatus)) {
            where.append(" AND pr.request_status = :approvalStatus ");
            params.addValue("approvalStatus", approvalStatus);
        }

        if (startDate != null && !startDate.isBlank()) {
            where.append(" AND pr.request_date >= TO_DATE(:startDate, 'YYYY-MM-DD') ");
            params.addValue("startDate", startDate);
        }

        if (endDate != null && !endDate.isBlank()) {
            where.append(" AND pr.request_date < TO_DATE(:endDate, 'YYYY-MM-DD') + 1 ");
            params.addValue("endDate", endDate);
        }

        if (keyword != null && !keyword.isBlank()) {

            if ("sellerName".equals(searchType)) {
                where.append("""
                        AND LOWER(REPLACE(s.name, ' ', ''))
                            LIKE LOWER('%' || REPLACE(:keyword, ' ', '') || '%')
                        """);
                params.addValue("keyword", keyword);

            } else if ("productSeq".equals(searchType)) {
                where.append(" AND TO_CHAR(p.seq) = :keyword ");
                params.addValue("keyword", keyword);

            } else {
                where.append("""
                        AND LOWER(REPLACE(p.product_name, ' ', ''))
                            LIKE LOWER('%' || REPLACE(:keyword, ' ', '') || '%')
                        """);
                params.addValue("keyword", keyword);
            }
        }

        return where.toString();
    }


    /*
        추가된 부분

        상품 관리 목록의 정렬 조건을 만드는 메서드입니다.

        정렬 조건:
        - salesDesc  : 판매량순
        - latestDesc : 최신순
        - priceAsc   : 가격순
        - ratingDesc : 별점순

        주의:
        ORDER BY 컬럼명은 SQL 파라미터로 바인딩할 수 없습니다.

        그래서 사용자가 보낸 sortType 값을 SQL에 그대로 붙이지 않고,
        switch문으로 허용된 값만 ORDER BY로 변환합니다.
    */
    private String makeOrderBySql(String sortType) {

        if (sortType == null || sortType.isBlank()) {
            return " ORDER BY pr.seq DESC ";
        }

        return switch (sortType) {
            case "salesDesc" -> " ORDER BY p.sales_count DESC, pr.seq DESC ";
            case "latestDesc" -> " ORDER BY p.created_date DESC, pr.seq DESC ";
            case "priceAsc" -> " ORDER BY p.price ASC, pr.seq DESC ";
            case "ratingDesc" -> " ORDER BY p.avg_rating DESC, pr.seq DESC ";
            default -> " ORDER BY pr.seq DESC ";
        };
    }


    /*
        SQL 조회 결과를 ProductManageDto로 변환합니다.

        주의:
        admin_seq는 NULL일 수 있으므로 rs.wasNull()로 한 번 더 확인합니다.
    */
    private ProductManageDto mapProductManageDto(ResultSet rs, int rowNum) throws SQLException {

        ProductManageDto dto = new ProductManageDto();

        dto.setProductRequestSeq(rs.getLong("product_request_seq"));
        dto.setSeq(rs.getLong("product_seq"));
        dto.setProductSeq(rs.getLong("product_seq"));
        dto.setSellerSeq(rs.getLong("seller_seq"));

        Long adminSeq = rs.getLong("admin_seq");
        dto.setAdminSeq(rs.wasNull() ? null : adminSeq);

        dto.setRequestType(rs.getString("request_type"));
        dto.setRequestStatus(rs.getString("request_status"));
        dto.setRejectReason(rs.getString("reject_reason"));

        dto.setRequestDate(
                rs.getTimestamp("request_date") != null
                        ? rs.getTimestamp("request_date").toLocalDateTime()
                        : null
        );

        dto.setProcessDate(
                rs.getTimestamp("process_date") != null
                        ? rs.getTimestamp("process_date").toLocalDateTime()
                        : null
        );

        dto.setCreatedDate(dto.getRequestDate());

        dto.setProductName(rs.getString("product_name"));
        dto.setPrice(rs.getInt("price"));
        dto.setThumbnailUrl(rs.getString("thumbnail_url"));

        dto.setApprovalStatus(rs.getString("approval_status"));
        dto.setSaleStatus(rs.getString("sale_status"));

        /*
            정렬에 사용하는 판매량과 평균 별점 값을 DTO에 담습니다.

            현재 화면에는 직접 출력하지 않더라도,
            판매량순 / 별점순 정렬 기준으로 사용한 값을
            나중에 화면에 표시할 수 있습니다.
        */
        dto.setSalesCount(rs.getLong("sales_count"));
        dto.setAvgRating(rs.getDouble("avg_rating"));

        dto.setSellerName(rs.getString("seller_name"));
        dto.setSellerEmail(rs.getString("seller_email"));
        dto.setSellerPhone(rs.getString("seller_phone"));

        return dto;
    }


    /*
        선택한 상품들을 삭제 처리합니다.

        실제 DELETE가 아니라 product.status를 DELETED로 바꾸고,
        hide_yn을 Y로 바꾸는 소프트 삭제 방식입니다.
    */
    public int deleteProducts(List<Long> productSeqs) {

        String sql = """
                UPDATE product
                SET status = 'DELETED',
                    hide_yn = 'Y',
                    updated_date = SYSDATE
                WHERE seq IN (:productSeqs)
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("productSeqs", productSeqs);

        int result = jdbcTemplate.update(sql, params);
        if (result > 0 && productSeqs != null) {
            for (Long seq : productSeqs) {
                eventPublisher.publishEvent(new com.example.java.product.event.ProductUpdatedEvent(seq));
            }
        }
        return result;
    }


    /*
        판매처 상품 관리 화면에서 선택한 상품들을 삭제 처리합니다.

        실제 DELETE가 아니라 product.status를 DELETED로 바꾸고,
        hide_yn을 Y로 바꾸는 소프트 삭제 방식입니다.

        sellerSeq 조건을 추가해서
        현재 로그인한 판매처의 상품만 삭제할 수 있게 합니다.
    */
    public int deleteSellerProducts(Long sellerSeq, List<Long> productSeqs) {

        String sql = """
                UPDATE product
                SET status = 'DELETED',
                    hide_yn = 'Y',
                    updated_date = SYSDATE
                WHERE seq IN (:productSeqs)
                  AND seller_seq = :sellerSeq
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("productSeqs", productSeqs)
                .addValue("sellerSeq", sellerSeq);

        int result = jdbcTemplate.update(sql, params);

        if (result > 0 && productSeqs != null) {
            for (Long seq : productSeqs) {
                eventPublisher.publishEvent(new com.example.java.product.event.ProductUpdatedEvent(seq));
            }
        }

        return result;
    }
}