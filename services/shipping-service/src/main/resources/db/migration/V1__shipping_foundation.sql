CREATE TABLE shipment_records (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    created_by VARCHAR(180),
    updated_by VARCHAR(180),
    order_id UUID NOT NULL,
    order_number VARCHAR(64),
    customer_ref VARCHAR(180) NOT NULL,
    shipment_reference VARCHAR(64) NOT NULL,
    carrier_code VARCHAR(64) NOT NULL,
    method_code VARCHAR(64) NOT NULL,
    method_name VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128),
    quoted_amount NUMERIC(14, 4) NOT NULL,
    currency_code VARCHAR(8) NOT NULL,
    external_shipment_id VARCHAR(128),
    tracking_number VARCHAR(128),
    tracking_url VARCHAR(512),
    estimated_delivery_at TIMESTAMPTZ,
    label_created_at TIMESTAMPTZ,
    shipped_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    last_status_update_at TIMESTAMPTZ,
    last_carrier_sync_at TIMESTAMPTZ,
    failure_reason VARCHAR(500),
    recipient_address_json TEXT NOT NULL,
    parcel_summary_json TEXT NOT NULL,
    metadata_json TEXT
);

CREATE UNIQUE INDEX ux_shipment_records_reference
    ON shipment_records (shipment_reference);

CREATE UNIQUE INDEX ux_shipment_records_order_customer_idempotency
    ON shipment_records (order_id, customer_ref, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX ix_shipment_records_order_id_updated_at
    ON shipment_records (order_id, updated_at DESC);

CREATE INDEX ix_shipment_records_customer_ref_updated_at
    ON shipment_records (customer_ref, updated_at DESC);

CREATE INDEX ix_shipment_records_tracking_number
    ON shipment_records (tracking_number);
