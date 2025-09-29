-- V7__remove_uuid_from_roles.sql
ALTER TABLE roles DROP COLUMN IF EXISTS uuid;
