package com.example.java.groupbuy.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QParticipation is a Querydsl query type for Participation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QParticipation extends EntityPathBase<Participation> {

    private static final long serialVersionUID = 456826114L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QParticipation participation = new QParticipation("participation");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final QGroupBuy groupBuy;

    public final QGroupBuyOptions groupBuyOptions;

    public final NumberPath<Long> memberSeq = createNumber("memberSeq", Long.class);

    public final DateTimePath<java.time.LocalDateTime> paymentDeadline = createDateTime("paymentDeadline", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> promotedAt = createDateTime("promotedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> seq = createNumber("seq", Long.class);

    public final EnumPath<ParticipationStatus> status = createEnum("status", ParticipationStatus.class);

    public QParticipation(String variable) {
        this(Participation.class, forVariable(variable), INITS);
    }

    public QParticipation(Path<? extends Participation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QParticipation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QParticipation(PathMetadata metadata, PathInits inits) {
        this(Participation.class, metadata, inits);
    }

    public QParticipation(Class<? extends Participation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.groupBuy = inits.isInitialized("groupBuy") ? new QGroupBuy(forProperty("groupBuy"), inits.get("groupBuy")) : null;
        this.groupBuyOptions = inits.isInitialized("groupBuyOptions") ? new QGroupBuyOptions(forProperty("groupBuyOptions"), inits.get("groupBuyOptions")) : null;
    }

}

