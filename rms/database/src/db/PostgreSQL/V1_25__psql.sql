ALTER TABLE membership ADD COLUMN inviter_id integer;
ALTER TABLE membership ADD COLUMN invited_on timestamp WITH TIME ZONE;