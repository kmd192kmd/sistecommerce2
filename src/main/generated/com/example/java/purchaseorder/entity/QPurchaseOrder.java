package com.example.java.purchaseorder.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPurchaseOrder is a Querydsl query type for PurchaseOrder
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPurchaseOrder extends EntityPathBase<PurchaseOrder> {

    private static final long serialVersionUID = -2070357858L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPurchaseOrder purchaseOrder = new QPurchaseOrder("purchaseOrder");

    public final DatePath<java.sql.Date> expectedDate = createDate("expectedDate", java.sql.Date.class);

    public final com.example.java.groupbuy.entity.QGroupBuyOptions groupBuyOptions;

    public final com.example.java.product.entity.QOptions options;

    public final DatePath<java.sql.Date> orderDate = createDate("orderDate", java.sql.Date.class);

    public final NumberPath<Integer> quantity = createNumber("quantity", Integer.class);

    public final DatePath<java.sql.Date> receivedDate = createDate("receivedDate", java.sql.Date.class);

    public final NumberPath<Long> seq = createNumber("seq", Long.class);

    public final EnumPath<com.example.java.purchaseorder.enums.PurchaseOrderStatus> status = createEnum("status", com.example.java.purchaseorder.enums.PurchaseOrderStatus.class);

    public final NumberPath<Long> supplyPrice = createNumber("supplyPrice", Long.class);

    public final NumberPath<Long> totalPrice = createNumber("totalPrice", Long.class);

    public final EnumPath<com.example.java.purchaseorder.enums.PurchaseOrderType> type = createEnum("type", com.example.java.purchaseorder.enums.PurchaseOrderType.class);

    public QPurchaseOrder(String variable) {
        this(PurchaseOrder.class, forVariable(variable), INITS);
    }

    public QPurchaseOrder(Path<? extends PurchaseOrder> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPurchaseOrder(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPurchaseOrder(PathMetadata metadata, PathInits inits) {
        this(PurchaseOrder.class, metadata, inits);
    }

    public QPurchaseOrder(Class<? extends PurchaseOrder> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.groupBuyOptions = inits.isInitialized("groupBuyOptions") ? new com.example.java.groupbuy.entity.QGroupBuyOptions(forProperty("groupBuyOptions"), inits.get("groupBuyOptions")) : null;
        this.options = inits.isInitialized("options") ? new com.example.java.product.entity.QOptions(forProperty("options")) : null;
    }

}

