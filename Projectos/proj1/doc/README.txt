All scripts must be run from the scripts directory.
The Peer folders need to be created before running the program (use one of the following scrips).
The RMI registry must be start on the build folder


To compile, run:
> compile.sh

To start a peer, run:
> peer.sh <version> <peer_id> <svc_access_point> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port>

To clean all chunks, maps, restored and log files from a peer, run:
> cleanup.sh <peer_id>

To start the client (TestApp), run:
> test.sh <peer_ap> backup|restore|delete|reclaim|state [<opnd_1> [<optnd_2]]

To create the directory where the peer will store everything, run:
> setup.sh <peer_id>


In addiction to the mandatory scripts, we created a few helper ones.

To start a peer with default parameters, run any of the scripts peer1-4.sh. For example:
> peer1.sh

To see all peers states, from 1 to the parameter, run:
> allStates.sh <nº of peers>

To clean all peers directories, from 1 to the parameter, run:
> cleanAll.sh <nº of peers>

To create all peer directories, from 1 to the parameter, run:
> setupAll.sh <nº of peers>

