CREATE TABLE IF NOT EXISTS acquisition_organization_log (
    id uuid PRIMARY KEY,
    action text NOT NULL,
    organization_id uuid NOT NULL,
    user_id uuid NOT NULL,
    event_date timestamp NOT NULL,
    action_date timestamp NOT NULL,
    modified_content_snapshot jsonb
);

CREATE INDEX IF NOT EXISTS organization_id_index ON acquisition_organization_log USING BTREE (organization_id);
