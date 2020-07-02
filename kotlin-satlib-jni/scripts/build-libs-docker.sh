#!/usr/bin/env bash

# Note: this script is being used in `make libs-docker`

set -e

# echo "Checking image '${DOCKER_IMAGE_NAME}' exists..."
if ! docker inspect ${DOCKER_IMAGE_NAME} 2>&1 >/dev/null; then
    echo "Docker image '${DOCKER_IMAGE_NAME}' does not exist!"
    exit 1
fi

# echo "Creating intermediate container for image '${DOCKER_IMAGE_NAME}'..."
id=$(docker create ${DOCKER_IMAGE_NAME})
# echo "Intermediate container id: $id"

echo "Extracting shared libs from intermediate container..."
docker cp $id:${DOCKER_PROJECT_DIR}/build/lib/. ${LIB_DIR}/
docker cp -L $id:/usr/local/lib/libminisat.so ${LIB_DIR}/
docker cp -L $id:/usr/local/lib/libglucose.so ${LIB_DIR}/
docker cp -L $id:/usr/local/lib/libcadical.so ${LIB_DIR}/
docker cp -L $id:/usr/local/lib/libcryptominisat5.so ${LIB_DIR}/

# echo "Removing intermediate container..."
docker rm --volumes $id
