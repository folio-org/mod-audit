CREATE TABLE IF NOT EXISTS acquisition_order_line_log (
    id uuid PRIMARY KEY,
    action text NOT NULL,
    order_id uuid NOT NULL,
    order_line_id uuid NOT NULL,
    user_id uuid NOT NULL,
    event_date timestamp NOT NULL,
    action_date timestamp NOT NULL,
    modified_content_snapshot jsonb
);

CREATE INDEX IF NOT EXISTS order_line_id_index ON acquisition_order_line_log USING BTREE (order_line_id);
