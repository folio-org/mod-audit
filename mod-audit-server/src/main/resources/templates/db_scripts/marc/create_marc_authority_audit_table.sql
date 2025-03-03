-- main audit table
CREATE TABLE IF NOT EXISTS marc_authority_audit (
    event_id   uuid      NOT NULL,
    event_date timestamp NOT NULL,
    entity_id  uuid      NOT NULL,
    origin     varchar   NOT NULL,
    action     varchar   NOT NULL,
    user_id    uuid      NOT NULL,
    diff       jsonb,
    PRIMARY KEY (event_id, event_date, entity_id)
) PARTITION BY HASH (entity_id);

-- main partitions
CREATE TABLE IF NOT EXISTS marc_authority_audit_p0 PARTITION OF marc_authority_audit FOR VALUES WITH (MODULUS 8, REMAINDER 0) PARTITION BY RANGE (event_date);
CREATE TABLE IF NOT EXISTS marc_authority_audit_p1 PARTITION OF marc_authority_audit FOR VALUES WITH (MODULUS 8, REMAINDER 1) PARTITION BY RANGE (event_date);
CREATE TABLE IF NOT EXISTS marc_authority_audit_p2 PARTITION OF marc_authority_audit FOR VALUES WITH (MODULUS 8, REMAINDER 2) PARTITION BY RANGE (event_date);
CREATE TABLE IF NOT EXISTS marc_authority_audit_p3 PARTITION OF marc_authority_audit FOR VALUES WITH (MODULUS 8, REMAINDER 3) PARTITION BY RANGE (event_date);
CREATE TABLE IF NOT EXISTS marc_authority_audit_p4 PARTITION OF marc_authority_audit FOR VALUES WITH (MODULUS 8, REMAINDER 4) PARTITION BY RANGE (event_date);
CREATE TABLE IF NOT EXISTS marc_authority_audit_p5 PARTITION OF marc_authority_audit FOR VALUES WITH (MODULUS 8, REMAINDER 5) PARTITION BY RANGE (event_date);
CREATE TABLE IF NOT EXISTS marc_authority_audit_p6 PARTITION OF marc_authority_audit FOR VALUES WITH (MODULUS 8, REMAINDER 6) PARTITION BY RANGE (event_date);
CREATE TABLE IF NOT EXISTS marc_authority_audit_p7 PARTITION OF marc_authority_audit FOR VALUES WITH (MODULUS 8, REMAINDER 7) PARTITION BY RANGE (event_date);

-- sub-partitions
CREATE TABLE IF NOT EXISTS marc_authority_audit_p0_2025_q1 PARTITION OF marc_authority_audit_p0 FOR VALUES FROM ('2025-01-01') TO ('2025-04-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p0_2025_q2 PARTITION OF marc_authority_audit_p0 FOR VALUES FROM ('2025-04-01') TO ('2025-07-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p0_2025_q3 PARTITION OF marc_authority_audit_p0 FOR VALUES FROM ('2025-07-01') TO ('2025-10-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p0_2025_q4 PARTITION OF marc_authority_audit_p0 FOR VALUES FROM ('2025-10-01') TO ('2026-01-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p1_2025_q1 PARTITION OF marc_authority_audit_p1 FOR VALUES FROM ('2025-01-01') TO ('2025-04-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p1_2025_q2 PARTITION OF marc_authority_audit_p1 FOR VALUES FROM ('2025-04-01') TO ('2025-07-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p1_2025_q3 PARTITION OF marc_authority_audit_p1 FOR VALUES FROM ('2025-07-01') TO ('2025-10-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p1_2025_q4 PARTITION OF marc_authority_audit_p1 FOR VALUES FROM ('2025-10-01') TO ('2026-01-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p2_2025_q1 PARTITION OF marc_authority_audit_p2 FOR VALUES FROM ('2025-01-01') TO ('2025-04-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p2_2025_q2 PARTITION OF marc_authority_audit_p2 FOR VALUES FROM ('2025-04-01') TO ('2025-07-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p2_2025_q3 PARTITION OF marc_authority_audit_p2 FOR VALUES FROM ('2025-07-01') TO ('2025-10-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p2_2025_q4 PARTITION OF marc_authority_audit_p2 FOR VALUES FROM ('2025-10-01') TO ('2026-01-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p3_2025_q1 PARTITION OF marc_authority_audit_p3 FOR VALUES FROM ('2025-01-01') TO ('2025-04-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p3_2025_q2 PARTITION OF marc_authority_audit_p3 FOR VALUES FROM ('2025-04-01') TO ('2025-07-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p3_2025_q3 PARTITION OF marc_authority_audit_p3 FOR VALUES FROM ('2025-07-01') TO ('2025-10-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p3_2025_q4 PARTITION OF marc_authority_audit_p3 FOR VALUES FROM ('2025-10-01') TO ('2026-01-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p4_2025_q1 PARTITION OF marc_authority_audit_p4 FOR VALUES FROM ('2025-01-01') TO ('2025-04-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p4_2025_q2 PARTITION OF marc_authority_audit_p4 FOR VALUES FROM ('2025-04-01') TO ('2025-07-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p4_2025_q3 PARTITION OF marc_authority_audit_p4 FOR VALUES FROM ('2025-07-01') TO ('2025-10-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p4_2025_q4 PARTITION OF marc_authority_audit_p4 FOR VALUES FROM ('2025-10-01') TO ('2026-01-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p5_2025_q1 PARTITION OF marc_authority_audit_p5 FOR VALUES FROM ('2025-01-01') TO ('2025-04-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p5_2025_q2 PARTITION OF marc_authority_audit_p5 FOR VALUES FROM ('2025-04-01') TO ('2025-07-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p5_2025_q3 PARTITION OF marc_authority_audit_p5 FOR VALUES FROM ('2025-07-01') TO ('2025-10-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p5_2025_q4 PARTITION OF marc_authority_audit_p5 FOR VALUES FROM ('2025-10-01') TO ('2026-01-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p6_2025_q1 PARTITION OF marc_authority_audit_p6 FOR VALUES FROM ('2025-01-01') TO ('2025-04-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p6_2025_q2 PARTITION OF marc_authority_audit_p6 FOR VALUES FROM ('2025-04-01') TO ('2025-07-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p6_2025_q3 PARTITION OF marc_authority_audit_p6 FOR VALUES FROM ('2025-07-01') TO ('2025-10-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p6_2025_q4 PARTITION OF marc_authority_audit_p6 FOR VALUES FROM ('2025-10-01') TO ('2026-01-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p7_2025_q1 PARTITION OF marc_authority_audit_p7 FOR VALUES FROM ('2025-01-01') TO ('2025-04-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p7_2025_q2 PARTITION OF marc_authority_audit_p7 FOR VALUES FROM ('2025-04-01') TO ('2025-07-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p7_2025_q3 PARTITION OF marc_authority_audit_p7 FOR VALUES FROM ('2025-07-01') TO ('2025-10-01');
CREATE TABLE IF NOT EXISTS marc_authority_audit_p7_2025_q4 PARTITION OF marc_authority_audit_p7 FOR VALUES FROM ('2025-10-01') TO ('2026-01-01');
