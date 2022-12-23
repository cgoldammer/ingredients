#!/bin/bash
set -e

cd ..
cd dockerfiles
docker build . -f sbt_scala --build-arg SCALA_VERSION="3.2.1" --tag scalabase
cd ..
docker build . -f Dockerfile-scala --target backend-step2-built2 --tag backend-step2-built2
