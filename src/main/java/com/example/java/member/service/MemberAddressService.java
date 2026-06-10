package com.example.java.member.service;

import com.example.java.member.dto.DeliveryAddressDto;
import com.example.java.member.entity.DeliveryAddress;
import com.example.java.member.entity.Member;
import com.example.java.member.repository.MemberAddressRepository;
import com.example.java.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAddressService {

    private final MemberAddressRepository memberAddressRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void addMyAddress(DeliveryAddressDto deliveryAddressDto) {
        Member member = memberRepository.getReferenceById(deliveryAddressDto.getMemberSeq());

        DeliveryAddress deliveryAddress = DeliveryAddress.builder()
                .member(member)
                .addressAlias(deliveryAddressDto.getAddressAlias())
                .recipientName(deliveryAddressDto.getRecipientName())
                .recipientPhone(deliveryAddressDto.getRecipientPhone())
                .zipcode(deliveryAddressDto.getZipcode())
                .address(deliveryAddressDto.getAddress())
                .addressDetail(deliveryAddressDto.getAddressDetail())
                .note(deliveryAddressDto.getNote())
                .entryCode(deliveryAddressDto.getEntryCode())
                .defaultYn(deliveryAddressDto.getDefaultYn())
                .status("Y")
                .build();

        if ("Y".equals(deliveryAddressDto.getDefaultYn())) {
            clearCurrentDefault(deliveryAddressDto.getMemberSeq());
        }

        memberAddressRepository.save(deliveryAddress);
    }

    @Transactional
    public void setDefault(Long addressSeq, Long memberSeq) {
        clearCurrentDefault(memberSeq);
        memberAddressRepository.findById(addressSeq)
                .ifPresent(DeliveryAddress::setDefault);
    }

    public List<DeliveryAddress> myAddress(Long memberSeq) {
        return memberAddressRepository.findByMember_SeqAndStatusOrderByDefaultYnDescSeqDesc(memberSeq, "Y");
    }

    public DeliveryAddress getAddress(Long addressSeq, Long memberSeq) {
        return memberAddressRepository.findById(addressSeq)
                .filter(a -> a.getMember().getSeq().equals(memberSeq))
                .filter(a -> "Y".equals(a.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("배송지를 찾을 수 없습니다."));
    }

    @Transactional
    public void updateAddress(Long addressSeq, DeliveryAddressDto dto) {
        DeliveryAddress address = memberAddressRepository.findById(addressSeq)
                .filter(a -> a.getMember().getSeq().equals(dto.getMemberSeq()))
                .orElseThrow(() -> new IllegalArgumentException("배송지를 찾을 수 없습니다."));
        if ("Y".equals(dto.getDefaultYn())) {
            clearCurrentDefault(dto.getMemberSeq());
        }
        address.update(dto);
    }

    @Transactional
    public void deleteAddress(Long addressSeq, Long memberSeq) {
        memberAddressRepository.findById(addressSeq)
                .filter(a -> a.getMember().getSeq().equals(memberSeq))
                .ifPresent(memberAddressRepository::delete);
    }

    private void clearCurrentDefault(Long memberSeq) {
        memberAddressRepository.findByMember_SeqOrderByDefaultYnDesc(memberSeq).stream()
                .filter(a -> "Y".equals(a.getDefaultYn()))
                .forEach(DeliveryAddress::clearDefault);
    }
}
