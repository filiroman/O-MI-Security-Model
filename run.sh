#!/bin/bash

#Set log level
#Possible values:
#info
#warn
#error
#debug (default)

while getopts ":l:" opt; do
  case $opt in
    l)
      java -jar -Dloglevel=$OPTARG target/OMISec-1.0-SNAPSHOT-jar-with-dependencies.jar
      exit 1
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit 1
      ;;
    :)
      echo "Option -$OPTARG requires an argument." >&2
      exit 1
      ;;
  esac
done
java -jar  target/OMISec-1.0-SNAPSHOT-jar-with-dependencies.jar
