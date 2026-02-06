CREATE TABLE shipment (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    carrier VARCHAR(64),
    tracking_number VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_shipment_order ON shipment(order_id);
