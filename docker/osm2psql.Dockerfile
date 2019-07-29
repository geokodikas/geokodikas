FROM centos:7

ARG HOST_UID
ARG HOST_GID

RUN yum update -y

COPY create_users.sh .

RUN bash create_users.sh && \
    mkdir -p /workdir/input && \
    chown -R luser:users /home/luser && \
    chown -R luser:users /workdir/

RUN yum install -y sudo && \
    echo "luser ALL=(root) NOPASSWD:ALL" > /etc/sudoers.d/user && \
    chmod 0440 /etc/sudoers.d/user


# actual osm2pgsql installation

RUN yum install -y epel-release && \
    yum install -y cmake make gcc-c++ boost-devel expat-devel zlib-devel bzip2-devel postgresql-devel proj-devel proj-epsg lua-devel git wget

WORKDIR /workdir
USER luser

RUN git clone git://github.com/openstreetmap/osm2pgsql.git && \
    cd osm2pgsql && \
    mkdir build && \
    cd build && \
    cmake .. -DCMAKE_BUILD_TYPE=Release && \
    make -j`nproc` && \
    sudo make install && \
    cd /workdir
#    wget https://raw.githubusercontent.com/openstreetmap/Nominatim/master/settings/import-full.style

COPY empty.style /workdir/empty.style

ENV OSM_ARGS "-S /workdir/empty.style -lc --hstore --keep-coastlines -G --slim"
#ENV OSM_ARGS "-S /workdir/empty.style -lc --hstore --keep-coastlines --extra-attributes -v --slim -G"
ENV NUM_PROC "1"
ENV CACHE_MEM "4096"
ENV PG_HOST "postgis"
ENV PG_DB "postgres"
ENV PGPASSWORD ""
ENV PG_USER "postgres"
ENV PG_PORT "5432"
ENV PG_PREFIX "osm_up"

CMD ["sh", "-c", "osm2pgsql ${OSM_ARGS} --number-processes ${NUM_PROC} -C ${CACHE_MEM} -P ${PG_PORT} -U ${PG_USER} -H ${PG_HOST} -d ${PG_DB} /workdir/input/*.pbf --prefix ${PG_PREFIX}"]
