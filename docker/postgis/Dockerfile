FROM postgres:11

ENV POSTGIS_MAJOR 2.5
ENV POSTGIS_VERSION 2.5.2+dfsg-1~exp1.pgdg90+1

RUN apt-get update \
      && apt-cache showpkg postgresql-$PG_MAJOR-postgis-$POSTGIS_MAJOR \
      && apt-get install -y --no-install-recommends \
           postgresql-$PG_MAJOR-postgis-$POSTGIS_MAJOR=$POSTGIS_VERSION \
           postgresql-$PG_MAJOR-postgis-$POSTGIS_MAJOR-scripts=$POSTGIS_VERSION \
           postgis=$POSTGIS_VERSION \
      && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /docker-entrypoint-initdb.d

COPY ./initdb-postgis.sh /docker-entrypoint-initdb.d/postgis.sh
COPY ./update-postgis.sh /usr/local/bin
COPY ./postgres.conf /etc/postgresql/postgresql.conf

# override data dir so it's stored into the image when doing a commit
ENV PGDATA /var/lib/geocoder_posgres/data
# this 777 will be replaced by 700 at runtime (allows semi-arbitrary "--user" values)
# https://github.com/docker-library/postgres/blob/ba24350aa02996f75cf5b44eea0af082775cc8ec/10/alpine/Dockerfile#L144
RUN mkdir -p "$PGDATA" && chown -R postgres:postgres "$PGDATA" && chmod 777 "$PGDATA"

CMD ["postgres", "-c", "config_file=/etc/postgresql/postgresql.conf"]
