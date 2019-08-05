#!/bin/sh
#set -e
#set -x
#
#echo "Doing a sleep for 20 seconds"
#sleep 20

#echo "Starting up..."
/docker-entrypoint.sh postgres -c config_file=/etc/postgresql/postgresql.conf
