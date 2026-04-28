-- Default admin for development only
-- Password: Admin@123 (BCrypt cost 12)
INSERT INTO users (email, full_name, global_role, password_hash, is_active)
VALUES (
    'admin@darkness.com',
    'System Admin',
    'ADMIN',
    '$2a$12$YfA9mMBuwkwAWT28.BgByuJGJUyzn.wADJYIF6Ko9bCHTZPRSyJey',
    TRUE
) ON CONFLICT (email) DO NOTHING;
