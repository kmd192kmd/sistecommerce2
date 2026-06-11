package com.example.java.product.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ProductUpdatedEvent {
    private final Long productSeq;
}
