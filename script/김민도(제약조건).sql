--배송
ALTER TABLE delivery
ADD CONSTRAINT ck_delivery_target
CHECK (
    (orders_seq IS NOT NULL AND purchase_order_seq IS NULL)
 OR (orders_seq IS NULL AND purchase_order_seq IS NOT NULL)
);

ALTER TABLE delivery
ADD CONSTRAINT ck_delivery_status
CHECK (
    status IN (
        'READY',
        'SHIPPING',
        'DELIVERED',
        'FAILED',
        'DELAYED',
        'CANCELED'
    )
);

ALTER TABLE delivery
ADD CONSTRAINT ck_delivery_fee
CHECK (
    distance_surcharge >= 0
    AND total_delivery_fee >= 0
);

--배송기록
ALTER TABLE delivery_history
ADD CONSTRAINT ck_delivery_history_lat
CHECK (
    curr_latitude BETWEEN -90 AND 90
);

ALTER TABLE delivery_history
ADD CONSTRAINT ck_delivery_history_lng
CHECK (
    curr_longitude BETWEEN -180 AND 180
);

--물류허브
ALTER TABLE hub
ADD CONSTRAINT ck_hub_lat
CHECK (
    latitude BETWEEN -90 AND 90
);

ALTER TABLE hub
ADD CONSTRAINT ck_hub_lng
CHECK (
    longitude BETWEEN -180 AND 180
);

--택배사
ALTER TABLE delivery_company
ADD CONSTRAINT ck_delivery_company_fee
CHECK (
    base_delivery_fee >= 0
    AND monthly_fee >= 0
);

--반품신청
ALTER TABLE return_request
ADD CONSTRAINT ck_return_request_status
CHECK (
    status IN (
        0, -- 신청
        1, -- 승인
        2, -- 수거중
        3, -- 완료
        4  -- 거절
    )
);

ALTER TABLE return_request
ADD CONSTRAINT ck_return_request_qty
CHECK (
    return_quantity > 0
);

--반품
ALTER TABLE returns
ADD CONSTRAINT ck_returns_status
CHECK (
    status IN (
        'READY',
        'RETURNING',
        'RETURNED'
    )
);

--카테고리
ALTER TABLE category
ADD CONSTRAINT ck_category_depth
CHECK (
    depth_level IN (0,1,2)
);

ALTER TABLE category
ADD CONSTRAINT uq_category_name
UNIQUE(parent_seq, category_name);