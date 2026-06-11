package com.example.java.product.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import com.example.java.product.document.ProductDocument;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {
}
