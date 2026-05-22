#!/bin/bash
#Start the dev env

#PHZ Full Stack convention is to use always virtualization, because
#there are zillions of micro-services each using different node, java, python etc
#versions. To make the head-ache less for developers, let's wrap everything in
#either docker-compose or vagrant. Let's NOT use program language specific tools
#such as pyenv or nvm since they are not language agnostic

if test -z $DOCKER_REGISTRY_USERNAME; then
    echo "Warn: DOCKER_REGISTRY_USERNAME is empty, skipping login"
else
    docker logout
    #docker login -u $DOCKER_REGISTRY_USERNAME -p $DOCKER_REGISTRY_PASSWORD
fi

export DOCKER_HOST=
docker compose build --no-cache
docker compose up -d
exit $?
