#!/usr/bin/env bash

print_info() {
    echo    ".-----------[BEGIN STATUS]------------."
    echo -e "Log directory:"
    echo -e "\t${LOG_DIR}"
    echo -e ".jar directory:"
    echo -e "\t${JAR_LOC}"
    echo -e "Process IDs:"
    for ((i=0; i<N; i++)); do
        echo -e "\t${PIDS[$i]}"
    done
    echo -e "'-----------[END   STATUS]------------'\n"
}

print_done() {
    echo "Done 👍"
}

terminate() {
    echo -e "\nTerminating Stormy Haystack data servers"
    for ((i=0; i<N; i++)); do
        kill -9 ${PIDS[$i]}
    done
}

interrupt_handler() {
    echo -e "\nCaught interrupt signal. Terminating..."
    terminate
    print_done
    exit
}

SCRIPT_LOC="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ARTIFACT_DIR="../out/artifacts/node_jar"
JAR_LOC="${ARTIFACT_DIR}/server.jar"
LOG_DIR="../logs/tmp"
CONF_DIR="../conf"

usage() {
    echo "Usage: $0"
    echo -e "\n\tLaunches a cluster coordinator based on the coordinator.conf configuration file in ${CONF_DIR}"
}

if [ ! "$(pwd)" == "${SCRIPT_LOC}" ]; then
    echo "WARNING: Script needs to be run from its location. Exiting."
    exit 1
fi

if [ ! -e ${JAR_LOC} ]; then
    echo "ERROR: Server JAR not found at ${JAR_LOC}. Exiting."
     exit 1
fi


if [ ! -d ${LOG_DIR} ]; then
    echo "Creating log dir ${LOG_DIR}"
    mkdir --parents ${LOG_DIR}
fi
    java -Dconfig.file=${CONF_DIR}/coordinator.conf -jar ${JAR_LOC} &>>${LOG_DIR}/coordinator.log &
    PID=$!

trap interrupt_handler INT


print_info

printf "Press any key to terminate... "
read -n1 -s key

echo -e "\nTerminating Stormy Haystack coordinator node ${PID}"
kill -9 ${PID}

print_done
