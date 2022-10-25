-- CREATE USER rms PASSWORD '123next!';
-- CREATE DATABASE rms OWNER rms ENCODING 'UTF-8';
\connect rms;
DROP SCHEMA IF EXISTS rms CASCADE;
CREATE SCHEMA rms AUTHORIZATION rms;
SET ROLE rms;
\i ../src/db/PostgreSQL/V1_00__psql.sql
\i ../src/db/migration/V1_01__db.sql
\i ../src/db/migration/V1_02__db.sql
\i ../src/db/migration/V1_03__db.sql
\i ../src/db/migration/V1_04__db.sql
\i ../src/db/migration/V1_05__db.sql

CREATE TABLE rms.schema_version (
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
CREATE INDEX schema_version_ir_idx ON rms.schema_version (installed_rank);
CREATE INDEX schema_version_s_idx ON rms.schema_version (success);
CREATE INDEX schema_version_vr_idx ON rms.schema_version (version_rank);

COPY schema_version (version_rank, installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	1.00	psql	SQL	V1_00__psql.sql	-1507010162	rms	2016-04-17 10:02:03.420792	3	t
2	2	1.01	db	SQL	V1_01__db.sql	2020644568	rms	2016-04-17 10:02:03.515629	81	t
3	3	1.02	db	SQL	V1_02__db.sql	742533032	rms	2016-04-17 10:02:03.515635	16	t
4	4	1.03	db	SQL	V1_03__db.sql	1616675202	rms	2016-08-18 14:20:46.281347	2	t
5	5	1.04	db	SQL	V1_04__db.sql	1616675202	rms	2016-10-21 14:20:46.281347	2	t
6	6	1.05	db	SQL	V1_05__db.sql	1616675202	rms	2016-10-24 17:13:24.281347	2	t
7	7	1.06	db	SQL	V1_06__db.sql	1616675202	rms	2016-11-14 17:13:24.281347	2	t
\.
