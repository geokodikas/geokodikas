#!/bin/sh

set -e
set -x

docker build -t ledfan/postgis_hstore:latest .
docker push ledfan/postgis_hstore:latest
