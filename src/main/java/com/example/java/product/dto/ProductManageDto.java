package com.example.java.product.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductManageDto {

    /*
        상품 관리 화면 목록에 출력할 데이터를 담는 DTO입니다.

        이 화면은 product 테이블만 조회하면 안 되고,
        product_request, product, seller 테이블을 함께 조회해야 합니다.

        화면 컬럼:
        - 번호       : product_request.seq
        - 승인상태   : product_request.request_status
        - 상품명     : product.product_name
        - 대표이미지 : product.thumbnail_url
        - 판매처     : seller.name
        - 요청일     : product_request.request_date
        - 반려상세   : product_request.reject_reason
        - 상품상세   : product.seq
    */

    private Long seq;

    private Long productRequestSeq;
    private Long productSeq;
    private Long sellerSeq;
    private Long adminSeq;

    private String requestType;
    private String requestStatus;
    private String rejectReason;

    private LocalDateTime requestDate;
    private LocalDateTime processDate;

    private String productName;
    private Integer price;
    private String thumbnailUrl;

    private String approvalStatus;
    private String saleStatus;

    private String sellerName;
    private String sellerEmail;
    private String sellerPhone;

    /*
        기존 상품 관리 HTML에서는 product.createdDate로 요청일을 출력하고 있습니다.

        기존 HTML을 크게 바꾸지 않기 위해
        createdDate에는 product_request.request_date 값을 넣어서 사용합니다.
    */
    private LocalDateTime createdDate;

    /*
        상품 정렬에 사용할 값입니다.

        판매량순:
        - product.sales_count 기준

        별점순:
        - product.avg_rating 기준

        최신순:
        - product.created_date 또는 product_request.request_date 기준
    */
    private Long salesCount;
    private Double avgRating;
}