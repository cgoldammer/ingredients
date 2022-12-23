#!/bin/bash
set -e

echo "Starting deploy"
ssh bizpersonal 'rm -rf ~/code/; mkdir -p ~/code/'
ssh bizpersonal 'rm -rf ~/code/temp; mkdir -p ~/code/temp'
echo "Starting sync"
rsync --exclude-from=.rsyncignore -r . bizpersonal:~/code


## RUN IF FLAG = full
# Echo "Building initial images"
# ssh bizpersonal 'bash ~/code/scripts/build_docker.sh'

echo "Setting up folders on containers"
ssh bizpersonal 'cd code; docker-compose build; docker-compose up --no-start'
ssh bizpersonal 'bash ~/code/copy_setup.sh'
echo "Deploy completed"
