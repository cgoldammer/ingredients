#!/bin/bash
set -e

echo "Starting deploy"
ssh bizpersonal 'sudo rm -rf ~/code/; mkdir -p ~/code/'
ssh bizpersonal 'sudo rm -rf ~/code/temp; mkdir -p ~/code/temp'
echo "starting sync"
rsync --exclude-from=.rsyncignore -r . bizpersonal:~/code

## RUN IF FLAG = full
# Echo "Building initial images"
# ssh bizpersonal 'bash ~/code/scripts/build_docker.sh'
echo "Setting up folders on containers"
# There is a weird issue where `docker-compose build` is a no-op, likely due to some caching, so need to explicitly build all containers.
# The --no-start is needed so that apps have a name I can refer to, and I will be able to copy files into them.
#
# TODO: Add runtime. It's likely 15-30 min for first-time run. There might be 
# caching speedups for later runs.
ssh bizpersonal 'cd code; docker-compose build nginx app_dev scala_prod scala_dev postgres2; docker-compose --profile all up --no-start'

# Todo: Ensure that App is built locally (it's hosted on nginx)

# This should no longer be necessary
#ssh bizpersonal '~/code/copy_setup.sh'


echo "Deploy completed"
