ALTER TABLE notifications ADD COLUMN error VARCHAR(500);
ALTER TABLE notifications ADD CONSTRAINT chk_notifications_status CHECK (status IN ('PENDING', 'SENT', 'SKIPPED', 'FAILED'));
CREATE INDEX idx_notifications_created ON notifications(created_at DESC);
