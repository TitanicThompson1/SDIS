#! /usr/bin/bash


argc=$#

if ((argc == 1 ))
then
    cd ../src/build

    for (( c=1; c<=$1; c++ ))
    do  
    echo 'Peer'$c
    java src.main.TestApp Peer$c state        
    done
else 
	echo "Usage: $0 [<Number_peers>]]"
	exit 1
fi