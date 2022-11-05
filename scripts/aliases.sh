#!/bin/bash
set -e

alias run_backend_local="export SETTINGS=devLocal; sbt 'runMain com.chrisgoldammer.cocktails.DataMain'; sbt 'runMain com.chrisgoldammer.cocktails.Main'"
alias run_frontend_local="(cd $CD_PATENT/frontend && npm run devLocal)"
alias tunnel_cocktails_frontend="ssh -NL 8083:localhost:8082 bizpersonal"
alias tunnel_cocktails_backend="ssh -NL 8300:localhost:8300 bizpersonal"
alias run_from_server="ssh bizpersonal 'cd ~/code; docker-compose up'"
alias db="psql -U postgres"
