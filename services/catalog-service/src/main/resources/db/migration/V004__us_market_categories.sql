-- =============================================================================
-- V004: US Market Category Taxonomy
-- =============================================================================
-- Replaces the old EU/SPC categories (V001 roots + V001 subs + V003 SPC) with
-- a new 18-category taxonomy designed for the US B2B equipment market.
--
-- Strategy:
--   1. Deactivate all existing categories (SET active = FALSE) to preserve
--      FK integrity with lots already referencing them.
--   2. Rename colliding slugs on old categories (append '-legacy') so the
--      UNIQUE slug constraint does not block new inserts.
--   3. Insert 18 new root categories  (40000000-0000-0000-0000-0000000000XX)
--   4. Insert subcategories            (41000000-0000-0000-0000-0000000000XX)
--   5. All inserts use ON CONFLICT (id) DO NOTHING for idempotent re-runs.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Step 1: Deactivate ALL old categories
-- -----------------------------------------------------------------------------
UPDATE app.categories SET active = FALSE WHERE active = TRUE;

-- -----------------------------------------------------------------------------
-- Step 1b: Rename slugs on old (deactivated) categories that collide with new
--          taxonomy slugs. Appends '-legacy' suffix to free up the slug value.
-- -----------------------------------------------------------------------------
UPDATE app.categories
   SET slug = slug || '-legacy'
 WHERE id::text LIKE '10000000-%'
    OR id::text LIKE '20000000-%'
    OR id::text LIKE '30000000-%';

-- -----------------------------------------------------------------------------
-- Step 2: Insert 18 root categories (level 0)
-- -----------------------------------------------------------------------------
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('40000000-0000-0000-0000-000000000001', NULL, 'Containers & Modular Structures',  'containers-modular',     'icon-containers',       0,  1),
    ('40000000-0000-0000-0000-000000000002', NULL, 'Construction Equipment',            'construction-equipment', 'icon-construction',     0,  2),
    ('40000000-0000-0000-0000-000000000003', NULL, 'Cranes & Lifting',                  'cranes-lifting',         'icon-cranes',           0,  3),
    ('40000000-0000-0000-0000-000000000004', NULL, 'Aerial Work Platforms',              'aerial-platforms',       'icon-aerial',           0,  4),
    ('40000000-0000-0000-0000-000000000005', NULL, 'Material Handling & Warehouse',      'material-handling',      'icon-material',         0,  5),
    ('40000000-0000-0000-0000-000000000006', NULL, 'Trucks',                             'trucks',                 'icon-trucks',           0,  6),
    ('40000000-0000-0000-0000-000000000007', NULL, 'Trailers',                           'trailers',               'icon-trailers',         0,  7),
    ('40000000-0000-0000-0000-000000000008', NULL, 'Agriculture Equipment',              'agriculture',            'icon-agriculture',      0,  8),
    ('40000000-0000-0000-0000-000000000009', NULL, 'Forestry & Logging',                 'forestry-logging',       'icon-forestry',         0,  9),
    ('40000000-0000-0000-0000-000000000010', NULL, 'Mining, Quarry & Aggregate',         'mining-quarry',          'icon-mining',           0, 10),
    ('40000000-0000-0000-0000-000000000011', NULL, 'Oil & Gas Equipment',                'oil-gas',                'icon-oilgas',           0, 11),
    ('40000000-0000-0000-0000-000000000012', NULL, 'Power & Climate Control',            'power-climate',          'icon-power',            0, 12),
    ('40000000-0000-0000-0000-000000000013', NULL, 'Metalworking & Fabrication',         'metalworking',           'icon-metalworking',     0, 13),
    ('40000000-0000-0000-0000-000000000014', NULL, 'Woodworking & Plastics',             'woodworking-plastics',   'icon-woodworking',      0, 14),
    ('40000000-0000-0000-0000-000000000015', NULL, 'Food Processing & Packaging',        'food-processing',        'icon-food',             0, 15),
    ('40000000-0000-0000-0000-000000000016', NULL, 'Medical, Lab & Pharmaceutical',      'medical-lab',            'icon-medical',          0, 16),
    ('40000000-0000-0000-0000-000000000017', NULL, 'Vehicles & Fleet',                   'vehicles-fleet',         'icon-vehicles',         0, 17),
    ('40000000-0000-0000-0000-000000000018', NULL, 'Attachments, Parts & Tools',         'attachments-parts',      'icon-attachments',      0, 18)
ON CONFLICT (id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Step 3: Insert subcategories (level 1)
-- -----------------------------------------------------------------------------

-- Root 01: Containers & Modular Structures
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('41000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'Office Containers',        'office-containers',        NULL, 1, 1),
    ('41000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000001', 'Shipping Containers',      'shipping-containers',      NULL, 1, 2),
    ('41000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000001', 'Storage Containers',       'storage-containers',       NULL, 1, 3),
    ('41000000-0000-0000-0000-000000000004', '40000000-0000-0000-0000-000000000001', 'Sanitary Containers',      'sanitary-containers',      NULL, 1, 4),
    ('41000000-0000-0000-0000-000000000005', '40000000-0000-0000-0000-000000000001', 'Modular Buildings',        'modular-buildings',        NULL, 1, 5),
    ('41000000-0000-0000-0000-000000000006', '40000000-0000-0000-0000-000000000001', 'Specialized Containers',   'specialized-containers',   NULL, 1, 6),
    ('41000000-0000-0000-0000-000000000007', '40000000-0000-0000-0000-000000000001', 'Container Accessories',    'container-accessories',    NULL, 1, 7)
ON CONFLICT (id) DO NOTHING;

-- Root 02: Construction Equipment
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('41000000-0000-0000-0000-000000000008', '40000000-0000-0000-0000-000000000002', 'Excavators',               'excavators',               NULL, 1, 1),
    ('41000000-0000-0000-0000-000000000009', '40000000-0000-0000-0000-000000000002', 'Loaders',                  'loaders',                  NULL, 1, 2),
    ('41000000-0000-0000-0000-000000000010', '40000000-0000-0000-0000-000000000002', 'Dozers',                   'dozers',                   NULL, 1, 3),
    ('41000000-0000-0000-0000-000000000011', '40000000-0000-0000-0000-000000000002', 'Graders',                  'graders',                  NULL, 1, 4),
    ('41000000-0000-0000-0000-000000000012', '40000000-0000-0000-0000-000000000002', 'Compaction Equipment',     'compaction-equipment',     NULL, 1, 5),
    ('41000000-0000-0000-0000-000000000013', '40000000-0000-0000-0000-000000000002', 'Paving & Asphalt',         'paving-asphalt',           NULL, 1, 6),
    ('41000000-0000-0000-0000-000000000014', '40000000-0000-0000-0000-000000000002', 'Concrete Equipment',       'concrete-equipment',       NULL, 1, 7),
    ('41000000-0000-0000-0000-000000000015', '40000000-0000-0000-0000-000000000002', 'Scrapers',                 'scrapers',                 NULL, 1, 8),
    ('41000000-0000-0000-0000-000000000016', '40000000-0000-0000-0000-000000000002', 'Boring & Trenching',       'boring-trenching',         NULL, 1, 9),
    ('41000000-0000-0000-0000-000000000017', '40000000-0000-0000-0000-000000000002', 'Demolition & Recycling',   'demolition-recycling',     NULL, 1, 10),
    ('41000000-0000-0000-0000-000000000018', '40000000-0000-0000-0000-000000000002', 'Pile Driving',             'pile-driving',             NULL, 1, 11)
ON CONFLICT (id) DO NOTHING;

-- Root 03: Cranes & Lifting
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('41000000-0000-0000-0000-000000000019', '40000000-0000-0000-0000-000000000003', 'Mobile Cranes',            'mobile-cranes',            NULL, 1, 1),
    ('41000000-0000-0000-0000-000000000020', '40000000-0000-0000-0000-000000000003', 'Tower Cranes',             'tower-cranes',             NULL, 1, 2),
    ('41000000-0000-0000-0000-000000000021', '40000000-0000-0000-0000-000000000003', 'Boom Trucks',              'boom-trucks',              NULL, 1, 3),
    ('41000000-0000-0000-0000-000000000022', '40000000-0000-0000-0000-000000000003', 'Overhead Cranes',          'overhead-cranes',          NULL, 1, 4),
    ('41000000-0000-0000-0000-000000000023', '40000000-0000-0000-0000-000000000003', 'Carry Deck Cranes',        'carry-deck-cranes',        NULL, 1, 5),
    ('41000000-0000-0000-0000-000000000024', '40000000-0000-0000-0000-000000000003', 'Crane Attachments',        'crane-attachments',        NULL, 1, 6)
ON CONFLICT (id) DO NOTHING;

-- Root 04: Aerial Work Platforms
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('41000000-0000-0000-0000-000000000025', '40000000-0000-0000-0000-000000000004', 'Boom Lifts',               'boom-lifts',               NULL, 1, 1),
    ('41000000-0000-0000-0000-000000000026', '40000000-0000-0000-0000-000000000004', 'Scissor Lifts',            'scissor-lifts',            NULL, 1, 2),
    ('41000000-0000-0000-0000-000000000027', '40000000-0000-0000-0000-000000000004', 'Telehandlers',             'telehandlers',             NULL, 1, 3),
    ('41000000-0000-0000-0000-000000000028', '40000000-0000-0000-0000-000000000004', 'Vertical Mast Lifts',      'vertical-mast-lifts',      NULL, 1, 4),
    ('41000000-0000-0000-0000-000000000029', '40000000-0000-0000-0000-000000000004', 'Scaffolding',              'scaffolding',              NULL, 1, 5)
ON CONFLICT (id) DO NOTHING;

-- Root 05: Material Handling & Warehouse
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('41000000-0000-0000-0000-000000000030', '40000000-0000-0000-0000-000000000005', 'Forklifts',                'forklifts',                NULL, 1, 1),
    ('41000000-0000-0000-0000-000000000031', '40000000-0000-0000-0000-000000000005', 'Rough Terrain Forklifts',  'rough-terrain-forklifts',  NULL, 1, 2),
    ('41000000-0000-0000-0000-000000000032', '40000000-0000-0000-0000-000000000005', 'Container Handlers',       'container-handlers',       NULL, 1, 3),
    ('41000000-0000-0000-0000-000000000033', '40000000-0000-0000-0000-000000000005', 'Conveyors',                'conveyors',                NULL, 1, 4),
    ('41000000-0000-0000-0000-000000000034', '40000000-0000-0000-0000-000000000005', 'Racking & Shelving',       'racking-shelving',         NULL, 1, 5),
    ('41000000-0000-0000-0000-000000000035', '40000000-0000-0000-0000-000000000005', 'Dock Equipment',           'dock-equipment',           NULL, 1, 6),
    ('41000000-0000-0000-0000-000000000036', '40000000-0000-0000-0000-000000000005', 'Pallet Jacks',             'pallet-jacks',             NULL, 1, 7)
ON CONFLICT (id) DO NOTHING;

-- Root 06: Trucks
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('41000000-0000-0000-0000-000000000037', '40000000-0000-0000-0000-000000000006', 'Heavy Duty Trucks',        'heavy-duty-trucks',        NULL, 1, 1),
    ('41000000-0000-0000-0000-000000000038', '40000000-0000-0000-0000-000000000006', 'Medium Duty Trucks',       'medium-duty-trucks',       NULL, 1, 2),
    ('41000000-0000-0000-0000-000000000039', '40000000-0000-0000-0000-000000000006', 'Dump Trucks',              'dump-trucks',              NULL, 1, 3),
    ('41000000-0000-0000-0000-000000000040', '40000000-0000-0000-0000-000000000006', 'Water Trucks',             'water-trucks',             NULL, 1, 4),
    ('41000000-0000-0000-0000-000000000041', '40000000-0000-0000-0000-000000000006', 'Service & Utility Trucks', 'service-utility-trucks',   NULL, 1, 5),
    ('41000000-0000-0000-0000-000000000042', '40000000-0000-0000-0000-000000000006', 'Bucket Trucks',            'bucket-trucks',            NULL, 1, 6),
    ('41000000-0000-0000-0000-000000000043', '40000000-0000-0000-0000-000000000006', 'Vacuum & Sewer Trucks',    'vacuum-sewer-trucks',      NULL, 1, 7),
    ('41000000-0000-0000-0000-000000000044', '40000000-0000-0000-0000-000000000006', 'Refuse Trucks',            'refuse-trucks',            NULL, 1, 8),
    ('41000000-0000-0000-0000-000000000045', '40000000-0000-0000-0000-000000000006', 'Mixer Trucks',             'mixer-trucks',             NULL, 1, 9),
    ('41000000-0000-0000-0000-000000000046', '40000000-0000-0000-0000-000000000006', 'Emergency Vehicles',       'emergency-vehicles',       NULL, 1, 10),
    ('41000000-0000-0000-0000-000000000047', '40000000-0000-0000-0000-000000000006', 'Tow & Recovery',           'tow-recovery',             NULL, 1, 11)
ON CONFLICT (id) DO NOTHING;

-- Root 07: Trailers
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('41000000-0000-0000-0000-000000000048', '40000000-0000-0000-0000-000000000007', 'Flatbed & Drop Deck',      'flatbed-drop-deck',        NULL, 1, 1),
    ('41000000-0000-0000-0000-000000000049', '40000000-0000-0000-0000-000000000007', 'Dry Van',                  'dry-van',                  NULL, 1, 2),
    ('41000000-0000-0000-0000-000000000050', '40000000-0000-0000-0000-000000000007', 'Refrigerated Trailers',    'refrigerated-trailers',    NULL, 1, 3),
    ('41000000-0000-0000-0000-000000000051', '40000000-0000-0000-0000-000000000007', 'Dump Trailers',            'dump-trailers',            NULL, 1, 4),
    ('41000000-0000-0000-0000-000000000052', '40000000-0000-0000-0000-000000000007', 'Tank Trailers',            'tank-trailers',            NULL, 1, 5),
    ('41000000-0000-0000-0000-000000000053', '40000000-0000-0000-0000-000000000007', 'Equipment Trailers',       'equipment-trailers',       NULL, 1, 6),
    ('41000000-0000-0000-0000-000000000054', '40000000-0000-0000-0000-000000000007', 'Car Carrier Trailers',     'car-carrier-trailers',     NULL, 1, 7),
    ('41000000-0000-0000-0000-000000000055', '40000000-0000-0000-0000-000000000007', 'Specialty Trailers',       'specialty-trailers',       NULL, 1, 8),
    ('41000000-0000-0000-0000-000000000056', '40000000-0000-0000-0000-000000000007', 'Utility & Cargo',          'utility-cargo',            NULL, 1, 9)
ON CONFLICT (id) DO NOTHING;

-- Root 08: Agriculture Equipment
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('41000000-0000-0000-0000-000000000057', '40000000-0000-0000-0000-000000000008', 'Tractors',                 'tractors',                 NULL, 1, 1),
    ('41000000-0000-0000-0000-000000000058', '40000000-0000-0000-0000-000000000008', 'Combines & Harvesters',    'combines-harvesters',      NULL, 1, 2),
    ('41000000-0000-0000-0000-000000000059', '40000000-0000-0000-0000-000000000008', 'Headers & Platforms',      'headers-platforms',        NULL, 1, 3),
    ('41000000-0000-0000-0000-000000000060', '40000000-0000-0000-0000-000000000008', 'Tillage Equipment',        'tillage-equipment',        NULL, 1, 4),
    ('41000000-0000-0000-0000-000000000061', '40000000-0000-0000-0000-000000000008', 'Planting & Seeding',       'planting-seeding',         NULL, 1, 5),
    ('41000000-0000-0000-0000-000000000062', '40000000-0000-0000-0000-000000000008', 'Hay & Forage',             'hay-forage',               NULL, 1, 6),
    ('41000000-0000-0000-0000-000000000063', '40000000-0000-0000-0000-000000000008', 'Sprayers & Applicators',   'sprayers-applicators',     NULL, 1, 7),
    ('41000000-0000-0000-0000-000000000064', '40000000-0000-0000-0000-000000000008', 'Grain Handling',           'grain-handling',           NULL, 1, 8),
    ('41000000-0000-0000-0000-000000000065', '40000000-0000-0000-0000-000000000008', 'Irrigation',               'irrigation',               NULL, 1, 9),
    ('41000000-0000-0000-0000-000000000066', '40000000-0000-0000-0000-000000000008', 'Livestock Equipment',      'livestock-equipment',      NULL, 1, 10)
ON CONFLICT (id) DO NOTHING;

-- Root 09: Forestry & Logging
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('41000000-0000-0000-0000-000000000067', '40000000-0000-0000-0000-000000000009', 'Feller Bunchers',          'feller-bunchers',          NULL, 1, 1),
    ('41000000-0000-0000-0000-000000000068', '40000000-0000-0000-0000-000000000009', 'Skidders',                 'skidders',                 NULL, 1, 2),
    ('41000000-0000-0000-0000-000000000069', '40000000-0000-0000-0000-000000000009', 'Forwarders',               'forwarders',               NULL, 1, 3),
    ('41000000-0000-0000-0000-000000000070', '40000000-0000-0000-0000-000000000009', 'Timber Harvesters',        'timber-harvesters',        NULL, 1, 4),
    ('41000000-0000-0000-0000-000000000071', '40000000-0000-0000-0000-000000000009', 'Log Loaders',              'log-loaders',              NULL, 1, 5),
    ('41000000-0000-0000-0000-000000000072', '40000000-0000-0000-0000-000000000009', 'Chippers & Grinders',      'chippers-grinders',        NULL, 1, 6),
    ('41000000-0000-0000-0000-000000000073', '40000000-0000-0000-0000-000000000009', 'Sawmill Equipment',        'sawmill-equipment',        NULL, 1, 7)
ON CONFLICT (id) DO NOTHING;

-- Root 10: Mining, Quarry & Aggregate
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('41000000-0000-0000-0000-000000000074', '40000000-0000-0000-0000-000000000010', 'Crushers',                 'crushers',                 NULL, 1, 1),
    ('41000000-0000-0000-0000-000000000075', '40000000-0000-0000-0000-000000000010', 'Screening Equipment',      'screening-equipment',      NULL, 1, 2),
    ('41000000-0000-0000-0000-000000000076', '40000000-0000-0000-0000-000000000010', 'Washing & Classifying',    'washing-classifying',      NULL, 1, 3),
    ('41000000-0000-0000-0000-000000000077', '40000000-0000-0000-0000-000000000010', 'Drilling Rigs',            'drilling-rigs',            NULL, 1, 4),
    ('41000000-0000-0000-0000-000000000078', '40000000-0000-0000-0000-000000000010', 'Haul Trucks',              'haul-trucks',              NULL, 1, 5),
    ('41000000-0000-0000-0000-000000000079', '40000000-0000-0000-0000-000000000010', 'Conveyor Systems',         'conveyor-systems',         NULL, 1, 6),
    ('41000000-0000-0000-0000-000000000080', '40000000-0000-0000-0000-000000000010', 'Draglines & Dredging',     'draglines-dredging',       NULL, 1, 7)
ON CONFLICT (id) DO NOTHING;

-- Root 11: Oil & Gas Equipment
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('41000000-0000-0000-0000-000000000081', '40000000-0000-0000-0000-000000000011', 'Drilling Rigs (O&G)',      'drilling-rigs-og',         NULL, 1, 1),
    ('41000000-0000-0000-0000-000000000082', '40000000-0000-0000-0000-000000000011', 'Production Equipment',     'production-equipment',     NULL, 1, 2),
    ('41000000-0000-0000-0000-000000000083', '40000000-0000-0000-0000-000000000011', 'Pipeline Equipment',       'pipeline-equipment',       NULL, 1, 3),
    ('41000000-0000-0000-0000-000000000084', '40000000-0000-0000-0000-000000000011', 'Well Service',             'well-service',             NULL, 1, 4),
    ('41000000-0000-0000-0000-000000000085', '40000000-0000-0000-0000-000000000011', 'Fracturing Equipment',     'fracturing-equipment',     NULL, 1, 5),
    ('41000000-0000-0000-0000-000000000086', '40000000-0000-0000-0000-000000000011', 'Support Equipment (O&G)',  'support-equipment-og',     NULL, 1, 6)
ON CONFLICT (id) DO NOTHING;

-- Root 12: Power & Climate Control
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('41000000-0000-0000-0000-000000000087', '40000000-0000-0000-0000-000000000012', 'Generators',               'generators',               NULL, 1, 1),
    ('41000000-0000-0000-0000-000000000088', '40000000-0000-0000-0000-000000000012', 'Compressors',              'compressors',              NULL, 1, 2),
    ('41000000-0000-0000-0000-000000000089', '40000000-0000-0000-0000-000000000012', 'Light Towers',             'light-towers',             NULL, 1, 3),
    ('41000000-0000-0000-0000-000000000090', '40000000-0000-0000-0000-000000000012', 'Heaters',                  'heaters',                  NULL, 1, 4),
    ('41000000-0000-0000-0000-000000000091', '40000000-0000-0000-0000-000000000012', 'Cooling & AC',             'cooling-ac',               NULL, 1, 5),
    ('41000000-0000-0000-0000-000000000092', '40000000-0000-0000-0000-000000000012', 'Dehumidifiers',            'dehumidifiers',            NULL, 1, 6),
    ('41000000-0000-0000-0000-000000000093', '40000000-0000-0000-0000-000000000012', 'Pumps',                    'pumps',                    NULL, 1, 7),
    ('41000000-0000-0000-0000-000000000094', '40000000-0000-0000-0000-000000000012', 'Welding Equipment',        'welding-equipment',        NULL, 1, 8),
    ('41000000-0000-0000-0000-000000000095', '40000000-0000-0000-0000-000000000012', 'Electrical Distribution',  'electrical-distribution',  NULL, 1, 9)
ON CONFLICT (id) DO NOTHING;

-- Root 13: Metalworking & Fabrication
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('41000000-0000-0000-0000-000000000096', '40000000-0000-0000-0000-000000000013', 'CNC Machines',             'cnc-machines',             NULL, 1, 1),
    ('41000000-0000-0000-0000-000000000097', '40000000-0000-0000-0000-000000000013', 'Manual Machine Tools',     'manual-machine-tools',     NULL, 1, 2),
    ('41000000-0000-0000-0000-000000000098', '40000000-0000-0000-0000-000000000013', 'Presses',                  'presses',                  NULL, 1, 3),
    ('41000000-0000-0000-0000-000000000099', '40000000-0000-0000-0000-000000000013', 'Sheet Metal',              'sheet-metal',              NULL, 1, 4),
    ('41000000-0000-0000-0000-000000000100', '40000000-0000-0000-0000-000000000013', 'Welding & Cutting',        'welding-cutting',          NULL, 1, 5),
    ('41000000-0000-0000-0000-000000000101', '40000000-0000-0000-0000-000000000013', 'Surface Treatment',        'surface-treatment',        NULL, 1, 6),
    ('41000000-0000-0000-0000-000000000102', '40000000-0000-0000-0000-000000000013', 'Inspection & Measuring',   'inspection-measuring',     NULL, 1, 7),
    ('41000000-0000-0000-0000-000000000103', '40000000-0000-0000-0000-000000000013', 'Saws',                     'saws',                     NULL, 1, 8)
ON CONFLICT (id) DO NOTHING;

-- Root 14: Woodworking & Plastics
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('41000000-0000-0000-0000-000000000104', '40000000-0000-0000-0000-000000000014', 'Woodworking CNC',          'woodworking-cnc',          NULL, 1, 1),
    ('41000000-0000-0000-0000-000000000105', '40000000-0000-0000-0000-000000000014', 'Saws (Woodworking)',       'saws-woodworking',         NULL, 1, 2),
    ('41000000-0000-0000-0000-000000000106', '40000000-0000-0000-0000-000000000014', 'Planers & Jointers',       'planers-jointers',         NULL, 1, 3),
    ('41000000-0000-0000-0000-000000000107', '40000000-0000-0000-0000-000000000014', 'Edge & Panel',             'edge-panel',               NULL, 1, 4),
    ('41000000-0000-0000-0000-000000000108', '40000000-0000-0000-0000-000000000014', 'Finishing Equipment',       'finishing-equipment',       NULL, 1, 5),
    ('41000000-0000-0000-0000-000000000109', '40000000-0000-0000-0000-000000000014', 'Plastics Processing',      'plastics-processing',      NULL, 1, 6)
ON CONFLICT (id) DO NOTHING;

-- Root 15: Food Processing & Packaging
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('41000000-0000-0000-0000-000000000110', '40000000-0000-0000-0000-000000000015', 'Food Processing Equipment','food-processing-equipment', NULL, 1, 1),
    ('41000000-0000-0000-0000-000000000111', '40000000-0000-0000-0000-000000000015', 'Bakery Equipment',         'bakery-equipment',         NULL, 1, 2),
    ('41000000-0000-0000-0000-000000000112', '40000000-0000-0000-0000-000000000015', 'Meat & Poultry',           'meat-poultry',             NULL, 1, 3),
    ('41000000-0000-0000-0000-000000000113', '40000000-0000-0000-0000-000000000015', 'Dairy & Beverage',         'dairy-beverage',           NULL, 1, 4),
    ('41000000-0000-0000-0000-000000000114', '40000000-0000-0000-0000-000000000015', 'Packaging Equipment',      'packaging-equipment',      NULL, 1, 5),
    ('41000000-0000-0000-0000-000000000115', '40000000-0000-0000-0000-000000000015', 'Commercial Kitchen',       'commercial-kitchen',       NULL, 1, 6),
    ('41000000-0000-0000-0000-000000000116', '40000000-0000-0000-0000-000000000015', 'Printing & Labeling',      'printing-labeling',        NULL, 1, 7)
ON CONFLICT (id) DO NOTHING;

-- Root 16: Medical, Lab & Pharmaceutical
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('41000000-0000-0000-0000-000000000117', '40000000-0000-0000-0000-000000000016', 'Medical Equipment',        'medical-equipment',        NULL, 1, 1),
    ('41000000-0000-0000-0000-000000000118', '40000000-0000-0000-0000-000000000016', 'Lab Equipment',            'lab-equipment',            NULL, 1, 2),
    ('41000000-0000-0000-0000-000000000119', '40000000-0000-0000-0000-000000000016', 'Pharmaceutical Equipment', 'pharmaceutical-equipment', NULL, 1, 3),
    ('41000000-0000-0000-0000-000000000120', '40000000-0000-0000-0000-000000000016', 'Biotech Equipment',        'biotech-equipment',        NULL, 1, 4)
ON CONFLICT (id) DO NOTHING;

-- Root 17: Vehicles & Fleet
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('41000000-0000-0000-0000-000000000121', '40000000-0000-0000-0000-000000000017', 'Pickup Trucks',            'pickup-trucks',            NULL, 1, 1),
    ('41000000-0000-0000-0000-000000000122', '40000000-0000-0000-0000-000000000017', 'Vans',                     'vans',                     NULL, 1, 2),
    ('41000000-0000-0000-0000-000000000123', '40000000-0000-0000-0000-000000000017', 'SUVs',                     'suvs',                     NULL, 1, 3),
    ('41000000-0000-0000-0000-000000000124', '40000000-0000-0000-0000-000000000017', 'Cars & Sedans',            'cars-sedans',              NULL, 1, 4),
    ('41000000-0000-0000-0000-000000000125', '40000000-0000-0000-0000-000000000017', 'Buses',                    'buses',                    NULL, 1, 5),
    ('41000000-0000-0000-0000-000000000126', '40000000-0000-0000-0000-000000000017', 'Utility Vehicles',         'utility-vehicles',         NULL, 1, 6),
    ('41000000-0000-0000-0000-000000000127', '40000000-0000-0000-0000-000000000017', 'Recreational Vehicles',    'recreational-vehicles',    NULL, 1, 7)
ON CONFLICT (id) DO NOTHING;

-- Root 18: Attachments, Parts & Tools
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('41000000-0000-0000-0000-000000000128', '40000000-0000-0000-0000-000000000018', 'Excavator Attachments',    'excavator-attachments',    NULL, 1, 1),
    ('41000000-0000-0000-0000-000000000129', '40000000-0000-0000-0000-000000000018', 'Loader Attachments',       'loader-attachments',       NULL, 1, 2),
    ('41000000-0000-0000-0000-000000000130', '40000000-0000-0000-0000-000000000018', 'Skid Steer Attachments',   'skid-steer-attachments',   NULL, 1, 3),
    ('41000000-0000-0000-0000-000000000131', '40000000-0000-0000-0000-000000000018', 'Crane Attachments & Parts','crane-attachments-parts',  NULL, 1, 4),
    ('41000000-0000-0000-0000-000000000132', '40000000-0000-0000-0000-000000000018', 'Construction Parts',       'construction-parts',       NULL, 1, 5),
    ('41000000-0000-0000-0000-000000000133', '40000000-0000-0000-0000-000000000018', 'Truck Parts',              'truck-parts',              NULL, 1, 6),
    ('41000000-0000-0000-0000-000000000134', '40000000-0000-0000-0000-000000000018', 'Hand & Power Tools',       'hand-power-tools',         NULL, 1, 7),
    ('41000000-0000-0000-0000-000000000135', '40000000-0000-0000-0000-000000000018', 'Safety Equipment',         'safety-equipment',         NULL, 1, 8)
ON CONFLICT (id) DO NOTHING;
