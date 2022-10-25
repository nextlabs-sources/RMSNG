DROP TRIGGER IF EXISTS insert_user_trigger ON "user";
DROP FUNCTION IF EXISTS insert_user_preferences();

CREATE FUNCTION insert_user_preferences() RETURNS trigger AS $$
BEGIN
	INSERT INTO user_preferences(id, expiry, watermark)
	VALUES(NEW.id, '{"option":0}', '$(User)\n$(Date) $(Time)');

	RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER insert_user_trigger
AFTER INSERT
	ON "user"
FOR EACH ROW
EXECUTE PROCEDURE insert_user_preferences();

UPDATE user_preferences SET watermark =  '$(User)\n$(Date) $(Time)' 
WHERE watermark IS NULL;