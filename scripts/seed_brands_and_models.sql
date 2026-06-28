-- =============================================================================
-- Seed: fnd_product_brand + fnd_brand_model
-- Target DB: PostgreSQL
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Brands
-- -----------------------------------------------------------------------------
INSERT INTO fnd_product_brand (category_id, code, description, sort_order, active) VALUES
(1, 'MARUTISUZUKI',  'Maruti Suzuki',  1,  true),
(1, 'TOYOTA',        'Toyota',         2,  true),
(1, 'HYUNDAI',       'Hyundai',        3,  true),
(1, 'TATA',          'Tata',           4,  true),
(1, 'KIA',           'Kia',            5,  true),
(1, 'MGMOTORS',      'MG Motors',      6,  true),
(1, 'MAHINDRA',      'Mahindra',       7,  true),
(1, 'JEEP',          'Jeep',           8,  true),
(1, 'HONDA',         'Honda',          9,  true),
(1, 'SKODA',         'Skoda',          10, true),
(1, 'CITROEN',       'Citroen',        11, true),
(1, 'RENAULT',       'Renault',        12, true),
(1, 'NISSAN',        'Nissan',         13, true),
(1, 'VOLKSWAGEN',    'Volkswagen',     14, true),
(1, 'CHEVROLET',     'Chevrolet',      15, true),
(1, 'FORD',          'Ford',           16, true),
(1, 'MERCEDESBENZ',  'Mercedes Benz',  17, true),
(1, 'AUDI',          'Audi',           18, true),
(1, 'BMW',           'BMW',            19, true);

-- -----------------------------------------------------------------------------
-- Models
-- -----------------------------------------------------------------------------

-- Maruti Suzuki
INSERT INTO fnd_brand_model (brand_id, code, description, sort_order, active)
SELECT id, v.code, v.description, v.sort_order, true
FROM fnd_product_brand b
JOIN (VALUES
    ('MARUTI800',    'Maruti 800',    1),
    ('ZEN',          'Zen',           2),
    ('ALTO',         'Alto',          3),
    ('ALTO800',      'Alto 800',      4),
    ('K10',          'K10',           5),
    ('ASTAR',        'Astar',         6),
    ('ESTILO',       'Estilo',        7),
    ('SPRESSO',      'Spresso',       8),
    ('GYPSY',        'Gypsy',         9),
    ('ESTEEM',       'Esteem',        10),
    ('SX4',          'SX4',           11),
    ('DZIRE',        'Dzire',         12),
    ('SWIFT',        'Swift',         13),
    ('RITZ',         'Ritz',          14),
    ('SCROSS',       'S-Cross',       15),
    ('IGNIS',        'Ignis',         16),
    ('XL6',          'XL6',           17),
    ('ERTIGA',       'Ertiga',        18),
    ('BALENO',       'Baleno',        19),
    ('CELERIO',      'Celerio',       20),
    ('WAGONR',       'WagonR',        21),
    ('FRONX',        'Fronx',         22),
    ('BREZZA',       'Brezza',        23),
    ('JIMNY',        'Jimny',         24),
    ('GRANDVITARA',  'Grand Vitara',  25),
    ('EVITARA',      'E Vitara',      26),
    ('EECO',         'Eeco',          27),
    ('OMNI',         'Omni',          28),
    ('INVICTO',      'Invicto',       29),
    ('VICTORIS',     'Victoris',      30)
) AS v(code, description, sort_order) ON b.code = 'MARUTISUZUKI';

-- Toyota
INSERT INTO fnd_brand_model (brand_id, code, description, sort_order, active)
SELECT id, v.code, v.description, v.sort_order, true
FROM fnd_product_brand b
JOIN (VALUES
    ('FORTUNER',      'Fortuner',       1),
    ('CRYSTA',        'Crysta',         2),
    ('INNOVA',        'Innova',         3),
    ('ETIOS',         'Etios',          4),
    ('LIVA',          'Liva',           5),
    ('HYRYDER',       'Hyryder',        6),
    ('TAISOR',        'Taisor',         7),
    ('GLANZA',        'Glanza',         8),
    ('HYCROSS',       'Hycross',        9),
    ('HILUX',         'Hilux',          10),
    ('RUMION',        'Rumion',         11),
    ('QUALIS',        'Qualis',         12),
    ('YARIS',         'Yaris',          13),
    ('ALTIS',         'Altis',          14),
    ('URBANCRUISER',  'Urban Cruiser',  15)
) AS v(code, description, sort_order) ON b.code = 'TOYOTA';

-- Hyundai
INSERT INTO fnd_brand_model (brand_id, code, description, sort_order, active)
SELECT id, v.code, v.description, v.sort_order, true
FROM fnd_product_brand b
JOIN (VALUES
    ('CRETA',     'Creta',      1),
    ('ELITEI20',  'Elite i20',  2),
    ('GRANDI10',  'Grand i10',  3),
    ('SANTAFE',   'Santa Fe',   4),
    ('ALCAZAR',   'Alcazar',    5),
    ('I10NIOS',   'i10 Nios',   6),
    ('I10',       'i10',        7),
    ('I20',       'i20',        8),
    ('EXTER',     'Exter',      9),
    ('VENUE',     'Venue',      10),
    ('IONIQ',     'Ioniq',      11),
    ('SANTRO',    'Santro',     12),
    ('GETZ',      'Getz',       13),
    ('EON',       'Eon',        14),
    ('ACCENT',    'Accent',     15),
    ('XCENT',     'Xcent',      16),
    ('ELANTRA',   'Elantra',    17),
    ('KONA',      'Kona',       18)
) AS v(code, description, sort_order) ON b.code = 'HYUNDAI';

-- Tata
INSERT INTO fnd_brand_model (brand_id, code, description, sort_order, active)
SELECT id, v.code, v.description, v.sort_order, true
FROM fnd_product_brand b
JOIN (VALUES
    ('HARRIER',  'Harrier',  1),
    ('HEXA',     'Hexa',     2),
    ('TIAGO',    'Tiago',    3),
    ('TIGOR',    'Tigor',    4),
    ('ALTROZ',   'Altroz',   5),
    ('PUNCH',    'Punch',    6),
    ('NEXON',    'Nexon',    7),
    ('CURVV',    'Curvv',    8),
    ('SAFARI',   'Safari',   9),
    ('INDICA',   'Indica',   10),
    ('VISTA',    'Vista',    11),
    ('BOLT',     'Bolt',     12),
    ('NANO',     'Nano',     13),
    ('SUMO',     'Sumo',     14),
    ('ARIA',     'Aria',     15),
    ('VENTURE',  'Venture',  16)
) AS v(code, description, sort_order) ON b.code = 'TATA';

-- Kia
INSERT INTO fnd_brand_model (brand_id, code, description, sort_order, active)
SELECT id, v.code, v.description, v.sort_order, true
FROM fnd_product_brand b
JOIN (VALUES
    ('SONET',     'Sonet',     1),
    ('SELTOS',    'Seltos',    2),
    ('SYROS',     'Syros',     3),
    ('CARNIVAL',  'Carnival',  4),
    ('CARENS',    'Carens',    5)
) AS v(code, description, sort_order) ON b.code = 'KIA';

-- MG Motors
INSERT INTO fnd_brand_model (brand_id, code, description, sort_order, active)
SELECT id, v.code, v.description, v.sort_order, true
FROM fnd_product_brand b
JOIN (VALUES
    ('ASTOR',    'Astor',    1),
    ('HECTOR',   'Hector',   2),
    ('COMET',    'Comet',    3),
    ('WINDSOR',  'Windsor',  4),
    ('ZSEV',     'ZS EV',    5)
) AS v(code, description, sort_order) ON b.code = 'MGMOTORS';

-- Mahindra
INSERT INTO fnd_brand_model (brand_id, code, description, sort_order, active)
SELECT id, v.code, v.description, v.sort_order, true
FROM fnd_product_brand b
JOIN (VALUES
    ('BOLERO',         'Bolero',          1),
    ('BOLERONEO',      'Bolero Neo',      2),
    ('THAR',           'Thar',            3),
    ('THARROXX',       'Thar Roxx',       4),
    ('SCORPIOCLASSIC', 'Scorpio Classic', 5),
    ('SCORPION',       'Scorpio-N',       6),
    ('XUV3XO',         'XUV 3XO',         7),
    ('XUV700',         'XUV 700',         8),
    ('BE6',            'BE 6',            9),
    ('XEV9E',          'XEV 9e',          10),
    ('XEV9S',          'XEV 9S',          11),
    ('XUV3XOEV',       'XUV 3XO EV',      12),
    ('VOYAGER',        'Voyager',         13),
    ('MAJOR',          'Major',           14),
    ('XYLO',           'Xylo',            15),
    ('QUANTO',         'Quanto',          16),
    ('TUV300',         'TUV300',          17),
    ('XUV500',         'XUV500',          18),
    ('VERITO',         'Verito',          19),
    ('E2O',            'e2o',             20),
    ('KUV100',         'KUV100',          21),
    ('MARAZZO',        'Marazzo',         22)
) AS v(code, description, sort_order) ON b.code = 'MAHINDRA';

-- Jeep
INSERT INTO fnd_brand_model (brand_id, code, description, sort_order, active)
SELECT id, v.code, v.description, v.sort_order, true
FROM fnd_product_brand b
JOIN (VALUES
    ('COMPASS',       'Compass',        1),
    ('MERIDIAN',      'Meridian',       2),
    ('WRANGLER',      'Wrangler',       3),
    ('GRANDCHEROKEE', 'Grand Cherokee', 4)
) AS v(code, description, sort_order) ON b.code = 'JEEP';

-- Honda
INSERT INTO fnd_brand_model (brand_id, code, description, sort_order, active)
SELECT id, v.code, v.description, v.sort_order, true
FROM fnd_product_brand b
JOIN (VALUES
    ('AMAZE',    'Amaze',    1),
    ('CITY',     'City',     2),
    ('ELEVATE',  'Elevate',  3),
    ('BRIO',     'Brio',     4),
    ('JAZZ',     'Jazz',     5),
    ('CIVIC',    'Civic',    6),
    ('ACCORD',   'Accord',   7),
    ('WRV',      'WR-V',     8),
    ('BRV',      'BR-V',     9),
    ('CRV',      'CR-V',     10),
    ('MOBILIO',  'Mobilio',  11)
) AS v(code, description, sort_order) ON b.code = 'HONDA';

-- Renault
INSERT INTO fnd_brand_model (brand_id, code, description, sort_order, active)
SELECT id, v.code, v.description, v.sort_order, true
FROM fnd_product_brand b
JOIN (VALUES
    ('KWID',     'Kwid',     1),
    ('TRIBER',   'Triber',   2),
    ('KIGER',    'Kiger',    3),
    ('DUSTER',   'Duster',   4),
    ('PULSE',    'Pulse',    5),
    ('SCALA',    'Scala',    6),
    ('FLUENCE',  'Fluence',  7),
    ('LODGY',    'Lodgy',    8)
) AS v(code, description, sort_order) ON b.code = 'RENAULT';

-- Nissan
INSERT INTO fnd_brand_model (brand_id, code, description, sort_order, active)
SELECT id, v.code, v.description, v.sort_order, true
FROM fnd_product_brand b
JOIN (VALUES
    ('MICRA',    'Micra',    1),
    ('SUNNY',    'Sunny',    2),
    ('EVALIA',   'Evalia',   3),
    ('TERRANO',  'Terrano',  4),
    ('KICKS',    'Kicks',    5),
    ('XTRAIL',   'X-Trail',  6)
) AS v(code, description, sort_order) ON b.code = 'NISSAN';

-- Volkswagen
INSERT INTO fnd_brand_model (brand_id, code, description, sort_order, active)
SELECT id, v.code, v.description, v.sort_order, true
FROM fnd_product_brand b
JOIN (VALUES
    ('GOLFGTI',  'Golf GTI',  1),
    ('VIRTUS',   'Virtus',    2),
    ('TAIGUN',   'Taigun',    3),
    ('POLO',     'Polo',      4),
    ('POLOGTI',  'Polo GTI',  5),
    ('VENTO',    'Vento',     6),
    ('AMEO',     'Ameo',      7),
    ('JETTA',    'Jetta',     8),
    ('PASSAT',   'Passat',    9),
    ('BEETLE',   'Beetle',    10)
) AS v(code, description, sort_order) ON b.code = 'VOLKSWAGEN';

-- Chevrolet
INSERT INTO fnd_brand_model (brand_id, code, description, sort_order, active)
SELECT id, v.code, v.description, v.sort_order, true
FROM fnd_product_brand b
JOIN (VALUES
    ('SPARK',    'Spark',    1),
    ('BEAT',     'Beat',     2),
    ('SAIL',     'Sail',     3),
    ('AVEO',     'Aveo',     4),
    ('OPTRA',    'Optra',    5),
    ('CRUZE',    'Cruze',    6),
    ('ENJOY',    'Enjoy',    7),
    ('TAVERA',   'Tavera',   8),
    ('CAPTIVA',  'Captiva',  9)
) AS v(code, description, sort_order) ON b.code = 'CHEVROLET';

-- Ford
INSERT INTO fnd_brand_model (brand_id, code, description, sort_order, active)
SELECT id, v.code, v.description, v.sort_order, true
FROM fnd_product_brand b
JOIN (VALUES
    ('FIGO',       'Figo',       1),
    ('FIESTA',     'Fiesta',     2),
    ('ECOSPORT',   'EcoSport',   3),
    ('FREESTYLE',  'Freestyle',  4),
    ('IKON',       'Ikon',       5),
    ('ENDEAVOUR',  'Endeavour',  6),
    ('FUSION',     'Fusion',     7)
) AS v(code, description, sort_order) ON b.code = 'FORD';

-- =============================================================================
-- Variants (fnd_model_varient)
-- Each block resolves model_id via (brand code, model code).
-- type values: PETROL / DIESEL / CNG / HYBRID / EV / NULL
-- =============================================================================

-- ---------- MARUTI SUZUKI ----------

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('STD_P', 'STD',   1, 'PETROL'),
    ('AC_P',  'AC',    2, 'PETROL'),
    ('DUO',   'DUO',   3, 'CNG')
) AS v(code, description, sort_order, type)
WHERE m.code = 'MARUTI800' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LX_P',  'LX',  1, 'PETROL'),
    ('LXI_P', 'LXi', 2, 'PETROL'),
    ('VXI_P', 'VXi', 3, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'ZEN' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('STD_P', 'STD', 1, 'PETROL'),
    ('LX_P',  'LX',  2, 'PETROL'),
    ('LXI_P', 'LXi', 3, 'PETROL'),
    ('VXI_P', 'VXi', 4, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'ALTO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('STD_P', 'STD',     1, 'PETROL'),
    ('LXI_P', 'LXi',     2, 'PETROL'),
    ('VXI_P', 'VXi',     3, 'PETROL'),
    ('VXIP',  'VXi+',    4, 'PETROL'),
    ('LXI_C', 'LXi CNG', 5, 'CNG')
) AS v(code, description, sort_order, type)
WHERE m.code = 'ALTO800' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LXI_P', 'LXi', 1, 'PETROL'),
    ('VXI_P', 'VXi', 2, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'K10' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LXI_P', 'LXi', 1, 'PETROL'),
    ('VXI_P', 'VXi', 2, 'PETROL'),
    ('ZXI_P', 'ZXi', 3, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'ASTAR' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LXI_P', 'LXi', 1, 'PETROL'),
    ('VXI_P', 'VXi', 2, 'PETROL'),
    ('ZXI_P', 'ZXi', 3, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'ESTILO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('STD_P', 'STD',     1, 'PETROL'),
    ('LXI_P', 'LXi',     2, 'PETROL'),
    ('VXI_P', 'VXi',     3, 'PETROL'),
    ('VXIP',  'VXi+',    4, 'PETROL'),
    ('LXI_C', 'LXi CNG', 5, 'CNG'),
    ('VXI_C', 'VXi CNG', 6, 'CNG')
) AS v(code, description, sort_order, type)
WHERE m.code = 'SPRESSO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('HT_P', 'Hard Top', 1, 'PETROL'),
    ('ST_P', 'Soft Top', 2, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'GYPSY' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LX_P',  'LX',  1, 'PETROL'),
    ('LXI_P', 'LXi', 2, 'PETROL'),
    ('VXI_P', 'VXi', 3, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'ESTEEM' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('VXI_P', 'VXi', 1, 'PETROL'),
    ('ZXI_P', 'ZXi', 2, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'SX4' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LXI_P',  'LXi',     1, 'PETROL'),
    ('VXI_P',  'VXi',     2, 'PETROL'),
    ('ZXI_P',  'ZXi',     3, 'PETROL'),
    ('ZXIP_P', 'ZXi+',    4, 'PETROL'),
    ('VXI_C',  'VXi CNG', 5, 'CNG'),
    ('ZXI_C',  'ZXi CNG', 6, 'CNG')
) AS v(code, description, sort_order, type)
WHERE m.code = 'DZIRE' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LXI_P',  'LXi',     1, 'PETROL'),
    ('VXI_P',  'VXi',     2, 'PETROL'),
    ('ZXI_P',  'ZXi',     3, 'PETROL'),
    ('ZXIP_P', 'ZXi+',    4, 'PETROL'),
    ('VXI_C',  'VXi CNG', 5, 'CNG')
) AS v(code, description, sort_order, type)
WHERE m.code = 'SWIFT' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LXI_P', 'LXi', 1, 'PETROL'),
    ('VXI_P', 'VXi', 2, 'PETROL'),
    ('ZXI_P', 'ZXi', 3, 'PETROL'),
    ('LDI_D', 'LDi', 4, 'DIESEL'),
    ('VDI_D', 'VDi', 5, 'DIESEL'),
    ('ZDI_D', 'ZDi', 6, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'RITZ' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('SIGMA_D', 'Sigma', 1, 'DIESEL'),
    ('DELTA_D', 'Delta', 2, 'DIESEL'),
    ('ZETA_D',  'Zeta',  3, 'DIESEL'),
    ('ALPHA_D', 'Alpha', 4, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'SCROSS' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('SIGMA_P', 'Sigma', 1, 'PETROL'),
    ('DELTA_P', 'Delta', 2, 'PETROL'),
    ('ZETA_P',  'Zeta',  3, 'PETROL'),
    ('ALPHA_P', 'Alpha', 4, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'IGNIS' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('ZETA_P',  'Zeta',      1, 'PETROL'),
    ('ALPHA_P', 'Alpha',     2, 'PETROL'),
    ('ALPHAP',  'Alpha+',    3, 'PETROL'),
    ('ZETA_C',  'Zeta CNG',  4, 'CNG'),
    ('ALPHA_C', 'Alpha CNG', 5, 'CNG')
) AS v(code, description, sort_order, type)
WHERE m.code = 'XL6' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LXI_P',  'LXi',     1, 'PETROL'),
    ('VXI_P',  'VXi',     2, 'PETROL'),
    ('ZXI_P',  'ZXi',     3, 'PETROL'),
    ('ZXIP_P', 'ZXi+',    4, 'PETROL'),
    ('VXI_C',  'VXi CNG', 5, 'CNG'),
    ('ZXI_C',  'ZXi CNG', 6, 'CNG')
) AS v(code, description, sort_order, type)
WHERE m.code = 'ERTIGA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('SIGMA_P', 'Sigma',     1, 'PETROL'),
    ('DELTA_P', 'Delta',     2, 'PETROL'),
    ('ZETA_P',  'Zeta',      3, 'PETROL'),
    ('ALPHA_P', 'Alpha',     4, 'PETROL'),
    ('DELTA_C', 'Delta CNG', 5, 'CNG'),
    ('ZETA_C',  'Zeta CNG',  6, 'CNG')
) AS v(code, description, sort_order, type)
WHERE m.code = 'BALENO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LXI_P',  'LXi',     1, 'PETROL'),
    ('VXI_P',  'VXi',     2, 'PETROL'),
    ('ZXI_P',  'ZXi',     3, 'PETROL'),
    ('ZXIP_P', 'ZXi+',    4, 'PETROL'),
    ('VXI_C',  'VXi CNG', 5, 'CNG')
) AS v(code, description, sort_order, type)
WHERE m.code = 'CELERIO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LXI_P', 'LXi',     1, 'PETROL'),
    ('VXI_P', 'VXi',     2, 'PETROL'),
    ('ZXI_P', 'ZXi',     3, 'PETROL'),
    ('LXI_C', 'LXi CNG', 4, 'CNG'),
    ('VXI_C', 'VXi CNG', 5, 'CNG')
) AS v(code, description, sort_order, type)
WHERE m.code = 'WAGONR' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('SIGMA_P',  'Sigma',     1, 'PETROL'),
    ('DELTA_P',  'Delta',     2, 'PETROL'),
    ('DELTAP_P', 'Delta+',    3, 'PETROL'),
    ('ZETA_P',   'Zeta',      4, 'PETROL'),
    ('ALPHA_P',  'Alpha',     5, 'PETROL'),
    ('DELTA_C',  'Delta CNG', 6, 'CNG')
) AS v(code, description, sort_order, type)
WHERE m.code = 'FRONX' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LXI_P',  'LXi',     1, 'PETROL'),
    ('VXI_P',  'VXi',     2, 'PETROL'),
    ('ZXI_P',  'ZXi',     3, 'PETROL'),
    ('ZXIP_P', 'ZXi+',    4, 'PETROL'),
    ('VXI_C',  'VXi CNG', 5, 'CNG'),
    ('ZXI_C',  'ZXi CNG', 6, 'CNG')
) AS v(code, description, sort_order, type)
WHERE m.code = 'BREZZA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('ZETA_P',  'Zeta',  1, 'PETROL'),
    ('ALPHA_P', 'Alpha', 2, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'JIMNY' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('SIGMA_P', 'Sigma',        1, 'PETROL'),
    ('DELTA_P', 'Delta',        2, 'PETROL'),
    ('ZETA_P',  'Zeta',         3, 'PETROL'),
    ('ALPHA_P', 'Alpha',        4, 'PETROL'),
    ('ZETA_H',  'Zeta Hybrid',  5, 'HYBRID'),
    ('ALPHA_H', 'Alpha Hybrid', 6, 'HYBRID'),
    ('DELTA_C', 'Delta CNG',    7, 'CNG')
) AS v(code, description, sort_order, type)
WHERE m.code = 'GRANDVITARA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('BASE_EV', 'Base', 1, 'EV'),
    ('MID_EV',  'Mid',  2, 'EV'),
    ('TOP_EV',  'Top',  3, 'EV')
) AS v(code, description, sort_order, type)
WHERE m.code = 'EVITARA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('STR5_P',  '5 STR',     1, 'PETROL'),
    ('STR7_P',  '7 STR',     2, 'PETROL'),
    ('STR5AC',  '5 STR AC',  3, 'PETROL'),
    ('STR5_C',  '5 STR CNG', 4, 'CNG')
) AS v(code, description, sort_order, type)
WHERE m.code = 'EECO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('CARGO_P', 'Cargo', 1, 'PETROL'),
    ('STR5_P',  '5 STR', 2, 'PETROL'),
    ('STR8_P',  '8 STR', 3, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'OMNI' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('ZETAP_H',  'Zeta+',  1, 'HYBRID'),
    ('ALPHAP_H', 'Alpha+', 2, 'HYBRID')
) AS v(code, description, sort_order, type)
WHERE m.code = 'INVICTO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('SIGMA_P', 'Sigma', 1, 'PETROL'),
    ('DELTA_P', 'Delta', 2, 'PETROL'),
    ('ZETA_P',  'Zeta',  3, 'PETROL'),
    ('ALPHA_P', 'Alpha', 4, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'VICTORIS' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MARUTISUZUKI');

-- ---------- TOYOTA ----------

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('4X2MT_D',  '4x2 MT',    1, 'DIESEL'),
    ('4X2AT_D',  '4x2 AT',    2, 'DIESEL'),
    ('4X4MT_D',  '4x4 MT',    3, 'DIESEL'),
    ('4X4AT_D',  '4x4 AT',    4, 'DIESEL'),
    ('LEGEND_D', 'Legender',  5, 'DIESEL'),
    ('4X2AT_P',  '4x2 AT',    6, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'FORTUNER' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TOYOTA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('GX_D', 'GX', 1, 'DIESEL'),
    ('VX_D', 'VX', 2, 'DIESEL'),
    ('ZX_D', 'ZX', 3, 'DIESEL'),
    ('GX_P', 'GX', 4, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'CRYSTA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TOYOTA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('G_D',  'G',  1, 'DIESEL'),
    ('GX_D', 'GX', 2, 'DIESEL'),
    ('VX_D', 'VX', 3, 'DIESEL'),
    ('ZX_D', 'ZX', 4, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'INNOVA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TOYOTA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('J_P',  'J',  1, 'PETROL'),
    ('G_P',  'G',  2, 'PETROL'),
    ('V_P',  'V',  3, 'PETROL'),
    ('VX_P', 'VX', 4, 'PETROL'),
    ('GD_D', 'GD', 5, 'DIESEL'),
    ('VD_D', 'VD', 6, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'ETIOS' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TOYOTA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('J_P',  'J',  1, 'PETROL'),
    ('G_P',  'G',  2, 'PETROL'),
    ('V_P',  'V',  3, 'PETROL'),
    ('GD_D', 'GD', 4, 'DIESEL'),
    ('VD_D', 'VD', 5, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'LIVA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TOYOTA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('E_P',  'E',         1, 'PETROL'),
    ('S_P',  'S',         2, 'PETROL'),
    ('G_P',  'G',         3, 'PETROL'),
    ('V_P',  'V',         4, 'PETROL'),
    ('V_H',  'V Hybrid',  5, 'HYBRID'),
    ('G_H',  'G Hybrid',  6, 'HYBRID'),
    ('S_C',  'S CNG',     7, 'CNG')
) AS v(code, description, sort_order, type)
WHERE m.code = 'HYRYDER' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TOYOTA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('E_P', 'E',     1, 'PETROL'),
    ('S_P', 'S',     2, 'PETROL'),
    ('G_P', 'G',     3, 'PETROL'),
    ('V_P', 'V',     4, 'PETROL'),
    ('S_C', 'S CNG', 5, 'CNG')
) AS v(code, description, sort_order, type)
WHERE m.code = 'TAISOR' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TOYOTA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('E_P', 'E',     1, 'PETROL'),
    ('S_P', 'S',     2, 'PETROL'),
    ('G_P', 'G',     3, 'PETROL'),
    ('V_P', 'V',     4, 'PETROL'),
    ('S_C', 'S CNG', 5, 'CNG')
) AS v(code, description, sort_order, type)
WHERE m.code = 'GLANZA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TOYOTA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('G_P',   'G',          1, 'PETROL'),
    ('V_P',   'V',          2, 'PETROL'),
    ('ZX_P',  'ZX',         3, 'PETROL'),
    ('ZXO_P', 'ZX(O)',      4, 'PETROL'),
    ('V_H',   'V Hybrid',   5, 'HYBRID'),
    ('ZX_H',  'ZX Hybrid',  6, 'HYBRID')
) AS v(code, description, sort_order, type)
WHERE m.code = 'HYCROSS' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TOYOTA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('STD_D',  'Standard', 1, 'DIESEL'),
    ('HIGH_D', 'High',     2, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'HILUX' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TOYOTA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('S_P', 'S',     1, 'PETROL'),
    ('G_P', 'G',     2, 'PETROL'),
    ('V_P', 'V',     3, 'PETROL'),
    ('S_C', 'S CNG', 4, 'CNG')
) AS v(code, description, sort_order, type)
WHERE m.code = 'RUMION' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TOYOTA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('B2_D', 'B2', 1, 'DIESEL'),
    ('B3_D', 'B3', 2, 'DIESEL'),
    ('GS_D', 'GS', 3, 'DIESEL'),
    ('RS_D', 'RS', 4, 'DIESEL'),
    ('FS_D', 'FS', 5, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'QUALIS' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TOYOTA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('J_P',  'J',  1, 'PETROL'),
    ('G_P',  'G',  2, 'PETROL'),
    ('V_P',  'V',  3, 'PETROL'),
    ('VX_P', 'VX', 4, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'YARIS' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TOYOTA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('G_P',  'G',  1, 'PETROL'),
    ('GL_P', 'GL', 2, 'PETROL'),
    ('J_D',  'J',  3, 'DIESEL'),
    ('VL_D', 'VL', 4, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'ALTIS' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TOYOTA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('MID_P',  'Mid',     1, 'PETROL'),
    ('HIGH_P', 'High',    2, 'PETROL'),
    ('PREM_P', 'Premium', 3, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'URBANCRUISER' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TOYOTA');

-- ---------- HYUNDAI ----------

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('E_P',   'E',     1, 'PETROL'),
    ('EX_P',  'EX',    2, 'PETROL'),
    ('S_P',   'S',     3, 'PETROL'),
    ('SX_P',  'SX',    4, 'PETROL'),
    ('SXO_P', 'SX(O)', 5, 'PETROL'),
    ('E_D',   'E',     6, 'DIESEL'),
    ('S_D',   'S',     7, 'DIESEL'),
    ('SX_D',  'SX',    8, 'DIESEL'),
    ('SXO_D', 'SX(O)', 9, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'CRETA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HYUNDAI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('ERA_P',    'Era',     1, 'PETROL'),
    ('MAGNA_P',  'Magna',   2, 'PETROL'),
    ('SPORTZ_P', 'Sportz',  3, 'PETROL'),
    ('ASTA_P',   'Asta',    4, 'PETROL'),
    ('ERA_D',    'Era',     5, 'DIESEL'),
    ('MAGNA_D',  'Magna',   6, 'DIESEL'),
    ('SPORTZ_D', 'Sportz',  7, 'DIESEL'),
    ('ASTA_D',   'Asta',    8, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'ELITEI20' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HYUNDAI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('ERA_P',    'Era',    1, 'PETROL'),
    ('MAGNA_P',  'Magna',  2, 'PETROL'),
    ('SPORTZ_P', 'Sportz', 3, 'PETROL'),
    ('ASTA_P',   'Asta',   4, 'PETROL'),
    ('MAGNA_D',  'Magna',  5, 'DIESEL'),
    ('SPORTZ_D', 'Sportz', 6, 'DIESEL'),
    ('ASTA_D',   'Asta',   7, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'GRANDI10' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HYUNDAI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('2WD_D', '2WD MT', 1, 'DIESEL'),
    ('4WD_D', '4WD AT', 2, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'SANTAFE' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HYUNDAI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('PREST_P', 'Prestige',  1, 'PETROL'),
    ('PLAT_P',  'Platinum',  2, 'PETROL'),
    ('SIG_P',   'Signature', 3, 'PETROL'),
    ('PREST_D', 'Prestige',  4, 'DIESEL'),
    ('PLAT_D',  'Platinum',  5, 'DIESEL'),
    ('SIG_D',   'Signature', 6, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'ALCAZAR' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HYUNDAI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('ERA_P',    'Era',        1, 'PETROL'),
    ('MAGNA_P',  'Magna',      2, 'PETROL'),
    ('SPORTZ_P', 'Sportz',     3, 'PETROL'),
    ('ASTA_P',   'Asta',       4, 'PETROL'),
    ('MAGNA_C',  'Magna CNG',  5, 'CNG'),
    ('SPORTZ_C', 'Sportz CNG', 6, 'CNG')
) AS v(code, description, sort_order, type)
WHERE m.code = 'I10NIOS' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HYUNDAI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('ERA_P',    'Era',    1, 'PETROL'),
    ('MAGNA_P',  'Magna',  2, 'PETROL'),
    ('SPORTZ_P', 'Sportz', 3, 'PETROL'),
    ('ASTA_P',   'Asta',   4, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'I10' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HYUNDAI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('ERA_P',    'Era',    1, 'PETROL'),
    ('MAGNA_P',  'Magna',  2, 'PETROL'),
    ('SPORTZ_P', 'Sportz', 3, 'PETROL'),
    ('ASTA_P',   'Asta',   4, 'PETROL'),
    ('ERA_D',    'Era',    5, 'DIESEL'),
    ('MAGNA_D',  'Magna',  6, 'DIESEL'),
    ('SPORTZ_D', 'Sportz', 7, 'DIESEL'),
    ('ASTA_D',   'Asta',   8, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'I20' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HYUNDAI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('EX_P',  'EX',     1, 'PETROL'),
    ('S_P',   'S',      2, 'PETROL'),
    ('SX_P',  'SX',     3, 'PETROL'),
    ('SXO_P', 'SX(O)',  4, 'PETROL'),
    ('S_C',   'S CNG',  5, 'CNG'),
    ('SX_C',  'SX CNG', 6, 'CNG')
) AS v(code, description, sort_order, type)
WHERE m.code = 'EXTER' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HYUNDAI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('E_P',   'E',     1, 'PETROL'),
    ('S_P',   'S',     2, 'PETROL'),
    ('SX_P',  'SX',    3, 'PETROL'),
    ('SXO_P', 'SX(O)', 4, 'PETROL'),
    ('S_D',   'S',     5, 'DIESEL'),
    ('SX_D',  'SX',    6, 'DIESEL'),
    ('SXO_D', 'SX(O)', 7, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'VENUE' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HYUNDAI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('PREM_EV', 'Premium',   1, 'EV'),
    ('SIG_EV',  'Signature', 2, 'EV')
) AS v(code, description, sort_order, type)
WHERE m.code = 'IONIQ' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HYUNDAI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('DLITE_P',  'D-Lite',    1, 'PETROL'),
    ('ERA_P',    'Era',       2, 'PETROL'),
    ('MAGNA_P',  'Magna',     3, 'PETROL'),
    ('SPORTZ_P', 'Sportz',    4, 'PETROL'),
    ('ASTA_P',   'Asta',      5, 'PETROL'),
    ('MAGNA_C',  'Magna CNG', 6, 'CNG')
) AS v(code, description, sort_order, type)
WHERE m.code = 'SANTRO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HYUNDAI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('GL_P',  'GL',  1, 'PETROL'),
    ('GLX_P', 'GLX', 2, 'PETROL'),
    ('GLS_P', 'GLS', 3, 'PETROL'),
    ('GLS_D', 'GLS', 4, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'GETZ' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HYUNDAI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('DLITE_P',  'D-Lite', 1, 'PETROL'),
    ('ERA_P',    'Era',    2, 'PETROL'),
    ('MAGNA_P',  'Magna',  3, 'PETROL'),
    ('SPORTZ_P', 'Sportz', 4, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'EON' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HYUNDAI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('GLE_P', 'GLE', 1, 'PETROL'),
    ('GLS_P', 'GLS', 2, 'PETROL'),
    ('GLE_D', 'GLE', 3, 'DIESEL'),
    ('GLS_D', 'GLS', 4, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'ACCENT' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HYUNDAI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('E_P',  'E',  1, 'PETROL'),
    ('S_P',  'S',  2, 'PETROL'),
    ('SX_P', 'SX', 3, 'PETROL'),
    ('S_D',  'S',  4, 'DIESEL'),
    ('SX_D', 'SX', 5, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'XCENT' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HYUNDAI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('S_P',   'S',     1, 'PETROL'),
    ('SX_P',  'SX',    2, 'PETROL'),
    ('SXO_P', 'SX(O)', 3, 'PETROL'),
    ('S_D',   'S',     4, 'DIESEL'),
    ('SX_D',  'SX',    5, 'DIESEL'),
    ('SXO_D', 'SX(O)', 6, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'ELANTRA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HYUNDAI');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('PREM_EV',   'Premium',    1, 'EV'),
    ('PREMDT_EV', 'Premium DT', 2, 'EV')
) AS v(code, description, sort_order, type)
WHERE m.code = 'KONA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HYUNDAI');

-- ---------- TATA ----------

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('XE_D',  'XE',     1, 'DIESEL'),
    ('XM_D',  'XM',     2, 'DIESEL'),
    ('XT_D',  'XT',     3, 'DIESEL'),
    ('XZ_D',  'XZ',     4, 'DIESEL'),
    ('XZP_D', 'XZ+',    5, 'DIESEL'),
    ('XZAP',  'XZA+',   6, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'HARRIER' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TATA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('XE_D',  'XE',  1, 'DIESEL'),
    ('XM_D',  'XM',  2, 'DIESEL'),
    ('XT_D',  'XT',  3, 'DIESEL'),
    ('XTA_D', 'XTA', 4, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'HEXA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TATA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('XE_P',  'XE',     1, 'PETROL'),
    ('XM_P',  'XM',     2, 'PETROL'),
    ('XT_P',  'XT',     3, 'PETROL'),
    ('XZ_P',  'XZ',     4, 'PETROL'),
    ('XZP_P', 'XZ+',    5, 'PETROL'),
    ('XM_C',  'XM CNG', 6, 'CNG'),
    ('XZ_C',  'XZ CNG', 7, 'CNG'),
    ('EV',    'EV',     8, 'EV')
) AS v(code, description, sort_order, type)
WHERE m.code = 'TIAGO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TATA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('XE_P',  'XE',     1, 'PETROL'),
    ('XM_P',  'XM',     2, 'PETROL'),
    ('XT_P',  'XT',     3, 'PETROL'),
    ('XZ_P',  'XZ',     4, 'PETROL'),
    ('XZP_P', 'XZ+',    5, 'PETROL'),
    ('XZ_C',  'XZ CNG', 6, 'CNG'),
    ('EV',    'EV',     7, 'EV')
) AS v(code, description, sort_order, type)
WHERE m.code = 'TIGOR' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TATA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('XE_P',  'XE',     1, 'PETROL'),
    ('XM_P',  'XM',     2, 'PETROL'),
    ('XT_P',  'XT',     3, 'PETROL'),
    ('XZ_P',  'XZ',     4, 'PETROL'),
    ('XZP_P', 'XZ+',    5, 'PETROL'),
    ('XE_D',  'XE',     6, 'DIESEL'),
    ('XZ_D',  'XZ',     7, 'DIESEL'),
    ('XZ_C',  'XZ CNG', 8, 'CNG')
) AS v(code, description, sort_order, type)
WHERE m.code = 'ALTROZ' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TATA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('PURE_P', 'Pure',         1, 'PETROL'),
    ('ADV_P',  'Adventure',    2, 'PETROL'),
    ('ACC_P',  'Accomplished', 3, 'PETROL'),
    ('CRE_P',  'Creative',     4, 'PETROL'),
    ('PURE_C', 'Pure CNG',     5, 'CNG'),
    ('EV',     'EV',           6, 'EV')
) AS v(code, description, sort_order, type)
WHERE m.code = 'PUNCH' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TATA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('SMART_P',  'Smart',    1, 'PETROL'),
    ('PURE_P',   'Pure',     2, 'PETROL'),
    ('CRE_P',    'Creative', 3, 'PETROL'),
    ('FEAR_P',   'Fearless', 4, 'PETROL'),
    ('SMART_D',  'Smart',    5, 'DIESEL'),
    ('PURE_D',   'Pure',     6, 'DIESEL'),
    ('CRE_D',    'Creative', 7, 'DIESEL'),
    ('FEAR_D',   'Fearless', 8, 'DIESEL'),
    ('EV',       'EV',       9, 'EV')
) AS v(code, description, sort_order, type)
WHERE m.code = 'NEXON' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TATA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('SMART_P', 'Smart',        1, 'PETROL'),
    ('PURE_P',  'Pure',         2, 'PETROL'),
    ('CRE_P',   'Creative',     3, 'PETROL'),
    ('ACC_P',   'Accomplished', 4, 'PETROL'),
    ('SMART_D', 'Smart',        5, 'DIESEL'),
    ('PURE_D',  'Pure',         6, 'DIESEL'),
    ('CRE_D',   'Creative',     7, 'DIESEL'),
    ('EV',      'EV',           8, 'EV')
) AS v(code, description, sort_order, type)
WHERE m.code = 'CURVV' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TATA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('XE_D',  'XE',  1, 'DIESEL'),
    ('XM_D',  'XM',  2, 'DIESEL'),
    ('XT_D',  'XT',  3, 'DIESEL'),
    ('XZ_D',  'XZ',  4, 'DIESEL'),
    ('XZP_D', 'XZ+', 5, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'SAFARI' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TATA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LX_P', 'LX', 1, 'PETROL'),
    ('LS_P', 'LS', 2, 'PETROL'),
    ('LV_P', 'LV', 3, 'PETROL'),
    ('LX_D', 'LX', 4, 'DIESEL'),
    ('LS_D', 'LS', 5, 'DIESEL'),
    ('LV_D', 'LV', 6, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'INDICA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TATA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LS_P', 'LS', 1, 'PETROL'),
    ('LX_P', 'LX', 2, 'PETROL'),
    ('LV_P', 'LV', 3, 'PETROL'),
    ('LS_D', 'LS', 4, 'DIESEL'),
    ('LX_D', 'LX', 5, 'DIESEL'),
    ('LV_D', 'LV', 6, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'VISTA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TATA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('XE_P', 'XE', 1, 'PETROL'),
    ('XM_P', 'XM', 2, 'PETROL'),
    ('XT_P', 'XT', 3, 'PETROL'),
    ('XE_D', 'XE', 4, 'DIESEL'),
    ('XM_D', 'XM', 5, 'DIESEL'),
    ('XT_D', 'XT', 6, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'BOLT' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TATA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('STD_P', 'STD', 1, 'PETROL'),
    ('CX_P',  'CX',  2, 'PETROL'),
    ('LX_P',  'LX',  3, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'NANO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TATA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LX_D', 'LX', 1, 'DIESEL'),
    ('SE_D', 'SE', 2, 'DIESEL'),
    ('GX_D', 'GX', 3, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'SUMO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TATA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('PLEAS_D', 'Pleasure', 1, 'DIESEL'),
    ('PRIDE_D', 'Pride',    2, 'DIESEL'),
    ('PREST_D', 'Prestige', 3, 'DIESEL'),
    ('PURE_D',  'Pure',     4, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'ARIA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TATA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LX_D', 'LX', 1, 'DIESEL'),
    ('EX_D', 'EX', 2, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'VENTURE' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'TATA');

-- ---------- KIA ----------

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('HTE_P',  'HTE',  1, 'PETROL'),
    ('HTK_P',  'HTK',  2, 'PETROL'),
    ('HTKP_P', 'HTK+', 3, 'PETROL'),
    ('HTX_P',  'HTX',  4, 'PETROL'),
    ('HTXP_P', 'HTX+', 5, 'PETROL'),
    ('GTX_P',  'GTX',  6, 'PETROL'),
    ('GTXP_P', 'GTX+', 7, 'PETROL'),
    ('HTK_D',  'HTK',  8, 'DIESEL'),
    ('HTX_D',  'HTX',  9, 'DIESEL'),
    ('GTX_D',  'GTX', 10, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'SONET' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'KIA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('HTE_P',  'HTE',    1, 'PETROL'),
    ('HTK_P',  'HTK',    2, 'PETROL'),
    ('HTKP_P', 'HTK+',   3, 'PETROL'),
    ('HTX_P',  'HTX',    4, 'PETROL'),
    ('HTXP_P', 'HTX+',   5, 'PETROL'),
    ('GTX_P',  'GTX',    6, 'PETROL'),
    ('XLINE_P','X-Line', 7, 'PETROL'),
    ('HTK_D',  'HTK',    8, 'DIESEL'),
    ('HTX_D',  'HTX',    9, 'DIESEL'),
    ('GTX_D',  'GTX',   10, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'SELTOS' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'KIA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('HTK_P',  'HTK',  1, 'PETROL'),
    ('HTKP_P', 'HTK+', 2, 'PETROL'),
    ('HTX_P',  'HTX',  3, 'PETROL'),
    ('HTXP_P', 'HTX+', 4, 'PETROL'),
    ('GTX_P',  'GTX',  5, 'PETROL'),
    ('HTK_D',  'HTK',  6, 'DIESEL'),
    ('HTX_D',  'HTX',  7, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'SYROS' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'KIA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('PREM_D',  'Premium',   1, 'DIESEL'),
    ('PREST_D', 'Prestige',  2, 'DIESEL'),
    ('LIMO_D',  'Limousine', 3, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'CARNIVAL' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'KIA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('PREM_P',  'Premium',  1, 'PETROL'),
    ('PREST_P', 'Prestige', 2, 'PETROL'),
    ('LUX_P',   'Luxury',   3, 'PETROL'),
    ('LUXP_P',  'Luxury+',  4, 'PETROL'),
    ('PREM_D',  'Premium',  5, 'DIESEL'),
    ('PREST_D', 'Prestige', 6, 'DIESEL'),
    ('LUX_D',   'Luxury',   7, 'DIESEL'),
    ('LUXP_D',  'Luxury+',  8, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'CARENS' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'KIA');

-- ---------- MG MOTORS ----------

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('STYLE_P', 'Style', 1, 'PETROL'),
    ('SUPER_P', 'Super', 2, 'PETROL'),
    ('SMART_P', 'Smart', 3, 'PETROL'),
    ('SHARP_P', 'Sharp', 4, 'PETROL'),
    ('SAVVY_P', 'Savvy', 5, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'ASTOR' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MGMOTORS');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('STYLE_P', 'Style', 1, 'PETROL'),
    ('SUPER_P', 'Super', 2, 'PETROL'),
    ('SMART_P', 'Smart', 3, 'PETROL'),
    ('SHARP_P', 'Sharp', 4, 'PETROL'),
    ('SAVVY_P', 'Savvy', 5, 'PETROL'),
    ('STYLE_D', 'Style', 6, 'DIESEL'),
    ('SMART_D', 'Smart', 7, 'DIESEL'),
    ('SHARP_D', 'Sharp', 8, 'DIESEL'),
    ('SHARP_H', 'Sharp Hybrid', 9, 'HYBRID')
) AS v(code, description, sort_order, type)
WHERE m.code = 'HECTOR' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MGMOTORS');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('PACE_EV',  'Pace',  1, 'EV'),
    ('PLAY_EV',  'Play',  2, 'EV'),
    ('PLUSH_EV', 'Plush', 3, 'EV')
) AS v(code, description, sort_order, type)
WHERE m.code = 'COMET' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MGMOTORS');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('EXCITE_EV', 'Excite',    1, 'EV'),
    ('EXCL_EV',   'Exclusive', 2, 'EV'),
    ('ESS_EV',    'Essence',   3, 'EV')
) AS v(code, description, sort_order, type)
WHERE m.code = 'WINDSOR' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MGMOTORS');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('EXCITE_EV', 'Excite',    1, 'EV'),
    ('EXCL_EV',   'Exclusive', 2, 'EV')
) AS v(code, description, sort_order, type)
WHERE m.code = 'ZSEV' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MGMOTORS');

-- ---------- MAHINDRA ----------

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('B4_D',  'B4',    1, 'DIESEL'),
    ('B6_D',  'B6',    2, 'DIESEL'),
    ('B6O_D', 'B6(O)', 3, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'BOLERO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MAHINDRA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('N4_D',   'N4',     1, 'DIESEL'),
    ('N8_D',   'N8',     2, 'DIESEL'),
    ('N10_D',  'N10',    3, 'DIESEL'),
    ('N10O_D', 'N10(O)', 4, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'BOLERONEO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MAHINDRA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('AX_D',  'AX',    1, 'DIESEL'),
    ('AXO_D', 'AX(O)', 2, 'DIESEL'),
    ('LX_D',  'LX',    3, 'DIESEL'),
    ('AX_P',  'AX',    4, 'PETROL'),
    ('LX_P',  'LX',    5, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'THAR' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MAHINDRA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('MX1_P',  'MX1',  1, 'PETROL'),
    ('MX3_P',  'MX3',  2, 'PETROL'),
    ('MX5_P',  'MX5',  3, 'PETROL'),
    ('AX3L_P', 'AX3L', 4, 'PETROL'),
    ('AX5L_P', 'AX5L', 5, 'PETROL'),
    ('AX7L_P', 'AX7L', 6, 'PETROL'),
    ('MX1_D',  'MX1',  7, 'DIESEL'),
    ('MX3_D',  'MX3',  8, 'DIESEL'),
    ('AX5L_D', 'AX5L', 9, 'DIESEL'),
    ('AX7L_D', 'AX7L', 10, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'THARROXX' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MAHINDRA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('S_D',   'S',   1, 'DIESEL'),
    ('S11_D', 'S11', 2, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'SCORPIOCLASSIC' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MAHINDRA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('Z2_P',  'Z2',  1, 'PETROL'),
    ('Z4_P',  'Z4',  2, 'PETROL'),
    ('Z6_P',  'Z6',  3, 'PETROL'),
    ('Z8_P',  'Z8',  4, 'PETROL'),
    ('Z8L_P', 'Z8L', 5, 'PETROL'),
    ('Z2_D',  'Z2',  6, 'DIESEL'),
    ('Z4_D',  'Z4',  7, 'DIESEL'),
    ('Z6_D',  'Z6',  8, 'DIESEL'),
    ('Z8_D',  'Z8',  9, 'DIESEL'),
    ('Z8L_D', 'Z8L', 10, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'SCORPION' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MAHINDRA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('MX1_P', 'MX1', 1, 'PETROL'),
    ('MX2_P', 'MX2', 2, 'PETROL'),
    ('MX3_P', 'MX3', 3, 'PETROL'),
    ('AX5_P', 'AX5', 4, 'PETROL'),
    ('AX7_P', 'AX7', 5, 'PETROL'),
    ('MX1_D', 'MX1', 6, 'DIESEL'),
    ('AX5_D', 'AX5', 7, 'DIESEL'),
    ('AX7_D', 'AX7', 8, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'XUV3XO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MAHINDRA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('MX_P',  'MX',  1, 'PETROL'),
    ('AX3_P', 'AX3', 2, 'PETROL'),
    ('AX5_P', 'AX5', 3, 'PETROL'),
    ('AX7_P', 'AX7', 4, 'PETROL'),
    ('MX_D',  'MX',  5, 'DIESEL'),
    ('AX3_D', 'AX3', 6, 'DIESEL'),
    ('AX5_D', 'AX5', 7, 'DIESEL'),
    ('AX7_D', 'AX7', 8, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'XUV700' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MAHINDRA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('P1_EV', 'Pack 1', 1, 'EV'),
    ('P2_EV', 'Pack 2', 2, 'EV'),
    ('P3_EV', 'Pack 3', 3, 'EV')
) AS v(code, description, sort_order, type)
WHERE m.code = 'BE6' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MAHINDRA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('P1_EV', 'Pack 1', 1, 'EV'),
    ('P2_EV', 'Pack 2', 2, 'EV'),
    ('P3_EV', 'Pack 3', 3, 'EV')
) AS v(code, description, sort_order, type)
WHERE m.code = 'XEV9E' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MAHINDRA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('P1_EV', 'Pack 1', 1, 'EV'),
    ('P2_EV', 'Pack 2', 2, 'EV')
) AS v(code, description, sort_order, type)
WHERE m.code = 'XEV9S' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MAHINDRA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('MX1_EV', 'MX1', 1, 'EV'),
    ('MX2_EV', 'MX2', 2, 'EV'),
    ('AX5_EV', 'AX5', 3, 'EV'),
    ('AX7_EV', 'AX7', 4, 'EV')
) AS v(code, description, sort_order, type)
WHERE m.code = 'XUV3XOEV' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MAHINDRA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('BASE_P', 'Base', 1, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'VOYAGER' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MAHINDRA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('BASE_D', 'Base', 1, 'DIESEL'),
    ('DI_D',   'DI',   2, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'MAJOR' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MAHINDRA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('D2_D', 'D2', 1, 'DIESEL'),
    ('D4_D', 'D4', 2, 'DIESEL'),
    ('E4_D', 'E4', 3, 'DIESEL'),
    ('E8_D', 'E8', 4, 'DIESEL'),
    ('E9_D', 'E9', 5, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'XYLO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MAHINDRA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('C2_D', 'C2', 1, 'DIESEL'),
    ('C4_D', 'C4', 2, 'DIESEL'),
    ('C6_D', 'C6', 3, 'DIESEL'),
    ('C8_D', 'C8', 4, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'QUANTO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MAHINDRA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('T4_D',  'T4',  1, 'DIESEL'),
    ('T6_D',  'T6',  2, 'DIESEL'),
    ('T8_D',  'T8',  3, 'DIESEL'),
    ('T10_D', 'T10', 4, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'TUV300' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MAHINDRA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('W4_D',  'W4',  1, 'DIESEL'),
    ('W6_D',  'W6',  2, 'DIESEL'),
    ('W8_D',  'W8',  3, 'DIESEL'),
    ('W10_D', 'W10', 4, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'XUV500' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MAHINDRA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('D2_D', 'D2', 1, 'DIESEL'),
    ('D4_D', 'D4', 2, 'DIESEL'),
    ('D6_D', 'D6', 3, 'DIESEL'),
    ('D2_P', 'D2', 4, 'PETROL'),
    ('D6_P', 'D6', 5, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'VERITO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MAHINDRA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('T0_EV', 'T0', 1, 'EV'),
    ('T2_EV', 'T2', 2, 'EV'),
    ('T4_EV', 'T4', 3, 'EV')
) AS v(code, description, sort_order, type)
WHERE m.code = 'E2O' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MAHINDRA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('K2_P', 'K2', 1, 'PETROL'),
    ('K4_P', 'K4', 2, 'PETROL'),
    ('K6_P', 'K6', 3, 'PETROL'),
    ('K8_P', 'K8', 4, 'PETROL'),
    ('K2_D', 'K2', 5, 'DIESEL'),
    ('K4_D', 'K4', 6, 'DIESEL'),
    ('K6_D', 'K6', 7, 'DIESEL'),
    ('K8_D', 'K8', 8, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'KUV100' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MAHINDRA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('M2_D', 'M2', 1, 'DIESEL'),
    ('M4_D', 'M4', 2, 'DIESEL'),
    ('M6_D', 'M6', 3, 'DIESEL'),
    ('M8_D', 'M8', 4, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'MARAZZO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'MAHINDRA');

-- ---------- JEEP ----------

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('SPORT_P', 'Sport',      1, 'PETROL'),
    ('LONG_P',  'Longitude',  2, 'PETROL'),
    ('LIM_P',   'Limited',    3, 'PETROL'),
    ('SPORT_D', 'Sport',      4, 'DIESEL'),
    ('LONG_D',  'Longitude',  5, 'DIESEL'),
    ('LIM_D',   'Limited',    6, 'DIESEL'),
    ('TRAIL_D', 'Trailhawk',  7, 'DIESEL'),
    ('MODELS',  'Model S',    8, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'COMPASS' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'JEEP');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LIM_D',  'Limited',    1, 'DIESEL'),
    ('LIMO_D', 'Limited(O)', 2, 'DIESEL'),
    ('OVER_D', 'Overland',   3, 'DIESEL'),
    ('4X4_D',  '4x4',        4, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'MERIDIAN' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'JEEP');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('RUB_P',  'Rubicon',   1, 'PETROL'),
    ('SAH_P',  'Sahara',    2, 'PETROL'),
    ('UNLI_P', 'Unlimited', 3, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'WRANGLER' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'JEEP');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('SUM_P',   'Summit',    1, 'PETROL'),
    ('TRAIL_P', 'Trailhawk', 2, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'GRANDCHEROKEE' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'JEEP');

-- ---------- HONDA ----------

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('E_P',  'E',  1, 'PETROL'),
    ('S_P',  'S',  2, 'PETROL'),
    ('V_P',  'V',  3, 'PETROL'),
    ('VX_P', 'VX', 4, 'PETROL'),
    ('E_D',  'E',  5, 'DIESEL'),
    ('S_D',  'S',  6, 'DIESEL'),
    ('V_D',  'V',  7, 'DIESEL'),
    ('VX_D', 'VX', 8, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'AMAZE' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HONDA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('V_P',  'V',     1, 'PETROL'),
    ('VX_P', 'VX',    2, 'PETROL'),
    ('ZX_P', 'ZX',    3, 'PETROL'),
    ('V_D',  'V',     4, 'DIESEL'),
    ('VX_D', 'VX',    5, 'DIESEL'),
    ('ZX_D', 'ZX',    6, 'DIESEL'),
    ('EHEV', 'e:HEV', 7, 'HYBRID')
) AS v(code, description, sort_order, type)
WHERE m.code = 'CITY' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HONDA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('SV_P', 'SV', 1, 'PETROL'),
    ('V_P',  'V',  2, 'PETROL'),
    ('VX_P', 'VX', 3, 'PETROL'),
    ('ZX_P', 'ZX', 4, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'ELEVATE' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HONDA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('E_P',  'E',  1, 'PETROL'),
    ('S_P',  'S',  2, 'PETROL'),
    ('V_P',  'V',  3, 'PETROL'),
    ('VX_P', 'VX', 4, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'BRIO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HONDA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('V_P',  'V',  1, 'PETROL'),
    ('VX_P', 'VX', 2, 'PETROL'),
    ('ZX_P', 'ZX', 3, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'JAZZ' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HONDA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('V_P',  'V',  1, 'PETROL'),
    ('VX_P', 'VX', 2, 'PETROL'),
    ('ZX_P', 'ZX', 3, 'PETROL'),
    ('V_D',  'V',  4, 'DIESEL'),
    ('VX_D', 'VX', 5, 'DIESEL'),
    ('ZX_D', 'ZX', 6, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'CIVIC' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HONDA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('VTI_P',  'VTi',    1, 'PETROL'),
    ('VTIL_P', 'VTi-L',  2, 'PETROL'),
    ('HYB_H',  'Hybrid', 3, 'HYBRID')
) AS v(code, description, sort_order, type)
WHERE m.code = 'ACCORD' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HONDA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('S_P',    'S',     1, 'PETROL'),
    ('VX_P',   'VX',    2, 'PETROL'),
    ('EDGE_P', 'Edge',  3, 'PETROL'),
    ('S_D',    'S',     4, 'DIESEL'),
    ('VX_D',   'VX',    5, 'DIESEL'),
    ('EDGE_D', 'Edge',  6, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'WRV' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HONDA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('S_P',  'S',  1, 'PETROL'),
    ('V_P',  'V',  2, 'PETROL'),
    ('VX_P', 'VX', 3, 'PETROL'),
    ('S_D',  'S',  4, 'DIESEL'),
    ('V_D',  'V',  5, 'DIESEL'),
    ('VX_D', 'VX', 6, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'BRV' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HONDA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('2WD_P', '2WD', 1, 'PETROL'),
    ('4WD_P', '4WD', 2, 'PETROL'),
    ('2WD_D', '2WD', 3, 'DIESEL'),
    ('4WD_D', '4WD', 4, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'CRV' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HONDA');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('E_P',  'E',  1, 'PETROL'),
    ('S_P',  'S',  2, 'PETROL'),
    ('V_P',  'V',  3, 'PETROL'),
    ('VX_P', 'VX', 4, 'PETROL'),
    ('E_D',  'E',  5, 'DIESEL'),
    ('S_D',  'S',  6, 'DIESEL'),
    ('V_D',  'V',  7, 'DIESEL'),
    ('VX_D', 'VX', 8, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'MOBILIO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'HONDA');

-- ---------- RENAULT ----------

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('RXE_P',  'RxE',     1, 'PETROL'),
    ('RXL_P',  'RxL',     2, 'PETROL'),
    ('RXT_P',  'RxT',     3, 'PETROL'),
    ('CLIM_P', 'Climber', 4, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'KWID' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'RENAULT');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('RXE_P', 'RxE', 1, 'PETROL'),
    ('RXL_P', 'RxL', 2, 'PETROL'),
    ('RXT_P', 'RxT', 3, 'PETROL'),
    ('RXZ_P', 'RxZ', 4, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'TRIBER' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'RENAULT');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('RXE_P', 'RxE', 1, 'PETROL'),
    ('RXL_P', 'RxL', 2, 'PETROL'),
    ('RXT_P', 'RxT', 3, 'PETROL'),
    ('RXZ_P', 'RxZ', 4, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'KIGER' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'RENAULT');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('RXE_P', 'RxE', 1, 'PETROL'),
    ('RXL_P', 'RxL', 2, 'PETROL'),
    ('RXS_P', 'RxS', 3, 'PETROL'),
    ('RXZ_P', 'RxZ', 4, 'PETROL'),
    ('RXE_D', 'RxE', 5, 'DIESEL'),
    ('RXL_D', 'RxL', 6, 'DIESEL'),
    ('RXS_D', 'RxS', 7, 'DIESEL'),
    ('RXZ_D', 'RxZ', 8, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'DUSTER' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'RENAULT');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('RXE_P', 'RxE', 1, 'PETROL'),
    ('RXL_P', 'RxL', 2, 'PETROL'),
    ('RXZ_P', 'RxZ', 3, 'PETROL'),
    ('RXL_D', 'RxL', 4, 'DIESEL'),
    ('RXZ_D', 'RxZ', 5, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'PULSE' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'RENAULT');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('RXE_P', 'RxE', 1, 'PETROL'),
    ('RXL_P', 'RxL', 2, 'PETROL'),
    ('RXZ_P', 'RxZ', 3, 'PETROL'),
    ('RXL_D', 'RxL', 4, 'DIESEL'),
    ('RXZ_D', 'RxZ', 5, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'SCALA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'RENAULT');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('E2_D', 'E2', 1, 'DIESEL'),
    ('E4_D', 'E4', 2, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'FLUENCE' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'RENAULT');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('RXE_D', 'RxE', 1, 'DIESEL'),
    ('RXL_D', 'RxL', 2, 'DIESEL'),
    ('RXZ_D', 'RxZ', 3, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'LODGY' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'RENAULT');

-- ---------- NISSAN ----------

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('XE_P',   'XE',         1, 'PETROL'),
    ('XL_P',   'XL',         2, 'PETROL'),
    ('XV_P',   'XV',         3, 'PETROL'),
    ('XVP_P',  'XV Premium', 4, 'PETROL'),
    ('XL_D',   'XL',         5, 'DIESEL'),
    ('XV_D',   'XV',         6, 'DIESEL'),
    ('XVP_D',  'XV Premium', 7, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'MICRA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'NISSAN');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('XE_P', 'XE', 1, 'PETROL'),
    ('XL_P', 'XL', 2, 'PETROL'),
    ('XV_P', 'XV', 3, 'PETROL'),
    ('XL_D', 'XL', 4, 'DIESEL'),
    ('XV_D', 'XV', 5, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'SUNNY' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'NISSAN');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('XE_D', 'XE', 1, 'DIESEL'),
    ('XL_D', 'XL', 2, 'DIESEL'),
    ('XV_D', 'XV', 3, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'EVALIA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'NISSAN');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('XE_P', 'XE', 1, 'PETROL'),
    ('XL_P', 'XL', 2, 'PETROL'),
    ('XV_P', 'XV', 3, 'PETROL'),
    ('XE_D', 'XE', 4, 'DIESEL'),
    ('XL_D', 'XL', 5, 'DIESEL'),
    ('XV_D', 'XV', 6, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'TERRANO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'NISSAN');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('XL_P',  'XL',         1, 'PETROL'),
    ('XV_P',  'XV',         2, 'PETROL'),
    ('XVP_P', 'XV Premium', 3, 'PETROL'),
    ('XL_D',  'XL',         4, 'DIESEL'),
    ('XV_D',  'XV',         5, 'DIESEL'),
    ('XVP_D', 'XV Premium', 6, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'KICKS' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'NISSAN');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('COMF_P',  'Comfort', 1, 'PETROL'),
    ('PREM_P',  'Premium', 2, 'PETROL'),
    ('PREM_H',  'Premium Hybrid', 3, 'HYBRID')
) AS v(code, description, sort_order, type)
WHERE m.code = 'XTRAIL' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'NISSAN');

-- ---------- VOLKSWAGEN ----------

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('STD_P', 'Standard', 1, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'GOLFGTI' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'VOLKSWAGEN');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('COMF_P', 'Comfortline', 1, 'PETROL'),
    ('HIGH_P', 'Highline',    2, 'PETROL'),
    ('TOP_P',  'Topline',     3, 'PETROL'),
    ('GT_P',   'GT',          4, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'VIRTUS' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'VOLKSWAGEN');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('COMF_P', 'Comfortline', 1, 'PETROL'),
    ('HIGH_P', 'Highline',    2, 'PETROL'),
    ('TOP_P',  'Topline',     3, 'PETROL'),
    ('GT_P',   'GT',          4, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'TAIGUN' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'VOLKSWAGEN');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('TREND_P', 'Trendline',   1, 'PETROL'),
    ('COMF_P',  'Comfortline', 2, 'PETROL'),
    ('HIGH_P',  'Highline',    3, 'PETROL'),
    ('GT_P',    'GT',          4, 'PETROL'),
    ('TREND_D', 'Trendline',   5, 'DIESEL'),
    ('COMF_D',  'Comfortline', 6, 'DIESEL'),
    ('HIGH_D',  'Highline',    7, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'POLO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'VOLKSWAGEN');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('STD_P', 'Standard', 1, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'POLOGTI' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'VOLKSWAGEN');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('TREND_P', 'Trendline',   1, 'PETROL'),
    ('COMF_P',  'Comfortline', 2, 'PETROL'),
    ('HIGH_P',  'Highline',    3, 'PETROL'),
    ('TREND_D', 'Trendline',   4, 'DIESEL'),
    ('COMF_D',  'Comfortline', 5, 'DIESEL'),
    ('HIGH_D',  'Highline',    6, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'VENTO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'VOLKSWAGEN');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('TREND_P', 'Trendline',   1, 'PETROL'),
    ('COMF_P',  'Comfortline', 2, 'PETROL'),
    ('HIGH_P',  'Highline',    3, 'PETROL'),
    ('COMF_D',  'Comfortline', 4, 'DIESEL'),
    ('HIGH_D',  'Highline',    5, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'AMEO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'VOLKSWAGEN');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('TREND_D', 'Trendline',   1, 'DIESEL'),
    ('COMF_D',  'Comfortline', 2, 'DIESEL'),
    ('HIGH_D',  'Highline',    3, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'JETTA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'VOLKSWAGEN');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('COMF_D', 'Comfortline', 1, 'DIESEL'),
    ('HIGH_D', 'Highline',    2, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'PASSAT' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'VOLKSWAGEN');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('STD_P', 'Standard', 1, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'BEETLE' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'VOLKSWAGEN');

-- ---------- CHEVROLET ----------

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LS_P', 'LS', 1, 'PETROL'),
    ('LT_P', 'LT', 2, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'SPARK' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'CHEVROLET');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LS_P',  'LS',  1, 'PETROL'),
    ('LT_P',  'LT',  2, 'PETROL'),
    ('LTZ_P', 'LTZ', 3, 'PETROL'),
    ('LS_D',  'LS',  4, 'DIESEL'),
    ('LT_D',  'LT',  5, 'DIESEL'),
    ('LTZ_D', 'LTZ', 6, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'BEAT' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'CHEVROLET');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('BASE_P', 'Base', 1, 'PETROL'),
    ('LS_P',   'LS',   2, 'PETROL'),
    ('LT_P',   'LT',   3, 'PETROL'),
    ('BASE_D', 'Base', 4, 'DIESEL'),
    ('LS_D',   'LS',   5, 'DIESEL'),
    ('LT_D',   'LT',   6, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'SAIL' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'CHEVROLET');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LS_P', 'LS', 1, 'PETROL'),
    ('LT_P', 'LT', 2, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'AVEO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'CHEVROLET');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LS_P',  'LS',     1, 'PETROL'),
    ('LT_P',  'LT',     2, 'PETROL'),
    ('LS_D',  'LS',     3, 'DIESEL'),
    ('LT_D',  'LT',     4, 'DIESEL'),
    ('MAG_D', 'Magnum', 5, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'OPTRA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'CHEVROLET');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LT_D',  'LT',  1, 'DIESEL'),
    ('LTZ_D', 'LTZ', 2, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'CRUZE' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'CHEVROLET');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('BASE_P', 'Base', 1, 'PETROL'),
    ('LS_P',   'LS',   2, 'PETROL'),
    ('LT_P',   'LT',   3, 'PETROL'),
    ('LTZ_P',  'LTZ',  4, 'PETROL'),
    ('BASE_D', 'Base', 5, 'DIESEL'),
    ('LS_D',   'LS',   6, 'DIESEL'),
    ('LT_D',   'LT',   7, 'DIESEL'),
    ('LTZ_D',  'LTZ',  8, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'ENJOY' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'CHEVROLET');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('B1_D', 'B1', 1, 'DIESEL'),
    ('B2_D', 'B2', 2, 'DIESEL'),
    ('LS_D', 'LS', 3, 'DIESEL'),
    ('LT_D', 'LT', 4, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'TAVERA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'CHEVROLET');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('LT_D',  'LT',  1, 'DIESEL'),
    ('LTZ_D', 'LTZ', 2, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'CAPTIVA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'CHEVROLET');

-- ---------- FORD ----------

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('AMB_P',   'Ambiente',   1, 'PETROL'),
    ('TREND_P', 'Trend',      2, 'PETROL'),
    ('TIT_P',   'Titanium',   3, 'PETROL'),
    ('TITP_P',  'Titanium+',  4, 'PETROL'),
    ('AMB_D',   'Ambiente',   5, 'DIESEL'),
    ('TREND_D', 'Trend',      6, 'DIESEL'),
    ('TIT_D',   'Titanium',   7, 'DIESEL'),
    ('TITP_D',  'Titanium+',  8, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'FIGO' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'FORD');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('AMB_P',   'Ambiente', 1, 'PETROL'),
    ('TREND_P', 'Trend',    2, 'PETROL'),
    ('TIT_P',   'Titanium', 3, 'PETROL'),
    ('AMB_D',   'Ambiente', 4, 'DIESEL'),
    ('TREND_D', 'Trend',    5, 'DIESEL'),
    ('TIT_D',   'Titanium', 6, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'FIESTA' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'FORD');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('AMB_P',    'Ambiente',  1, 'PETROL'),
    ('TREND_P',  'Trend',     2, 'PETROL'),
    ('TIT_P',    'Titanium',  3, 'PETROL'),
    ('TITP_P',   'Titanium+', 4, 'PETROL'),
    ('SPORT_P',  'Sport',     5, 'PETROL'),
    ('AMB_D',    'Ambiente',  6, 'DIESEL'),
    ('TREND_D',  'Trend',     7, 'DIESEL'),
    ('TIT_D',    'Titanium',  8, 'DIESEL'),
    ('TITP_D',   'Titanium+', 9, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'ECOSPORT' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'FORD');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('AMB_P',   'Ambiente',  1, 'PETROL'),
    ('TREND_P', 'Trend',     2, 'PETROL'),
    ('TIT_P',   'Titanium',  3, 'PETROL'),
    ('TITP_P',  'Titanium+', 4, 'PETROL'),
    ('AMB_D',   'Ambiente',  5, 'DIESEL'),
    ('TREND_D', 'Trend',     6, 'DIESEL'),
    ('TIT_D',   'Titanium',  7, 'DIESEL'),
    ('TITP_D',  'Titanium+', 8, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'FREESTYLE' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'FORD');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('FLAIR_P', 'Flair', 1, 'PETROL'),
    ('NXT_P',   'NXT',   2, 'PETROL'),
    ('ZXI_P',   'ZXi',   3, 'PETROL'),
    ('FLAIR_D', 'Flair', 4, 'DIESEL'),
    ('NXT_D',   'NXT',   5, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'IKON' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'FORD');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('TREND_D', 'Trend',    1, 'DIESEL'),
    ('TIT_D',   'Titanium', 2, 'DIESEL'),
    ('SPORT_D', 'Sport',    3, 'DIESEL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'ENDEAVOUR' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'FORD');

INSERT INTO fnd_model_varient (model_id, code, description, sort_order, active, type)
SELECT m.id, v.code, v.description, v.sort_order, true, v.type
FROM fnd_brand_model m
CROSS JOIN (VALUES
    ('BASE_P', 'Base', 1, 'PETROL'),
    ('PLUS_P', 'Plus', 2, 'PETROL')
) AS v(code, description, sort_order, type)
WHERE m.code = 'FUSION' AND m.brand_id = (SELECT id FROM fnd_product_brand WHERE code = 'FORD');
