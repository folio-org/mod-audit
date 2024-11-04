CREATE TABLE IF NOT EXISTS acquisition_invoice_log (
    id uuid PRIMARY KEY,
    action text NOT NULL,
    invoice_id uuid NOT NULL,
    user_id uuid NOT NULL,
    event_date timestamp NOT NULL,
    action_date timestamp NOT NULL,
    modified_content_snapshot jsonb
);

CREATE INDEX IF NOT EXISTS invoice_id_index ON acquisition_invoice_log USING BTREE (invoice_id);
