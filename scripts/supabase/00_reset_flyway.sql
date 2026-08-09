-- Run once before the first backend start against Supabase if Flyway failed mid-migration.
-- Safe on a fresh project: only drops Flyway's tracking table, not app data.

DROP TABLE IF EXISTS flyway_schema_history CASCADE;
