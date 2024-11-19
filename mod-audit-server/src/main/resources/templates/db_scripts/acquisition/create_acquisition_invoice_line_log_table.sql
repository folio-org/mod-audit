CREATE TABLE IF NOT EXISTS acquisition_invoice_line_log (
    id uuid PRIMARY KEY,
    action text NOT NULL,
    invoice_id uuid NOT NULL,
    invoice_line_id uuid NOT NULL,
    user_id uuid NOT NULL,
    event_date timestamp NOT NULL,
    action_date timestamp NOT NULL,
    modified_content_snapshot jsonb
);

CREATE INDEX IF NOT EXISTS invoice_line_id_index ON acquisition_invoice_line_log USING BTREE (invoice_line_id);
