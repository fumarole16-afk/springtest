-- One-time cleanup: any stocks pointing to a non-canonical (non-MIN id) industry
-- are re-pointed to the canonical row so duplicates can be safely deleted.
UPDATE stocks s
SET industry_id = canon.min_id
FROM (
    SELECT i.id AS dup_id, m.min_id
    FROM industries i
    JOIN (SELECT name, MIN(id) AS min_id FROM industries GROUP BY name) m ON i.name = m.name
    WHERE i.id <> m.min_id
) canon
WHERE s.industry_id = canon.dup_id;

DELETE FROM industry_metrics WHERE industry_id IN (
    SELECT i.id FROM industries i
    JOIN (SELECT name, MIN(id) AS min_id FROM industries GROUP BY name) m ON i.name = m.name
    WHERE i.id <> m.min_id
);

DELETE FROM industries WHERE id NOT IN (SELECT MIN(id) FROM industries GROUP BY name);

INSERT INTO sectors (name, description) VALUES
('Technology', 'Information Technology'),
('Healthcare', 'Healthcare'),
('Financials', 'Financials'),
('Consumer Discretionary', 'Consumer Discretionary'),
('Consumer Staples', 'Consumer Staples'),
('Energy', 'Energy'),
('Industrials', 'Industrials'),
('Materials', 'Materials'),
('Real Estate', 'Real Estate'),
('Utilities', 'Utilities'),
('Communication Services', 'Communication Services')
ON CONFLICT (name) DO NOTHING;

-- Idempotent industry upserts via WHERE NOT EXISTS (industries.name has no unique
-- constraint, so ON CONFLICT can't be used directly).
INSERT INTO industries (name, description, sector_id)
SELECT v.name, v.description, s.id FROM (VALUES
    ('Software', 'Software & Services', 'Technology'),
    ('Semiconductors', 'Semiconductors & Equipment', 'Technology'),
    ('Hardware', 'Technology Hardware & Equipment', 'Technology'),
    ('Pharmaceuticals', 'Pharmaceuticals', 'Healthcare'),
    ('Biotechnology', 'Biotechnology', 'Healthcare'),
    ('Medical Devices', 'Healthcare Equipment & Supplies', 'Healthcare'),
    ('Banks', 'Banks', 'Financials'),
    ('Insurance', 'Insurance', 'Financials'),
    ('Capital Markets', 'Capital Markets', 'Financials'),
    ('Oil & Gas', 'Oil, Gas & Consumable Fuels', 'Energy'),
    ('Energy Equipment', 'Energy Equipment & Services', 'Energy'),
    ('Retail', 'Retail', 'Consumer Discretionary'),
    ('Automobiles', 'Automobiles & Components', 'Consumer Discretionary'),
    ('Food & Beverage', 'Food, Beverage & Tobacco', 'Consumer Staples'),
    ('Household Products', 'Household & Personal Products', 'Consumer Staples'),
    ('Aerospace & Defense', 'Aerospace & Defense', 'Industrials'),
    ('Transportation', 'Transportation', 'Industrials'),
    ('Chemicals', 'Chemicals', 'Materials'),
    ('Metals & Mining', 'Metals & Mining', 'Materials'),
    ('REITs', 'Equity Real Estate Investment Trusts', 'Real Estate'),
    ('Electric Utilities', 'Electric Utilities', 'Utilities'),
    ('Media', 'Media & Entertainment', 'Communication Services'),
    ('Telecom', 'Telecommunication Services', 'Communication Services')
) AS v(name, description, sector_name)
JOIN sectors s ON s.name = v.sector_name
WHERE NOT EXISTS (SELECT 1 FROM industries i WHERE i.name = v.name);
