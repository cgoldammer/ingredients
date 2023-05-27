
cat_nginx () { 
    export nginx=$(docker container ls  -a | grep 'code-nginx' | awk '{print $1}' | head -1)
    docker exec -t -i $nginx cat /etc/nginx/nginx.conf
}

run_nginx () { 
    export nginx=$(docker container ls  -a | grep 'code-nginx' | awk '{print $1}' | head -1)
    docker exec -t -i $nginx $1
}

alias test_nginx='echo $(date) && curl localhost:80 || : && cat support/logs/error.log'

resync_nginx () {
    export nginx=$(docker container ls  -a | grep 'code-nginx' | awk '{print $1}' | head -1)
	docker container kill $nginx
	docker-compose up -d nginx
}
