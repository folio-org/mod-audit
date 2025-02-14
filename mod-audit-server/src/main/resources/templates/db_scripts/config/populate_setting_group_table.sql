INSERT INTO setting_group (id, name, description)
VALUES ('audit.inventory',
        'Inventory Audit Configuration',
        'Group of configurations for audit of inventory records: instances, holdings, items, etc.'),
       ('audit.authority',
        'Authority Audit Configuration',
        'Group of configurations for audit of authority records')
  ON CONFLICT (id) DO NOTHING;
