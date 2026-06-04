package com.example.java.groupbuy.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QGroupBuy is a Querydsl query type for GroupBuy
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGroupBuy extends EntityPathBase<GroupBuy> {

    private static final long serialVersionUID = 428369702L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QGroupBuy groupBuy = new QGroupBuy("groupBuy");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> endAt = createDateTime("endAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> finalPrice = createNumber("finalPrice", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> finishedAt = createDateTime("finishedAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> maxCount = createNumber("maxCount", Integer.class);

    public final NumberPath<Integer> minCount = createNumber("minCount", Integer.class);

    public final NumberPath<Integer> originalPrice = createNumber("originalPrice", Integer.class);

    public final com.example.java.product.entity.QProduct product;

    public final NumberPath<Long> seq = createNumber("seq", Long.class);

    public final DateTimePath<java.time.LocalDateTime> startAt = createDateTime("startAt", java.time.LocalDateTime.class);

    public final EnumPath<GroupBuyStatus> status = createEnum("status", GroupBuyStatus.class);

    public QGroupBuy(String variable) {
        this(GroupBuy.class, forVariable(variable), INITS);
    }

    public QGroupBuy(Path<? extends GroupBuy> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QGroupBuy(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QGroupBuy(PathMetadata metadata, PathInits inits) {
        this(GroupBuy.class, metadata, inits);
    }

    public QGroupBuy(Class<? extends GroupBuy> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.product = inits.isInitialized("product") ? new com.example.java.product.entity.QProduct(forProperty("product")) : null;
    }

}

