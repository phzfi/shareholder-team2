#!/bin/bash
# Report test coverage
USER=`whoami`
GROUP=`id -gn`
# Check if workspace is set
if [ -z "$workspace" ]; then
  # If not set, default to current directory
  workspace="."
fi

mkdir -p ${workspace}/reports/coverage
docker compose run --rm test
sudo chown -R $USER:$GROUP ${workspace}/reports/coverage || true

