package com.example.java.product.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "options")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Options {

    @Id
    @Column(name = "seq")
    @SequenceGenerator(name = "options_seq", allocationSize = 1, sequenceName = "options_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "options_seq")
    private Long seq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_seq")
    private Product product;

    @Column(name = "color", length = 100)
    private String color;

    @Column(name = "options_size", length = 100)
    private String optionsSize;

    @Column(name = "volume_weight", length = 100)
    private String volumeWeight;

    @Column(name = "taste", length = 100)
    private String taste;

    @Column(name = "storage_type", length = 100)
    private String storageType;

    @Column(name = "scent_ingredient", length = 100)
    private String scentIngredient;

    @Column(name = "voltage", length = 100)
    private String voltage;

    @Column(name = "quantity_set", length = 100)
    private String quantitySet;

    @Column(name = "size_spec", length = 100)
    private String sizeSpec;

    @Column(name = "storage_capacity", length = 100)
    private String storageCapacity;

    @Column(name = "memory", length = 100)
    private String memory;

    @Column(name = "switch_axis", length = 100)
    private String switchAxis;

    @Column(name = "connection_type", length = 100)
    private String connectionType;

    @Column(name = "wearable_spec", length = 100)
    private String wearableSpec;

    @Column(name = "material_type", length = 100)
    private String materialType;

    @Column(name = "options_type", length = 100)
    private String optionsType;

    @Column(name = "stock", nullable = false)
    @Builder.Default
    private Integer stock = 0;

    @Column(name = "safety_stock", nullable = false)
    @Builder.Default
    private Integer safetyStock = 0;

    @Column(name = "additional_price", nullable = false)
    @Builder.Default
    private Integer additionalPrice = 0;
    
    // 재고 증가 메서드
    public void increaseStock(int quantity) {
        this.stock += quantity;
    }

    // 재고 감소 메서드
    public void decreaseStock(int quantity) {
        if (this.stock < quantity) {
            throw new IllegalArgumentException("재고 부족");
        }

        this.stock -= quantity;
    }
    
    // 옵션 화면 표시용 메서드
    public String getDisplayName() {
        List<String> values = new ArrayList<>();

        if (color != null) values.add(color);
        if (optionsSize != null) values.add(optionsSize);
        if (volumeWeight != null) values.add(volumeWeight);
        if (taste != null) values.add(taste);
        if (storageType != null) values.add(storageType);
        if (scentIngredient != null) values.add(scentIngredient);
        if (voltage != null) values.add(voltage);
        if (quantitySet != null) values.add(quantitySet);
        if (sizeSpec != null) values.add(sizeSpec);
        if (storageCapacity != null) values.add(storageCapacity);
        if (memory != null) values.add(memory);
        if (switchAxis != null) values.add(switchAxis);
        if (connectionType != null) values.add(connectionType);
        if (wearableSpec != null) values.add(wearableSpec);
        if (materialType != null) values.add(materialType);
        if (optionsType != null) values.add(optionsType);

        return String.join(" / ", values);
    }
}
