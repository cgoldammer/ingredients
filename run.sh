#!/bin/bash
set -e

ssh -NL 8082:localhost:8082 bizpersonal & 
ssh -NL 8300:localhost:8300 bizpersonal &
ssh -NL bizpersonal 'cd code; docker-compose up'
