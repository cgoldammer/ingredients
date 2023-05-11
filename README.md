# Ingredients App

This is a way of storing recipes that are made up of ingredients, and then summarizing this data in a useful way.

For now, it's focused on cocktails. So I can ask: "What are the cocktails I can make with the ingredients I have at home".

# Build modes


There are the following build modes:

- Local
  1. Compile backend
  2. Run `run_backend_local` alias
  3. Run `run_frontend_local`
- Docker (all commands runnable via `run_{type}` from `scripts/aliases.sh`:
  1. Local: `devDocker`: Dev environment on local docker. Frontend on localhost:8082, backend on localhost:8080
  2. Server - note that this requires running `./deploy.sh` first: 
    a. `devServer`: Dev environment on server. Same ports as `devDocker` (port-forwarded)
    b. `prod`: Prod environment on server. Available on bizpersonal:80, backend on bizpersonal:8081

# Todo

- Improve way for deploying to prod with or without wiping the database and resetting with simple data

# Build process

Run 
```
./scripts/build_docker.sh # to create intermediate docker containers.
docker-compose build
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
tunnel_cocktails_frontend &
tunnel_cocktails_backend &
run_from_server
```
