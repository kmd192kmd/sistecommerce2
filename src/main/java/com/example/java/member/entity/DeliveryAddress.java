package com.example.java.member.entity;

import com.example.java.member.dto.DeliveryAddressDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "delivery_address")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "delivery_address_seq")
    @SequenceGenerator(name = "delivery_address_seq", sequenceName = "delivery_address_seq",  allocationSize = 1)
    private Long seq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_seq", nullable = false)
    private Member member;

    @Column(name = "address_alias")
    private String addressAlias;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false)
    private String recipientPhone;

    @Column(name = "zipcode", nullable = false)
    private String zipcode;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "address_detail")
    private String addressDetail;

    @Column(name = "note")
    private String note;

    @Column(name = "entry_code")
    private String entryCode;

    @Column(name = "default_yn", nullable = false)
    private String defaultYn;

    @Column(name = "status", nullable = false)
    private String status;

    public void clearDefault() {
        this.defaultYn = "N";
    }

    public void setDefault() {
        this.defaultYn = "Y";
    }

    public void update(DeliveryAddressDto dto) {
        this.addressAlias   = dto.getAddressAlias();
        this.recipientName  = dto.getRecipientName();
        this.recipientPhone = dto.getRecipientPhone();
        this.zipcode        = dto.getZipcode();
        this.address        = dto.getAddress();
        this.addressDetail  = dto.getAddressDetail();
        this.note           = dto.getNote();
        this.entryCode      = dto.getEntryCode();
        this.defaultYn      = dto.getDefaultYn();
    }

}
