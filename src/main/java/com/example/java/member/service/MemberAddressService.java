package com.example.java.member.service;

import com.example.java.member.dto.DeliveryAddressDto;
import com.example.java.member.entity.DeliveryAddress;
import com.example.java.member.repository.MemberAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAddressService {

    private final MemberAddressRepository memberAddressRepository;

    public List<DeliveryAddress> myAddress(Long memberSeq) {

        return memberAddressRepository.findByMember_Seq(memberSeq);
    }
}
