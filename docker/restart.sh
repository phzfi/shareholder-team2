#!/bin/bash
#Deploy to prod/stg
ENV=$1
if [ -z "$ENV" ]; then
    echo "Usage (env=stg/prod): ./restart.sh <env>, e.g. ./restart.sh stg"
    exit 1
fi

SERVICE_NAME=shareholder-list-team2-$ENV
echo "Restarting $SERVICE_NAME"
export DOCKER_HOST=docker-swarm-master.in.phz.fi
echo ""
# Below is example to restart the service that docker containers have.
# Here we set docker swarm to scale to 0 which shutdown all the existing instances/containers.
# Then set it back to their original instance scale amount.
# In this example assume we have 3 docker containers which project have (defined in docker-compose.stg.yml or docker-compose.prod.yml): "frontend", "backend" and "db"
docker service scale ${SERVICE_NAME}_frontend=0 ${SERVICE_NAME}_backend=0 ${SERVICE_NAME}_db=0
if [ "$ENV" == "prod" ];
then
    docker service scale ${SERVICE_NAME}_db=1 ${SERVICE_NAME}_backend=2 ${SERVICE_NAME}_frontend=2
else
    docker service scale ${SERVICE_NAME}_db=1 ${SERVICE_NAME}_backend=1 ${SERVICE_NAME}_frontend=1
fi
export DOCKER_HOST=
echo "Restart DONE!"
