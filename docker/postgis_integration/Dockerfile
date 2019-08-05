FROM mdillon/postgis:11-alpine

# this must run after other scripts
COPY ./enable-hstore.sh /docker-entrypoint-initdb.d/zz_enable-hstore.sh
COPY ./postgres.conf /etc/postgresql/postgresql.conf
COPY ./startup.sh /startup.sh

# override data dir so it's stored into the image when doing a commit
ENV PGDATA /var/lib/geocoder_posgres/data
# this 777 will be replaced by 700 at runtime (allows semi-arbitrary "--user" values)
# https://github.com/docker-library/postgres/blob/ba24350aa02996f75cf5b44eea0af082775cc8ec/10/alpine/Dockerfile#L144
RUN mkdir -p "$PGDATA" && chown -R postgres:postgres "$PGDATA" && chmod 777 "$PGDATA"

#ENTRYPOINT ["/startup.sh"]
CMD ["postgres", "-c", "config_file=/etc/postgresql/postgresql.conf"]
