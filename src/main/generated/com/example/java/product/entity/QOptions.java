package com.example.java.product.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOptions is a Querydsl query type for Options
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOptions extends EntityPathBase<Options> {

    private static final long serialVersionUID = -1789903059L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOptions options = new QOptions("options");

    public final NumberPath<Integer> additionalPrice = createNumber("additionalPrice", Integer.class);

    public final StringPath color = createString("color");

    public final StringPath connectionType = createString("connectionType");

    public final StringPath materialType = createString("materialType");

    public final StringPath memory = createString("memory");

    public final StringPath optionsSize = createString("optionsSize");

    public final StringPath optionsType = createString("optionsType");

    public final QProduct product;

    public final StringPath quantitySet = createString("quantitySet");

    public final NumberPath<Integer> safetyStock = createNumber("safetyStock", Integer.class);

    public final StringPath scentIngredient = createString("scentIngredient");

    public final NumberPath<Long> seq = createNumber("seq", Long.class);

    public final StringPath sizeSpec = createString("sizeSpec");

    public final NumberPath<Integer> stock = createNumber("stock", Integer.class);

    public final StringPath storageCapacity = createString("storageCapacity");

    public final StringPath storageType = createString("storageType");

    public final StringPath switchAxis = createString("switchAxis");

    public final StringPath taste = createString("taste");

    public final StringPath voltage = createString("voltage");

    public final StringPath volumeWeight = createString("volumeWeight");

    public final StringPath wearableSpec = createString("wearableSpec");

    public QOptions(String variable) {
        this(Options.class, forVariable(variable), INITS);
    }

    public QOptions(Path<? extends Options> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOptions(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOptions(PathMetadata metadata, PathInits inits) {
        this(Options.class, metadata, inits);
    }

    public QOptions(Class<? extends Options> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.product = inits.isInitialized("product") ? new QProduct(forProperty("product")) : null;
    }

}

