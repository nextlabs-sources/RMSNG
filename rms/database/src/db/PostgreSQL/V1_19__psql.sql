UPDATE all_nxl SET is_shared = TRUE WHERE is_shared IS NULL;
ALTER TABLE all_nxl ALTER COLUMN is_shared SET NOT NULL;