package com.example.java.member.dto;

import com.example.java.member.entity.DeliveryAddress;
import lombok.*;

@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAddressDto {

    private Long seq;
    private Long memberSeq;
    private String addressAlias;
    private String recipientName;
    private String recipientPhone;
    private String zipcode;
    private String address;
    private String addressDetail;
    private String note;
    private String entryCode;
    private String defaultYn;
    private String status;

    public static DeliveryAddressDto from(DeliveryAddress deliveryAddress) {
        return DeliveryAddressDto.builder()
                .seq(deliveryAddress.getSeq())
                .memberSeq(deliveryAddress.getMember().getSeq())
                .addressAlias(deliveryAddress.getAddressAlias())
                .recipientName(deliveryAddress.getRecipientName())
                .recipientPhone(deliveryAddress.getRecipientPhone())
                .zipcode(deliveryAddress.getZipcode())
                .address(deliveryAddress.getAddress())
                .addressDetail(deliveryAddress.getAddressDetail())
                .note(deliveryAddress.getNote())
                .entryCode(deliveryAddress.getEntryCode())
                .defaultYn(deliveryAddress.getDefaultYn())
                .status(deliveryAddress.getStatus())
                .build();
    }
}
