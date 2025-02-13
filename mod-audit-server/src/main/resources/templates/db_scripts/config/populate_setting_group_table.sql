INSERT INTO setting_group (id, name, description)
VALUES ('audit.inventory',
        'Inventory Audit Configuration',
        'Group of configurations for audit of inventory records: instances, holdings, items, etc.'),
       ('audit.authority',
        'Authority Audit Configuration',
        'Group of configurations for audit of authority records'),
       ('audit.authority',
        'Authority Audit Configuration',
        'Group of configurations for audit of authority records'),
       ('audit.marc',
        'Marc Audit Configuration',
        'Group of configurations for audit of marc records: Bib, Authority')
ON CONFLICT (id) DO NOTHING;
