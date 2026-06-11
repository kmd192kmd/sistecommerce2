package com.example.java.product.listener;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.example.java.product.document.ProductDocument;
import com.example.java.product.repository.ProductSearchRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!prod")
public class ElasticsearchIndexInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void run(String... args) throws Exception {
        log.info("Elasticsearch 인덱스 강제 재동기화(Force Sync) 시작...");
        try {
            // 1. 기존 인덱스 삭제 및 재생성 (매핑 초기화)
            IndexOperations indexOps = elasticsearchOperations.indexOps(ProductDocument.class);
            if (indexOps.exists()) {
                indexOps.delete();
                log.info("기존 Elasticsearch 상품 인덱스를 삭제했습니다.");
            }
            indexOps.create();
            indexOps.putMapping(indexOps.createMapping(ProductDocument.class));
            log.info("Elasticsearch 상품 인덱스 및 매핑을 신규 생성했습니다.");

            // 2. DB에서 필터링 없이 전체 상품 데이터 조회
            String sql = """
                    SELECT 
                        seq, seller_seq, category_seq, product_name, price, 
                        sale_status, approval_status, hide_yn, view_count, 
                        avg_rating, review_count, sales_count, created_date, 
                        status, thumbnail_url 
                    FROM product
                    """;

            List<ProductDocument> docs = jdbcTemplate.query(sql, (rs, rowNum) -> ProductDocument.builder()
                    .id(rs.getLong("seq"))
                    .sellerSeq(rs.getLong("seller_seq"))
                    .categorySeq(rs.getLong("category_seq"))
                    .productName(rs.getString("product_name"))
                    .price(rs.getInt("price"))
                    .saleStatus(rs.getString("sale_status"))
                    .approvalStatus(rs.getString("approval_status"))
                    .hideYn(rs.getString("hide_yn"))
                    .viewCount(rs.getLong("view_count"))
                    .avgRating(rs.getDouble("avg_rating"))
                    .reviewCount(rs.getLong("review_count"))
                    .salesCount(rs.getLong("sales_count"))
                    .createdDate(rs.getTimestamp("created_date") != null 
                            ? rs.getTimestamp("created_date").toLocalDateTime() 
                            : null)
                    .status(rs.getString("status"))
                    .thumbnailUrl(rs.getString("thumbnail_url"))
                    .build());

            // 3. Elasticsearch에 전체 저장
            if (!docs.isEmpty()) {
                productSearchRepository.saveAll(docs);
                log.info("Elasticsearch 전체 상품 동기화 완료! (색인된 상품 수: {}개)", docs.size());
            } else {
                log.info("DB에 동기화할 상품 데이터가 존재하지 않습니다.");
            }
        } catch (Exception e) {
            log.error("Elasticsearch 초기화 및 강제 동기화 실패: ", e);
        }
    }
}
