package com.example.java.member.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QNotificationPreference is a Querydsl query type for NotificationPreference
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNotificationPreference extends EntityPathBase<NotificationPreference> {

    private static final long serialVersionUID = 1550313464L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QNotificationPreference notificationPreference = new QNotificationPreference("notificationPreference");

    public final StringPath emailYn = createString("emailYn");

    public final StringPath marketingEmailYn = createString("marketingEmailYn");

    public final StringPath marketingSmsYn = createString("marketingSmsYn");

    public final QMember member;

    public final StringPath pushYn = createString("pushYn");

    public final NumberPath<Long> seq = createNumber("seq", Long.class);

    public final StringPath smsYn = createString("smsYn");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QNotificationPreference(String variable) {
        this(NotificationPreference.class, forVariable(variable), INITS);
    }

    public QNotificationPreference(Path<? extends NotificationPreference> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QNotificationPreference(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QNotificationPreference(PathMetadata metadata, PathInits inits) {
        this(NotificationPreference.class, metadata, inits);
    }

    public QNotificationPreference(Class<? extends NotificationPreference> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new QMember(forProperty("member")) : null;
    }

}

