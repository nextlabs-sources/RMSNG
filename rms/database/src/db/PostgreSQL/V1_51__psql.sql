CREATE TABLE user_preferences (
	id SERIAL,
	expiry text,
	watermark character varying(255),
	preferences text,
	PRIMARY KEY (id), 
	CONSTRAINT fk_user_id FOREIGN KEY (id) REFERENCES "user"(id) ON DELETE CASCADE
);

INSERT INTO user_preferences (id, preferences, expiry)
SELECT id, preferences, '{"option":0}' FROM "user" ORDER BY id;

ALTER TABLE "user" DROP COLUMN preferences;

CREATE FUNCTION insert_user_preferences() RETURNS trigger AS $$
BEGIN
	INSERT INTO user_preferences(id, expiry)
	VALUES(NEW.id, '{"option":0}');

	RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER insert_user_trigger
AFTER INSERT
	ON "user"
FOR EACH ROW
EXECUTE PROCEDURE insert_user_preferences();