CREATE TABLE IF NOT EXISTS acquisition_order_log (
    id uuid PRIMARY KEY,
    action text NOT NULL,
    order_id uuid NOT NULL,
    user_id uuid NOT NULL,
    user_name text NOT NULL,
    event_date timestamp NOT NULL,
    action_date timestamp NOT NULL,
    modified_content_snapshot jsonb
);

CREATE INDEX IF NOT EXISTS order_id_index ON acquisition_order_log USING BTREE (order_id);
