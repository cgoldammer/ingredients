#!/bin/bash
set -e

alias run_backend_local="type=dev hostName=localhost $CD_PATENT/backend/stack.sh exec app -- -e dev"
alias run_frontend_local="(cd $CD_PATENT/frontend && npm run devLocal)"
alias tunnel_cocktails_frontend="ssh -NL 8083:localhost:8082 bizpersonal"
alias tunnel_cocktails_backend="ssh -NL 8300:localhost:8300 bizpersonal"
alias run_patent_from_business="ssh bizpersonal 'cd ~/code; docker-compose up'"