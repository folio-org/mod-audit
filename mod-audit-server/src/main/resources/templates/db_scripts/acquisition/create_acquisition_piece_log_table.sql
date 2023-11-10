CREATE TABLE IF NOT EXISTS acquisition_piece_log (
    id uuid PRIMARY KEY,
    action text NOT NULL,
    piece_id uuid NOT NULL,
    user_id uuid NOT NULL,
    event_date timestamp NOT NULL,
    action_date timestamp NOT NULL,
    modified_content_snapshot jsonb
);

CREATE INDEX IF NOT EXISTS piece_id_index ON acquisition_piece_log USING BTREE (piece_id);
