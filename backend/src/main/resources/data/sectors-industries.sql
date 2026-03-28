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

INSERT INTO industries (name, description, sector_id) VALUES
('Software', 'Software & Services', (SELECT id FROM sectors WHERE name = 'Technology')),
('Semiconductors', 'Semiconductors & Equipment', (SELECT id FROM sectors WHERE name = 'Technology')),
('Hardware', 'Technology Hardware & Equipment', (SELECT id FROM sectors WHERE name = 'Technology'));

INSERT INTO industries (name, description, sector_id) VALUES
('Pharmaceuticals', 'Pharmaceuticals', (SELECT id FROM sectors WHERE name = 'Healthcare')),
('Biotechnology', 'Biotechnology', (SELECT id FROM sectors WHERE name = 'Healthcare')),
('Medical Devices', 'Healthcare Equipment & Supplies', (SELECT id FROM sectors WHERE name = 'Healthcare'));

INSERT INTO industries (name, description, sector_id) VALUES
('Banks', 'Banks', (SELECT id FROM sectors WHERE name = 'Financials')),
('Insurance', 'Insurance', (SELECT id FROM sectors WHERE name = 'Financials')),
('Capital Markets', 'Capital Markets', (SELECT id FROM sectors WHERE name = 'Financials'));

INSERT INTO industries (name, description, sector_id) VALUES
('Oil & Gas', 'Oil, Gas & Consumable Fuels', (SELECT id FROM sectors WHERE name = 'Energy')),
('Energy Equipment', 'Energy Equipment & Services', (SELECT id FROM sectors WHERE name = 'Energy'));

INSERT INTO industries (name, description, sector_id) VALUES
('Retail', 'Retail', (SELECT id FROM sectors WHERE name = 'Consumer Discretionary')),
('Automobiles', 'Automobiles & Components', (SELECT id FROM sectors WHERE name = 'Consumer Discretionary'));

INSERT INTO industries (name, description, sector_id) VALUES
('Food & Beverage', 'Food, Beverage & Tobacco', (SELECT id FROM sectors WHERE name = 'Consumer Staples')),
('Household Products', 'Household & Personal Products', (SELECT id FROM sectors WHERE name = 'Consumer Staples'));

INSERT INTO industries (name, description, sector_id) VALUES
('Aerospace & Defense', 'Aerospace & Defense', (SELECT id FROM sectors WHERE name = 'Industrials')),
('Transportation', 'Transportation', (SELECT id FROM sectors WHERE name = 'Industrials'));

INSERT INTO industries (name, description, sector_id) VALUES
('Chemicals', 'Chemicals', (SELECT id FROM sectors WHERE name = 'Materials')),
('Metals & Mining', 'Metals & Mining', (SELECT id FROM sectors WHERE name = 'Materials'));

INSERT INTO industries (name, description, sector_id) VALUES
('REITs', 'Equity Real Estate Investment Trusts', (SELECT id FROM sectors WHERE name = 'Real Estate'));

INSERT INTO industries (name, description, sector_id) VALUES
('Electric Utilities', 'Electric Utilities', (SELECT id FROM sectors WHERE name = 'Utilities'));

INSERT INTO industries (name, description, sector_id) VALUES
('Media', 'Media & Entertainment', (SELECT id FROM sectors WHERE name = 'Communication Services')),
('Telecom', 'Telecommunication Services', (SELECT id FROM sectors WHERE name = 'Communication Services'));
