#!/usr/bin/env bash
PWD=$(pwd)
SCRIPT_LOC="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd ${SCRIPT_LOC}

START=0
N=2
MAX_N=5
ARTIFACT_DIR="../out/artifacts/client_jar"
JAR_LOC="${ARTIFACT_DIR}/client.jar"
LOG_DIR="../logs/tmp"
CONF_DIR="../conf"
CONF_LOC="${CONF_DIR}/client.conf"


if [ ! -e ${JAR_LOC} ]; then
    echo "ERROR: Server JAR not found at ${JAR_LOC}. Exiting."
    exit 1
fi

echo "Config:"
cat ${CONF_LOC}
java -Dconfig.file=${CONF_LOC} -jar ${JAR_LOC}

cd ${PWD}
