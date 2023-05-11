#!/bin/bash
set -e

# This sets up all services on the EC2 instance
# It should allow the following connections:
# - dev  = bizpersonal:8082
#       A dev version. One should avoid having to use this, and instead prefer local
#       development, but this can spot issues on the EC2 instance 
#       (e.g. that docker-compose isn't running correctly)
#       Accesses data on localhost:8300 (which is run on the server).
#       To open this in localhost:8082 on locally, we need to forward both ports 8082 and 8300.
# - prod = bizpersonal:80

PROFILE=$1

PID=$(lsof -i :8080 | grep ssh | awk 'NR==2 {print $2}'); if [ ! -z "$PID" ]; then kill $PID; fi
PID=$(lsof -i :8082 | grep ssh | awk 'NR==2 {print $2}'); if [ ! -z "$PID" ]; then kill $PID; fi

if [[ "$PROFILE" == "prod" ]]
then
  ssh bizpersonal 'cd code; docker-compose --profile prod up'
else 
  ssh -NL 8082:localhost:8082 bizpersonal & 
  ssh -NL 8080:localhost:8080 bizpersonal &
  ssh bizpersonal 'cd code; docker-compose --profile dev up'
fi

