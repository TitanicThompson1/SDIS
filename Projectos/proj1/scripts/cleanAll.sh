#! /usr/bin/bash


argc=$#

if ((argc == 1 ))
then
    for (( c=1; c<=$1; c++ ))
    do  
        sh cleanup.sh $c
    done
else 
	echo "Usage: $0 [<Number_peers>]]"
	exit 1
fi