CREATE TABLE IF NOT EXISTS setting
(
    id           varchar(100) PRIMARY KEY,
    key          varchar(50) NOT NULL,
    value        jsonb       NOT NULL,
    type         varchar(50) NOT NULL,
    description  varchar(500),
    group_id     varchar(50) NOT NULL REFERENCES setting_group (id),
    created_date timestamp without time zone,
    created_by   uuid,
    updated_date timestamp without time zone,
    updated_by   uuid
);