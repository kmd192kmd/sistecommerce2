package com.example.java.groupbuy.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QWaitingQueue is a Querydsl query type for WaitingQueue
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QWaitingQueue extends EntityPathBase<WaitingQueue> {

    private static final long serialVersionUID = 864743139L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QWaitingQueue waitingQueue = new QWaitingQueue("waitingQueue");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final QGroupBuy groupBuy;

    public final QGroupBuyOptions groupBuyOptions;

    public final NumberPath<Long> memberSeq = createNumber("memberSeq", Long.class);

    public final NumberPath<Long> seq = createNumber("seq", Long.class);

    public QWaitingQueue(String variable) {
        this(WaitingQueue.class, forVariable(variable), INITS);
    }

    public QWaitingQueue(Path<? extends WaitingQueue> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QWaitingQueue(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QWaitingQueue(PathMetadata metadata, PathInits inits) {
        this(WaitingQueue.class, metadata, inits);
    }

    public QWaitingQueue(Class<? extends WaitingQueue> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.groupBuy = inits.isInitialized("groupBuy") ? new QGroupBuy(forProperty("groupBuy"), inits.get("groupBuy")) : null;
        this.groupBuyOptions = inits.isInitialized("groupBuyOptions") ? new QGroupBuyOptions(forProperty("groupBuyOptions"), inits.get("groupBuyOptions")) : null;
    }

}

