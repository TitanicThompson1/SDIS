package src.main;

import src.data.*;
import src.communication.*;
import src.protocol.*;
import src.utils.MyConstants;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Class that setups the peer and channels and runs it
 */
public class PeerRunner {

    public static void main(String[] args) {    //0

        if(args.length != 9){
            System.out.println("Usage: java PeerRunner <ProtocolVersion> <PeerID> <RMI> <IP MC> <Port MC> <IP MBC> <Port MBC> <IP MRC> <Port MRC>");
            return;
        }
        
        MyConstants.path = "Peer" + args[1] + "/";
        
        MyFileHandler.setLogFile("log" + args[2] + ".txt");
        

        // Creating RMI Registry
        try {
            

            // Initializing the peer
            Peer peer = new Peer(args[0], Integer.parseInt(args[1]));

            // Initializing the channels
            Channel mcChannel = new Channel(Integer.parseInt(args[4]), args[3], peer);
            Channel mdbChannel = new Channel(Integer.parseInt(args[6]), args[5], peer);
            Channel mdrChannel = new Channel(Integer.parseInt(args[8]), args[7], peer);


            // Saving the channels in peer
            peer.setMCChannel(mcChannel);
            peer.setMDBChannel(mdbChannel);
            peer.setMDRChannel(mdrChannel);

            PeerInterface stub = (PeerInterface) UnicastRemoteObject.exportObject(peer, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(args[2], stub);          

            // Starting all threads
            Thread t1 = new Thread(mcChannel);
            Thread t2 = new Thread(mdbChannel);
            Thread t3 = new Thread(mdrChannel);
            Thread p = new Thread(peer);
            
            p.start();
            t1.start();
            t2.start();
            t3.start();

            peer.sendIsAliveMessages();
            
            System.out.println("Started Peer" + args[1]);
            try {

                // Waiting for threads to finish
                t1.join();
                t2.join();
                t3.join();
                p.join();
                
            } catch (InterruptedException e) {
                System.out.println("Main thread Interrupted");
            }

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

   
}
