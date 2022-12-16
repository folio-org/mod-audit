DROP TABLE IF EXISTS acquisition_order_log;

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

--test data--
INSERT INTO acquisition_order_log (id,action,order_id,user_id,user_name,event_date,action_date,modified_content_snapshot) values (
'607ea52c-2c65-4d28-9a8f-e7d236fd6b09',
'CREATE',
'646ea52c-2c65-4d28-9a8f-e7d236fd6b09',
'646ea52c-2c65-4d28-9a8f-e7d236fd6b09',
'TEST',
'2017-01-13T17:09:42.411',
'2017-01-13T17:09:42.411',
'{ "name": "TestOrderType", "value": "TestOrderValue" }'
);

CREATE INDEX IF NOT EXISTS order_id_index ON acquisition_order_log USING BTREE (order_id);
