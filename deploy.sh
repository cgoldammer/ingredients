#!/bin/bash
set -e

echo "Starting deploy"
ssh bizpersonal 'rm -rf ~/code/; mkdir -p ~/code/'
rsync --exclude-from=.rsyncignore -r . bizpersonal:~/code
echo "Deploy completed"

## RUN IF FLAG = full
## ssh business "
##   cd ~/code/dockerfiles
##   docker build . -f Dockerfile-scala --target backend-step2-built2 --tag backend-step2-built2
##   cd ..
##   docker build . -f Dockerfile-scala --target backend-step2-built2 --tag backend-step2-built2
## "
ssh bizpersonal 'cd ~/code; docker-compose build'



