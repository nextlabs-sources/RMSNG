-- CREATE USER router PASSWORD '123next!';
-- CREATE DATABASE router OWNER router ENCODING 'UTF-8';
\connect router;
DROP SCHEMA IF EXISTS router CASCADE;
CREATE SCHEMA router AUTHORIZATION router;
SET ROLE router;
\i ../src/db/PostgreSQL/V1_00__psql.sql
\i ../src/db/migration/V1_01__db.sql

CREATE TABLE router.schema_version (
  version_rank int NOT NULL,
  installed_rank int NOT NULL,
  version varchar(50) NOT NULL,
  description varchar(200) NOT NULL,
  type varchar(20) NOT NULL,
  script varchar(1000) NOT NULL,
  checksum int,
  installed_by varchar(30) NOT NULL,
  installed_on timestamp without time zone NOT NULL DEFAULT now(),
  execution_time int NOT NULL,
  success boolean NOT NULL
) WITH (
  OIDS=FALSE
);
CREATE INDEX schema_version_ir_idx ON router.schema_version (installed_rank);
CREATE INDEX schema_version_s_idx ON router.schema_version (success);
CREATE INDEX schema_version_vr_idx ON router.schema_version (version_rank);

COPY schema_version (version_rank, installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	1.00	psql	SQL	V1_00__psql.sql	1999639210	router	2016-04-16 22:01:45.309156	4	t
2	2	1.01	db	SQL	V1_01__db.sql	-2127497235	router	2016-04-16 22:01:45.376885	57	t
\.

COPY tenant (id, name, otp, hsk, server, display_name, description, email, creation_time) FROM stdin;
b1384228-8197-4213-a970-ceaf8c4ae85c	skydrm.com	\\x8610eee7c76ed4006a23a1aca59a88fe	\\x9bf3bac6376535e4dfb239c0dae2f152ca31e7698411f11931e0720296ef2230	https://rmtest.nextlabs.solutions/rms				2016-04-14 15:35:38.199-07
eb1003ae-aa7b-4f10-a375-b866b5308ba4	jt2go	\\x2587d7b8c8a0d0c6341c6fef3d1ec553	\\xa9d0ec8164e648c9f57bb208c270108d43ccf199b35356ac0b537fc5ebd5c4f8	https://rmtest.nextlabs.solutions/rms				2016-04-14 15:35:38.199-07
c4c23616-738c-4cf5-8a76-b90b167282f2	testdrm.com	\\x764e4405e3ffef1cfafa5b19e72daedb	\\xa9d0ec8164e648c9f57bb208c270108d43ccf199b35356ac0b537fc5ebd5c4f8	https://testdrm.com/rms				2016-04-14 15:35:38.199-07
\.
