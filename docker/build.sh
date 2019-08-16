#!/bin/sh

set -e
set -x

cd postgis

docker build -t ledfan/postgis_hstore:latest .
docker push ledfan/postgis_hstore:latest

cd ..
cd osm2pgsql

docker build -t ledfan/osm2pgsql:latest .
docker push ledfan/osm2pgsql:latest
