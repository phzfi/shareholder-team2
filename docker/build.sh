#!/bin/bash
#Note! Publish only staging and prod images, do not push dev images to docker-registry (but build dev version locally)

GROUP=bc
NAME=shareholder-list-team2

#No need to change these:
BUILD_ENV=$1
VERSION=$2

TAG=$GROUP/$NAME:$BUILD_ENV-$VERSION

#No need to change anything below this line
if ! ( [ "$BUILD_ENV" == "dev" ] || [ "$BUILD_ENV" == "stg" ] || [ "$BUILD_ENV" == "prod" ] ) || [ -z "$VERSION" ]; then
    echo "Usage: ./build.sh [dev|stg|prod] <version>, e.g. ./build.sh stg stg-123"
    exit 1
fi

if ( [ "$BUILD_ENV" == "stg" ] || [ "$BUILD_ENV" == "prod" ] ) && test -z $DOCKER_REGISTRY_USERNAME; then
    echo "Please provide docker-registry.in.phz.fi password in environment by variable DOCKER_REGISTRY_USERNAME from phz.kdbx, if you build this manually"
    exit 1
fi
if ( [ "$BUILD_ENV" == "stg" ] || [ "$BUILD_ENV" == "prod" ] ) && test -z $DOCKER_REGISTRY_PASSWORD; then
    echo "Please provide docker-registry.in.phz.fi password in environment by variable DOCKER_REGISTRY_PASSWORD from phz.kdbx, if you build this manually"
    exit 1
fi

export DOCKER_HOST=

echo "Building $TAG"

echo $BUILD_ENV-$VERSION > ../VERSION

if test "$BUILD_ENV" != "dev"; then
    docker login docker-registry-in.phz.fi -u $DOCKER_REGISTRY_USERNAME -p $DOCKER_REGISTRY_PASSWORD
fi
docker build -f Dockerfile --no-cache --build-arg BUILD_ENV="$BUILD_ENV" -t $TAG . || exit $?


# We don't want to push dev images to registry, just test if building works
if test "$BUILD_ENV" == "stg" || test "$BUILD_ENV" == "prod"; then
    docker tag $TAG docker-registry-in.phz.fi/$TAG \
    && docker push docker-registry-in.phz.fi/$TAG
else
    echo "Skip pushing $BUILD_ENV images to registry, since they are built locally"
fi

if test "$BUILD_ENV" == "prod"; then
    echo "Tagging and pushing $NAME:latest for prod"
    docker tag "$TAG" docker-registry-in.phz.fi/"$GROUP"/"$NAME":latest \
      && docker push docker-registry-in.phz.fi/"$GROUP"/"$NAME":latest
fi

