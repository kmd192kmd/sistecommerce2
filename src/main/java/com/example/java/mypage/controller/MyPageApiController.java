package com.example.java.mypage.controller;

import com.example.java.member.dto.DeliveryAddressDto;
import com.example.java.member.entity.DeliveryAddress;
import com.example.java.member.security.CustomUserDetails;
import com.example.java.member.service.MemberAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/mypage/address")
@RestController
@RequiredArgsConstructor
public class MyPageApiController {

    private final MemberAddressService memberAddressService;

    @PostMapping
    public ResponseEntity<?> addMyAddress(@RequestBody DeliveryAddressDto deliveryAddressDto, @AuthenticationPrincipal     CustomUserDetails customUserDetails ) {

        deliveryAddressDto.setMemberSeq(customUserDetails.getMemberSeq());
        memberAddressService.addMyAddress(deliveryAddressDto);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{seq}")
    public ResponseEntity<DeliveryAddress> getAddress(@PathVariable Long seq,
                                                      @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        DeliveryAddress address = memberAddressService.getAddress(seq, customUserDetails.getMemberSeq());
        return ResponseEntity.ok(address);
    }

    @PutMapping("/{seq}")
    public ResponseEntity<?> updateAddress(@PathVariable Long seq,
                                           @RequestBody DeliveryAddressDto deliveryAddressDto,
                                           @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        deliveryAddressDto.setMemberSeq(customUserDetails.getMemberSeq());
        memberAddressService.updateAddress(seq, deliveryAddressDto);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{seq}/default")
    public ResponseEntity<?> setDefault(@PathVariable Long seq,
                                        @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        memberAddressService.setDefault(seq, customUserDetails.getMemberSeq());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{seq}")
    public ResponseEntity<?> deleteAddress(@PathVariable Long seq,
                                           @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        memberAddressService.deleteAddress(seq, customUserDetails.getMemberSeq());
        return ResponseEntity.ok().build();
    }

}
