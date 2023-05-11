#!/bin/bash
STARTDIR=$PWD

# This sets up files for Docker.
# In principle, one should use volumes that are then accessed by the different
# containers. But this is hard to on EC2, and requires a complex arrangement.
# So instead, we are simply manually moving files between containers.
# Here, the only thing that's shared is the content for web serving (html + js)
# which is used both by the `app` container (it contains it after building)
# and the `nginx` app (to serve the content).
set -e
cd /home/ec2-user
export nginx=$(docker container ls  -a | grep 'code-nginx' | awk '{print $1}' | head -1)
export app=$(docker container ls  -a | grep 'code-app_dev' | awk '{print $1}'| head -1)
echo "nginx: $nginx | app: $app"
docker cp "$app:/frontend/serve_content" code/temp/serve_content
docker cp code/temp/serve_content/. "$nginx:/usr/share/nginx/html"
docker cp code/temp/serve_content/index_prod.html "$nginx:/usr/share/nginx/html/index.html"
echo "COPYING COMPLETED"
cd $STARTDIR
