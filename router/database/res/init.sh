psql -h localhost -U router -f init.sql
vacuumlo -Urouter router
