package com.example.java.cart.service;

import com.example.java.cart.repository.CartLogRepository;
import com.example.java.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartLogRepository cartLogRepository;


}
