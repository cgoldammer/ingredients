# Ingredients App

This is mainly a way for me to build an app with a decent deployment framework.

Feedback appreciated!

# Build modes

There are the following build modes:

1. `devLocal`: Running dev server on laptop, data served from fakeApi in frontend
2. `devDocker`: Running dev server on docker image on laptop, database is served from docker image on laptop.

# Build process

```
cd dockerfiles
docker build . -f sbt_scala --build-arg SCALA_VERSION="3.1.2" -t scalabase

docker build . -f Dockerfile-scala --target backend-step2-build --tag backend-step2-build

docker-compose build
docker-compose up
```

Then 