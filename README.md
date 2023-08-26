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

See [build_server script](./scripts/build_server.sh).

# SSL

I'm roughly following this [tutorial](https://pentacent.medium.com/nginx-and-lets-encrypt-with-docker-in-less-than-5-minutes-b4b8a60d3a71)

The tricky thing is that I'm hosting this on `cocktails.chrisgoldammer.com`, so how do I satisfy the challenge?
- url=cocktails.chrisgoldammer.com is forwarded to the IP for this app using Route53
- Certbot when run from a machine does this:
  a. Ping url
  b. write some challenge to a folder on the machine
  c. If the ping to the URL matches the challenge, then it's good (because it proves I control the machine)


1. Create a `certbot` service
2. Create a `nginx` service:
  - The service uses the /var/www/certbot to write the certbot challenge
  - ssl secrets are stored like this: `/etc/nginx/ssl/live/cocktails.chrisgoldammer.com/fullchain.pem`
3. Use volumes to ensure that both the challenge and secret location are synced between certbot and nginx.
4. If I run the `certbot` command from the `certbot` instance, roughly this happens:
  - The secret and the challenge are stored on their certbot locations
  - They are synced to the nginx service
  - We do a web request to url. It's forwarded to the machine, and received by nginx. Since nginx has available both the challenge and the secret, it can succesfully resolve the challenge.
  - This requires the 80 port to be open to the world

ALl of this is run with `init.sh`

Todo: I probably want to have some way of backing up the obtained certificates, because otherwise I'd have to recreate them for each push since I'm deleting the code folder.
