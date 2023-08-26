#!/bin/bash
set -e

alias run_backend_local="export SETTINGS=devLocal && cd $CD_COCKTAIL/backend && sbt 'runMain 
com.chrisgoldammer.cocktails.DataSetupDevMain' && sbt 'runMain com.chrisgoldammer.cocktails.Main'"
alias run_frontend_local="(cd $CD_COCKTAIL/frontend && npm run devLocal)"
alias tunnel_cocktails_frontend="ssh -NL 8083:localhost:8082 bizpersonal"
alias tunnel_cocktails_backend="ssh -NL 8300:localhost:8300 bizpersonal"
alias db="psql -U postgres"


alias run_dev_docker="docker compose --profile dev up"
alias run_dev_server="~/run.sh dev"
alias run_prod="~/run.sh prod"
alias certbot='ssh bizpersonal "docker compose run --rm  certbot certonly --webroot --webroot-path /var/www/certbot/ --dry-run -d cocktails.chrisgoldammer.com"'


# function run_local () {
#     run_backend_local
#     run_frontend_local
# }
