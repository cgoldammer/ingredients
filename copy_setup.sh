#!/bin/bash
STARTDIR=$PWD

set -e
cd /home/ec2-user
export nginx=$(docker container ls  -a | grep 'code-nginx' | awk '{print $1}' | head -1)
export app=$(docker container ls  -a | grep 'code-app_dev' | awk '{print $1}'| head -1)
echo "nginx: $nginx | app: $app"
docker cp "$app:/frontend/serve_content" code/temp/serve_content
docker cp code/temp/serve_content/. "$nginx:/usr/share/nginx/html"
docker cp code/temp/serve_content/index_prod.html "$nginx:/usr/share/nginx/html/index.html"
cd $STARTDIR
