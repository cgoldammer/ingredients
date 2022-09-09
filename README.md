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
docker build . -f sbt_scala --build-arg SCALA_VERSION="3.1.2" -t scalabase]
cd ..
docker build . -f Dockerfile-scala --target backend-step2-built2 --tag backend-step2-built2

docker-compose build
docker-compose up
```



# Setup for EC2 server

```angular2html
sudo yum update -y
sudo yum install tmux -y
sudo yum install docker -y
```

// https://www.cyberciti.biz/faq/how-to-install-docker-on-amazon-linux-2/
```angular2html
sudo usermod -a -G docker ec2-user
id ec2-user
newgrp docker
sudo yum install python3-pip
pip3 install --user docker-compose

sudo systemctl enable docker.service
sudo systemctl start docker.service
```


To run server:
```angular2html
./deploy.sh
tunnel_patent_web_from_business &
tunnel_patent_api_from_business &
run_patent_from_business
```
