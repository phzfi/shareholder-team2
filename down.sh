#!/bin/bash
#Shutdown/destroy the dev env

function docker_down() {
    export DOCKER_HOST=
    #if debugging do not destroy
    if [[ ! -z "$1" ]]; then
        docker compose pause
    else
	docker compose down --remove-orphans
    fi
}

docker_down $1
