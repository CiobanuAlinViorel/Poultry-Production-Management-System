-- Notification extends BaseEntity, which carries created_by / updated_by.
ALTER TABLE notification.notification ADD COLUMN IF NOT EXISTS created_by UUID;
ALTER TABLE notification.notification ADD COLUMN IF NOT EXISTS updated_by UUID;
