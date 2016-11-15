#/bin/bash

INSTALL_DIR=/home/rmartyna/Projects/asl-service/config

cd ${INSTALL_DIR}
java -Dasl.properties=${INSTALL_DIR}/asl-service.properties -cp asl-service.jar pl.edu.agh.Main
