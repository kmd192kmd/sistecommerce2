package com.example.java.product.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.product.dto.ProductDto;
import com.example.java.product.entity.Product;
import com.example.java.product.repository.ProductListRepository;

import lombok.RequiredArgsConstructor;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.example.java.product.document.ProductDocument;
import com.example.java.admin.repository.HotDealProductRepository;

/*
    ProductListService

    상품 목록 조회, 검색, 필터링 등 읽기 중심의 목록 관련 기능을 제공합니다.
*/
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductListService {

    private final ProductListRepository productListRepository;
    private final CategoryService categoryService;
    private final org.springframework.cache.CacheManager cacheManager;
    private final ElasticsearchOperations elasticsearchOperations;
    private final HotDealProductRepository hotDealProductRepository;

    private static class HotDealDetail {
        final Integer discountRate;
        final Integer discountPrice;

        HotDealDetail(Integer discountRate, Integer discountPrice) {
            this.discountRate = discountRate;
            this.discountPrice = discountPrice;
        }
    }

    public Page<ProductDto> getProductList(
            Long categorySeq,
            String keyword,
            String sortBy,
            int page) {

        return getProductList(categorySeq, keyword, sortBy, page, null, null, null, false, false);
    }

    public Page<ProductDto> getProductList(
            Long categorySeq,
            String keyword,
            String sortBy,
            int page,
            Integer minPrice,
            Integer maxPrice,
            Double minRating,
            boolean hideOutOfStock,
            boolean showHotDealsOnly) {

        Pageable pageable = PageRequest.of(page, 20, getSortOption(sortBy));

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        List<Long> activeHotDealProductSeqs = hotDealProductRepository.findActiveHotDealProductSeqs(now);
        List<Object[]> activeHotDealDetails = hotDealProductRepository.findActiveHotDealDetails(now);

        // Map productSeq -> HotDealDetail
        java.util.Map<Long, HotDealDetail> hotDealMap = new java.util.HashMap<>();
        for (Object[] detail : activeHotDealDetails) {
            Long productSeq = (Long) detail[0];
            Integer discountRate = (Integer) detail[1];
            Integer discountPrice = (Integer) detail[2];
            hotDealMap.put(productSeq, new HotDealDetail(discountRate, discountPrice));
        }

        // Helper mapping function for ProductDto
        java.util.function.Consumer<ProductDto> applyHotDeal = dto -> {
            if (hotDealMap.containsKey(dto.getSeq())) {
                HotDealDetail detail = hotDealMap.get(dto.getSeq());
                dto.setHotDeal(true);
                int originalPrice = dto.getPrice();
                dto.setOriginalPrice(originalPrice);
                
                int discount = 0;
                int finalDiscountRate = 0;
                if (detail.discountPrice != null && detail.discountPrice > 0) {
                    discount = detail.discountPrice;
                    finalDiscountRate = originalPrice > 0 ? (detail.discountPrice * 100) / originalPrice : 0;
                } else if (detail.discountRate != null && detail.discountRate > 0) {
                    discount = originalPrice * detail.discountRate / 100;
                    finalDiscountRate = detail.discountRate;
                }
                
                dto.setDiscountRate(finalDiscountRate);
                dto.setPrice(Math.max(0, originalPrice - discount));
            } else {
                dto.setHotDeal(false);
                dto.setOriginalPrice(dto.getPrice());
                dto.setDiscountRate(0);
            }
        };

        // 1. 추천(recommend) 정렬은 기존 RDBMS 방식 그대로 사용 (캐싱용이므로 성능 영향 없음)
        if ("recommend".equalsIgnoreCase(sortBy)) {
            Integer finalMinPrice = minPrice != null ? minPrice : 0;
            Integer finalMaxPrice = maxPrice != null ? maxPrice : 999999999;
            Double finalMinRating = minRating != null ? minRating : 0.0;
            String finalKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword : null;
            String saleStatus = hideOutOfStock ? "ON_SALE" : null;
            List<Long> categorySeqs = categoryService.getDescendantCategorySeqs(categorySeq);

            Page<Product> productPage = productListRepository.findWithFilters(
                    categorySeqs,
                    finalKeyword,
                    finalMinPrice,
                    finalMaxPrice,
                    finalMinRating,
                    saleStatus,
                    pageable,
                    activeHotDealProductSeqs,
                    showHotDealsOnly
            );
            Page<ProductDto> dtoPage = productPage.map(Product::toDto);
            dtoPage.forEach(applyHotDeal);
            return dtoPage;
        }

        // 2. 그 외 일반 정렬/검색은 Elasticsearch 활용
        String queryJson;
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode boolNode = mapper.createObjectNode();
            
            // must
            ArrayNode mustArray = mapper.createArrayNode();
            mustArray.add(mapper.createObjectNode().set("term", mapper.createObjectNode().put("status", "NORMAL")));
            mustArray.add(mapper.createObjectNode().set("term", mapper.createObjectNode().put("hideYn", "N")));
            
            // must_not
            ArrayNode mustNotArray = mapper.createArrayNode();
            mustNotArray.add(mapper.createObjectNode().set("term", mapper.createObjectNode().put("saleStatus", "STOPPED")));
            boolNode.set("must_not", mustNotArray);
            
            // filter
            ArrayNode filterArray = mapper.createArrayNode();
            
            if (showHotDealsOnly) {
                if (activeHotDealProductSeqs == null || activeHotDealProductSeqs.isEmpty()) {
                    return new PageImpl<>(java.util.Collections.emptyList(), pageable, 0);
                } else {
                    ArrayNode hotDeals = mapper.createArrayNode();
                    for (Long seq : activeHotDealProductSeqs) {
                        hotDeals.add(seq);
                    }
                    filterArray.add(mapper.createObjectNode().set("terms", mapper.createObjectNode().set("id", hotDeals)));
                }
            }
            
            List<Long> categorySeqs = categoryService.getDescendantCategorySeqs(categorySeq);
            if (categorySeqs != null && !categorySeqs.isEmpty()) {
                ArrayNode catArray = mapper.createArrayNode();
                for (Long seq : categorySeqs) {
                    catArray.add(seq);
                }
                filterArray.add(mapper.createObjectNode().set("terms", mapper.createObjectNode().set("categorySeq", catArray)));
            }
            
            if ((minPrice != null && minPrice > 0) || (maxPrice != null && maxPrice < 999999999)) {
                ObjectNode priceRange = mapper.createObjectNode();
                ObjectNode rangeOpts = mapper.createObjectNode();
                if (minPrice != null && minPrice > 0) {
                    rangeOpts.put("gte", minPrice);
                }
                if (maxPrice != null && maxPrice < 999999999) {
                    rangeOpts.put("lte", maxPrice);
                }
                priceRange.set("price", rangeOpts);
                filterArray.add(mapper.createObjectNode().set("range", priceRange));
            }
            
            if (minRating != null && minRating > 0.0) {
                ObjectNode ratingRange = mapper.createObjectNode();
                ratingRange.set("avgRating", mapper.createObjectNode().put("gte", minRating));
                filterArray.add(mapper.createObjectNode().set("range", ratingRange));
            }
            
            if (hideOutOfStock) {
                filterArray.add(mapper.createObjectNode().set("term", mapper.createObjectNode().put("saleStatus", "ON_SALE")));
            }
            
            boolNode.set("filter", filterArray);
            
            // Keyword (Advanced Search: Nori + Autocomplete + Chosung + Fuzzy + Vector)
            if (keyword != null && !keyword.trim().isEmpty()) {
                String trimmedKeyword = keyword.trim();
                ObjectNode keywordBool = mapper.createObjectNode();
                ArrayNode keywordShould = mapper.createArrayNode();
                
                if (com.example.java.product.util.ChosungUtil.isChosungOnly(trimmedKeyword)) {
                    ObjectNode matchChosung = mapper.createObjectNode();
                    matchChosung.set("productNameChosung", mapper.createObjectNode()
                            .put("query", trimmedKeyword)
                            .put("boost", 50.0));
                    keywordShould.add(mapper.createObjectNode().set("match", matchChosung));
                } else {
                    // 1. 완전 일치 (match_phrase)에 가장 높은 가중치를 주어 어순까지 같은 상품을 최우선 노출
                    ObjectNode matchPhrase = mapper.createObjectNode();
                    matchPhrase.set("productName", mapper.createObjectNode()
                            .put("query", trimmedKeyword)
                            .put("boost", 100.0));
                    keywordShould.add(mapper.createObjectNode().set("match_phrase", matchPhrase));

                    // 2. 기본 형태소 매칭 (match) 가중치 상향
                    ObjectNode matchName = mapper.createObjectNode();
                    matchName.set("productName", mapper.createObjectNode()
                            .put("query", trimmedKeyword)
                            .put("boost", 50.0));
                    keywordShould.add(mapper.createObjectNode().set("match", matchName));
                    
                    // 3. 자동완성 및 부분 매칭 (autocomplete) 가중치 상향
                    ObjectNode matchAuto = mapper.createObjectNode();
                    matchAuto.set("productName.autocomplete", mapper.createObjectNode()
                            .put("query", trimmedKeyword)
                            .put("boost", 20.0));
                    keywordShould.add(mapper.createObjectNode().set("match", matchAuto));
                    
                    // 4. 오타 및 퍼지 매칭 (fuzzy) 가중치 상향
                    ObjectNode fuzzyName = mapper.createObjectNode();
                    fuzzyName.set("productName", mapper.createObjectNode()
                            .put("value", trimmedKeyword)
                            .put("fuzziness", "AUTO")
                            .put("boost", 5.0));
                    keywordShould.add(mapper.createObjectNode().set("fuzzy", fuzzyName));
                }
                ObjectNode boolInner = mapper.createObjectNode();
                boolInner.set("should", keywordShould);
                boolInner.put("minimum_should_match", 1);
                keywordBool.set("bool", boolInner);
                
                mustArray.add(keywordBool);
            }
            
            boolNode.set("must", mustArray);
            
            // Popularity score sorting
            boolean isPopularitySort = (sortBy == null || "popularity".equalsIgnoreCase(sortBy) || "salesdesc".equalsIgnoreCase(sortBy));
            if (isPopularitySort) {
                ObjectNode functionScoreNode = mapper.createObjectNode();
                functionScoreNode.set("query", mapper.createObjectNode().set("bool", boolNode));
                
                ArrayNode functions = mapper.createArrayNode();
                ObjectNode scriptScore = mapper.createObjectNode();
                ObjectNode script = mapper.createObjectNode();
                
                String scriptSource;
                if (keyword != null && !keyword.trim().isEmpty()) {
                    // 검색어가 있는 경우: 검색어 매칭(임베딩 유사도 등)의 비중이 훨씬 높도록 가중치를 조정하고, 인기 지수는 로그 스케일로 극도로 제한(최대 약 5점)합니다.
                    scriptSource = "Math.log1p(doc['salesCount'].value) * 0.2 + Math.log1p(doc['viewCount'].value) * 0.1 + doc['avgRating'].value * 0.05 + Math.log1p(doc['reviewCount'].value) * 0.05";
                    scriptSource += " + (doc['embedding'].size() == 0 ? 0.0 : (cosineSimilarity(params.queryVector, 'embedding') + 1.0) * 100.0)";
                    
                    float[] queryVector = com.example.java.product.util.EmbeddingUtil.getEmbedding(keyword.trim());
                    ArrayNode vectorArray = mapper.createArrayNode();
                    for (float v : queryVector) {
                        vectorArray.add(v);
                    }
                    ObjectNode paramsNode = mapper.createObjectNode();
                    paramsNode.set("queryVector", vectorArray);
                    script.set("params", paramsNode);
                } else {
                    // 검색어가 없는 경우: 기존의 단순 인기 지표 합산 수식을 유지합니다.
                    scriptSource = "doc['salesCount'].value * 50.0 + doc['viewCount'].value * 30.0 + doc['avgRating'].value * 10.0 + doc['reviewCount'].value * 10.0";
                }
                
                script.put("source", scriptSource);
                scriptScore.set("script", script);
                functions.add(mapper.createObjectNode().set("script_score", scriptScore));
                
                functionScoreNode.set("functions", functions);
                functionScoreNode.put("boost_mode", "sum");
                
                queryJson = mapper.writeValueAsString(mapper.createObjectNode().set("function_score", functionScoreNode));
            } else {
                queryJson = mapper.writeValueAsString(mapper.createObjectNode().set("bool", boolNode));
            }
        } catch (Exception e) {
            throw new RuntimeException("Elasticsearch query build failed", e);
        }
        
        Query query = new StringQuery(queryJson);
        query.setPageable(pageable);
        
        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(query, ProductDocument.class);
        
        List<ProductDto> list = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(doc -> ProductDto.builder()
                        .seq(doc.getId())
                        .sellerSeq(doc.getSellerSeq())
                        .categorySeq(doc.getCategorySeq())
                        .productName(doc.getProductName())
                        .price(doc.getPrice())
                        .saleStatus(doc.getSaleStatus())
                        .approvalStatus(doc.getApprovalStatus())
                        .hideYn(doc.getHideYn())
                        .viewCount(doc.getViewCount())
                        .avgRating(doc.getAvgRating())
                        .reviewCount(doc.getReviewCount())
                        .salesCount(doc.getSalesCount())
                        .createdDate(doc.getCreatedDate())
                        .status(doc.getStatus())
                        .thumbnailUrl(doc.getThumbnailUrl())
                        .image(doc.getThumbnailUrl())
                        .build())
                .collect(Collectors.toList());

        list.forEach(applyHotDeal);

        return new PageImpl<>(list, pageable, searchHits.getTotalHits());
    }

    private Sort getSortOption(String sortBy) {

        if (sortBy == null || "popularity".equalsIgnoreCase(sortBy) || "salesdesc".equalsIgnoreCase(sortBy) || "sales_desc".equalsIgnoreCase(sortBy)) {
            return Sort.by(Sort.Direction.DESC, "_score");
        }

        return switch (sortBy.toLowerCase()) {
            case "price_asc", "priceasc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc", "pricedesc" -> Sort.by(Sort.Direction.DESC, "price");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdDate");
            case "review", "reviewdesc" -> Sort.by(Sort.Direction.DESC, "reviewCount");
            case "recommend" -> Sort.by(Sort.Direction.DESC, "recommend");
            default -> Sort.by(Sort.Direction.DESC, "_score");
        };
    }

    @org.springframework.cache.annotation.Cacheable(value = "popularProducts", key = "'list'")
    public List<ProductDto> getPopularProducts() {
        return getProductList(null, null, "recommend", 0, null, null, null, false, false)
                .getContent().stream().limit(5).collect(java.util.stream.Collectors.toList());
    }

    @org.springframework.cache.annotation.CachePut(value = "popularProducts", key = "'list'")
    public List<ProductDto> refreshPopularProducts() {
        // 1. 기존 캐시에서 이전 데이터 순위 가져오기
        java.util.List<ProductDto> previousList = null;
        org.springframework.cache.Cache cache = cacheManager.getCache("popularProducts");
        if (cache != null) {
            org.springframework.cache.Cache.ValueWrapper wrapper = cache.get("list");
            if (wrapper != null) {
                previousList = (java.util.List<ProductDto>) wrapper.get();
            }
        }

        // 2. 신규 추천 상품 조회 (5개)
        java.util.List<ProductDto> newProducts = getProductList(null, null, "recommend", 0, null, null, null, false, false)
                .getContent().stream().limit(5).collect(java.util.stream.Collectors.toList());

        // 3. 이전 순위 맵 구성 (ProductSeq -> Rank(1부터 시작))
        java.util.Map<Long, Integer> previousRankMap = new java.util.HashMap<>();
        if (previousList != null) {
            for (int i = 0; i < previousList.size(); i++) {
                if (previousList.get(i) != null && previousList.get(i).getSeq() != null) {
                    previousRankMap.put(previousList.get(i).getSeq(), i + 1);
                }
            }
        }

        // 4. 순위 변동 계산 및 설정
        int maxRise = 0;
        for (int i = 0; i < newProducts.size(); i++) {
            ProductDto newProduct = newProducts.get(i);
            int newRank = i + 1;
            newProduct.setHot(false); // 초기화

            int rise = 0;
            if (previousRankMap.containsKey(newProduct.getSeq())) {
                int oldRank = previousRankMap.get(newProduct.getSeq());
                int change = oldRank - newRank; // 이전 순위가 더 크면(예: 3위 -> 1위) 순위 상승(+)
                rise = change;
                if (change > 0) {
                    newProduct.setRankChange("▲" + change);
                } else if (change < 0) {
                    newProduct.setRankChange("▼" + Math.abs(change));
                } else {
                    newProduct.setRankChange("-");
                }
            } else {
                newProduct.setRankChange("NEW");
                // 이전 리스트가 존재하는 경우에만 신규 진입 상품의 이전 순위를 6위로 가정하여 상승폭 계산
                if (previousList != null && !previousList.isEmpty()) {
                    rise = 6 - newRank;
                } else {
                    rise = 0;
                }
            }

            if (rise > maxRise) {
                maxRise = rise;
            }
        }

        // 5. 가장 많이 상승한 상품에 hot=true 설정 (상승폭이 1 이상인 경우에만)
        if (maxRise > 0) {
            for (int i = 0; i < newProducts.size(); i++) {
                ProductDto newProduct = newProducts.get(i);
                int newRank = i + 1;
                int rise = 0;
                if (previousRankMap.containsKey(newProduct.getSeq())) {
                    rise = previousRankMap.get(newProduct.getSeq()) - newRank;
                } else if (previousList != null && !previousList.isEmpty()) {
                    rise = 6 - newRank;
                }
                if (rise == maxRise) {
                    newProduct.setHot(true);
                }
            }
        }

        return newProducts;
    }
}
