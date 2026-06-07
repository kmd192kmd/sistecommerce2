package com.example.java.adminpayment.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAdminPayment is a Querydsl query type for AdminPayment
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAdminPayment extends EntityPathBase<AdminPayment> {

    private static final long serialVersionUID = 1160714886L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAdminPayment adminPayment = new QAdminPayment("adminPayment");

    public final com.example.java.purchaseorder.entity.QPurchaseOrder purchaseOrder;

    public final NumberPath<Long> seq = createNumber("seq", Long.class);

    public final NumberPath<Integer> status = createNumber("status", Integer.class);

    public final EnumPath<com.example.java.adminpayment.enums.PaymentType> type = createEnum("type", com.example.java.adminpayment.enums.PaymentType.class);

    public QAdminPayment(String variable) {
        this(AdminPayment.class, forVariable(variable), INITS);
    }

    public QAdminPayment(Path<? extends AdminPayment> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAdminPayment(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAdminPayment(PathMetadata metadata, PathInits inits) {
        this(AdminPayment.class, metadata, inits);
    }

    public QAdminPayment(Class<? extends AdminPayment> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.purchaseOrder = inits.isInitialized("purchaseOrder") ? new com.example.java.purchaseorder.entity.QPurchaseOrder(forProperty("purchaseOrder"), inits.get("purchaseOrder")) : null;
    }

}

