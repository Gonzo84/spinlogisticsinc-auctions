-- =============================================================================
-- V003: Add SPC-specific container & equipment categories
-- =============================================================================
-- Maps to SPC product lines used by seed-demo-data.sh and buyer-web frontend.
-- These are root-level (level 0) categories specific to the SPC business.
-- Existing generic categories are preserved for backward compatibility.
-- =============================================================================

INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('30000000-0000-0000-0000-000000000001', NULL, 'Office Containers',        'office-containers',        'icon-office-container',    0, 20),
    ('30000000-0000-0000-0000-000000000002', NULL, 'Shipping Containers',      'shipping-containers',      'icon-shipping-container',  0, 21),
    ('30000000-0000-0000-0000-000000000003', NULL, 'Sanitary Containers',      'sanitary-containers',      'icon-sanitary-container',  0, 22),
    ('30000000-0000-0000-0000-000000000004', NULL, 'Storage Containers',       'storage-containers',       'icon-storage-container',   0, 23),
    ('30000000-0000-0000-0000-000000000005', NULL, 'Modular Structures',       'modular-structures',       'icon-modular',             0, 24),
    ('30000000-0000-0000-0000-000000000006', NULL, 'Climate Control',          'climate-control',          'icon-climate',             0, 25),
    ('30000000-0000-0000-0000-000000000007', NULL, 'Construction Equipment',   'construction-equipment',   'icon-construction-equip',  0, 26),
    ('30000000-0000-0000-0000-000000000008', NULL, 'Fencing & Barriers',       'fencing',                  'icon-fencing',             0, 27)
ON CONFLICT (id) DO NOTHING;
