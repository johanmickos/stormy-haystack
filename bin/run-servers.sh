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

PKILL_PATTERN="server.jar" # TODO Ensure this doesn't clash with other unrelated processes
SCRIPT_LOC="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
START=0
N=2
MAX_N=5
ARTIFACT_DIR="../out/artifacts/node_jar"
JAR_LOC="${ARTIFACT_DIR}/server.jar"
LOG_DIR="../logs/tmp"
CONF_DIR="../conf"

usage() {
    echo "Usage: $0 [-n|--num-nodes N]"
    echo -e "\n\tLaunches ${N} or N data nodes based on server-{i}.conf configuration files in ${CONF_DIR}"
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

for i in "$@"; do

    case ${i} in
        -n=*|--num-nodes=*)
        N="${i#*=}"
        if [ $N > $MAX_N ]; then
            echo "ERROR: Entered number of nodes ${N} exceeds maximum of ${MAX_N}"
            exit 1
        fi
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

echo  -e "Launching ${N} data nodes\n"
for ((i=0; i<N; i++)); do
    java -Dconfig.file=${CONF_DIR}/server-${i}.conf -jar ${JAR_LOC} &>>${LOG_DIR}/server-${i}.log &
    PIDS[$i]=$!
done

trap interrupt_handler INT


print_info

printf "Press any key to terminate... "
read -n1 -s key

echo -e "\nTerminating Stormy Haystack data servers"
for ((i=0; i<N; i++)); do
    kill -9 ${PIDS[$i]}
done

print_done