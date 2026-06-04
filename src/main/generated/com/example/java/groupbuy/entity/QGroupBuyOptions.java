package com.example.java.groupbuy.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QGroupBuyOptions is a Querydsl query type for GroupBuyOptions
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGroupBuyOptions extends EntityPathBase<GroupBuyOptions> {

    private static final long serialVersionUID = -1162772328L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QGroupBuyOptions groupBuyOptions = new QGroupBuyOptions("groupBuyOptions");

    public final QGroupBuy groupBuy;

    public final NumberPath<Integer> occupiedCount = createNumber("occupiedCount", Integer.class);

    public final com.example.java.product.entity.QOptions options;

    public final NumberPath<Integer> orderQty = createNumber("orderQty", Integer.class);

    public final NumberPath<Long> seq = createNumber("seq", Long.class);

    public QGroupBuyOptions(String variable) {
        this(GroupBuyOptions.class, forVariable(variable), INITS);
    }

    public QGroupBuyOptions(Path<? extends GroupBuyOptions> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QGroupBuyOptions(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QGroupBuyOptions(PathMetadata metadata, PathInits inits) {
        this(GroupBuyOptions.class, metadata, inits);
    }

    public QGroupBuyOptions(Class<? extends GroupBuyOptions> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.groupBuy = inits.isInitialized("groupBuy") ? new QGroupBuy(forProperty("groupBuy"), inits.get("groupBuy")) : null;
        this.options = inits.isInitialized("options") ? new com.example.java.product.entity.QOptions(forProperty("options")) : null;
    }

}

