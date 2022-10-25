SET IDENTITY_INSERT rms.users ON
GO
INSERT INTO rms.users (id, display_name, creation_time) VALUES (0, 'SYSTEM', CURRENT_TIMESTAMP);
GO
SET IDENTITY_INSERT rms.users OFF
GO