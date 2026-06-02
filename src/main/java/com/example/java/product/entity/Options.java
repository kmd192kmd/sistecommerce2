package com.example.java.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    @Column(name = "product_seq", nullable = false)
    private Long productSeq;

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
}
