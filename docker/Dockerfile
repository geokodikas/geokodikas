FROM mdillon/postgis:11-alpine

# this must run after other scripts
COPY ./enable-hstore.sh /docker-entrypoint-initdb.d/zz_enable-hstore.sh
