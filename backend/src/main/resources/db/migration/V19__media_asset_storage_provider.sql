ALTER TABLE media_asset
    ADD COLUMN IF NOT EXISTS storage_provider VARCHAR(20);

UPDATE media_asset
SET storage_provider = 'CLOUDINARY'
WHERE storage_provider IS NULL;

ALTER TABLE media_asset
    ALTER COLUMN storage_provider SET NOT NULL;

ALTER TABLE media_asset
    ALTER COLUMN storage_provider SET DEFAULT 'CLOUDINARY';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_media_storage_provider'
    ) THEN
        ALTER TABLE media_asset
            ADD CONSTRAINT chk_media_storage_provider
                CHECK (storage_provider IN ('CLOUDINARY', 'S3'));
    END IF;
END $$;

COMMENT ON COLUMN media_asset.s3_bucket IS 'Provider namespace: Cloudinary cloud name or S3 bucket';
COMMENT ON COLUMN media_asset.s3_key IS 'Logical storage key (provider-neutral)';
COMMENT ON COLUMN media_asset.storage_provider IS 'Storage backend that owns this object';
