#! /bin/bash
touch admin_list.txt
mvn install
mvn assembly:assembly
