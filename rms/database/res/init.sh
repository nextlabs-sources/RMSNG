psql -h localhost -U rms -f init.sql
vacuumlo -Urms rms
