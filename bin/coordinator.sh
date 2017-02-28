#!/usr/bin/env bash
SCRIPT_LOC="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

print_info() {
    echo    ".--------------[COORDINATOR STATUS]--------------."
    echo -e "Log location:"
    echo -e "\t${LOG_DIR}"
    echo -e ".jar location:"
    echo -e "\t${JAR_LOC}"
    echo -e "Config location:"
    echo -e "\t${CONF_LOC}"
    echo -e "Process ID:"
    echo -e "\t${PID}"
    echo -e "'-----------[ END COORDINATOR STATUS]------------'\n"
}

print_done() {
    echo "Done 👍"
}

terminate() {
    echo -e "\nTerminating Stormy Haystack coordinator"
    kill -9 ${PID}
}

interrupt_handler() {
    echo -e "\nCaught interrupt signal. Terminating..."
    terminate
    print_done
    exit
}

ARTIFACT_DIR="../out/artifacts/node_jar"
JAR_LOC="${ARTIFACT_DIR}/server.jar"
LOG_DIR="../logs/tmp"
LOG_LOC="${LOG_DIR}/coordinator.log"
CONF_DIR="../conf"
CONF_LOC="${CONF_DIR}/coordinator.conf"

usage() {
    echo "Usage: $0 [-c=CONFIG|--config=CONFIG]"
    echo -e "\n\tLaunches a cluster coordinator based on CONFIG or the default configuration file at ${CONF_LOC}"
}

if [ ! "$(pwd)" == "${SCRIPT_LOC}" ]; then
    echo "WARNING: Script needs to be run from its location. Exiting."
    exit 1
fi

if [ ! -e ${JAR_LOC} ]; then
    echo "ERROR: JAR not found at ${JAR_LOC}. Exiting."
     exit 1
fi


if [ ! -d ${LOG_DIR} ]; then
    echo "Creating log dir ${LOG_DIR}"
    mkdir --parents ${LOG_DIR}
fi

for i in "$@"; do
    case ${i} in
        -c=*|--config=*)
        CONF_LOC="${i#*=}"
        shift
        ;;
        -h|--help)
        usage
        exit 0
        ;;
        *)
        echo "Unknown option: ${i}"
        usage
        exit 1
        ;;
    esac
done

if [ ! -e ${CONF_LOC} ]; then
    echo "ERROR: Configuration not found at ${CONF_LOC}. Exiting."
    exit 1
fi

java -Dconfig.file=${CONF_LOC} -jar ${JAR_LOC} &>>${LOG_LOC} &
PID=$!

trap interrupt_handler INT


print_info

printf "Press any key to terminate... "
read -n1 -s key

terminate
print_done
