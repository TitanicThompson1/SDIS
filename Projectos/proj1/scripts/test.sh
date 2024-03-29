#! /usr/bin/bash

# Script for running the test app
# To be run at the root of the compiled tree
# No jar files used
# Assumes that TestApp is the main class 
#  and that it belongs to the test package
# Modify as appropriate, so that it can be run 
#  from the root of the compiled tree

cd ../src/build

# Check number input arguments
argc=$#

if (( argc < 2 )) 
then
	echo "Usage: $0 <peer_ap> backup|restore|delete|reclaim|state [<opnd_1> [<optnd_2]]"
	exit 1
fi

# Assign input arguments to nicely named variables

pap=$1
oper=$2

# Validate remaining arguments 

case $oper in
backup)
	if(( argc != 4 )) 
	then
		echo "Usage: $0 <peer_ap> backup <filename> <rep degree>"
		exit 1
	fi
	opernd_1=$3
	rep_deg=$4
	;;
restore)
	if(( argc != 3 ))
	then
		echo "Usage: $0 <peer_app> restore <r>"
	fi
	opernd_1=$3
	rep_deg=""
	;;
delete)
	if(( argc != 3 ))
	then
		echo "Usage: $0 <peer_app> delete <filename>"
		exit 1
	fi
	opernd_1=$3
	rep_deg=""
	;;
reclaim)
	if(( argc != 3 ))
	then
		echo "Usage: $0 <peer_app> reclaim <max space>"
		exit 1
	fi
	opernd_1=$3
	rep_deg=""
	;;
state)
	if(( argc != 2 ))
	then
		echo "Usage: $0 <peer_app> state"
		exit 1
	fi
	opernd_1=""
	rep_deg=""
	;;
*)
	echo "Usage: $0 <peer_ap> backup|restore|delete|reclaim|state [<opnd_1> [<optnd_2]]"
	exit 1
	;;
esac

# Execute the program
# Should not need to change anything but the class and its package, unless you use any jar file

# echo "java test.TestApp ${pap} ${oper} ${opernd_1} ${rep_deg}"

java src.main.TestApp ${pap} ${oper} ${opernd_1} ${rep_deg}
