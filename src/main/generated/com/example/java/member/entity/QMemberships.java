package com.example.java.member.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMemberships is a Querydsl query type for Memberships
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMemberships extends EntityPathBase<Memberships> {

    private static final long serialVersionUID = 522875595L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMemberships memberships = new QMemberships("memberships");

    public final StringPath billingKey = createString("billingKey");

    public final DateTimePath<java.time.LocalDateTime> canceledAt = createDateTime("canceledAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> expireAt = createDateTime("expireAt", java.time.LocalDateTime.class);

    public final QMember member;

    public final DateTimePath<java.time.LocalDateTime> nextBillingAt = createDateTime("nextBillingAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> seq = createNumber("seq", Long.class);

    public final DateTimePath<java.time.LocalDateTime> startedAt = createDateTime("startedAt", java.time.LocalDateTime.class);

    public final StringPath status = createString("status");

    public QMemberships(String variable) {
        this(Memberships.class, forVariable(variable), INITS);
    }

    public QMemberships(Path<? extends Memberships> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMemberships(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMemberships(PathMetadata metadata, PathInits inits) {
        this(Memberships.class, metadata, inits);
    }

    public QMemberships(Class<? extends Memberships> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new QMember(forProperty("member")) : null;
    }

}

