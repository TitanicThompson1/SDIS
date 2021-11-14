package src.main;

import src.protocol.*;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Client class. Connects with peer through RMI
 */
public class TestApp {
    public static void main(String[] args) {
        
        if(args.length < 2 || args.length > 4){
            System.out.println("Usage: java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
            return;
        }

        try {
            // Tries to connect to the RMI stub
            Registry registry = LocateRegistry.getRegistry("localhost");
            PeerInterface stub = (PeerInterface) registry.lookup(args[0]);
            
            switch (args[1]) {
                case "backup":      // Backup protocol
                    int desiredRepDeg = Integer.parseInt(args[3]);
                    if(desiredRepDeg <= 0)
                        throw new Exception("Replication degree not valid. Must be greater than 0.");

                    stub.backup(args[2], Integer.parseInt(args[3]));
                    System.out.println("Backup done!");
                    break;

                case "delete":      // Delete protocol
                    stub.delete(args[2]);
                    System.out.println("Deletion done!");
                    break;

                case "restore":     // Restore protocol
                    stub.restore(args[2]);
                    System.out.println("Restore done!");
                    break;

                case "reclaim":     // Reclaim protocol
                    int availableSize = Integer.parseInt(args[2]);
                    if (availableSize < 0)
                        throw new Exception("Available size not valid. Must be equal or greater than 0.");

                    stub.reclaim(availableSize);
                    System.out.println("Reclaim done!");
                    break;

                case "state":       // State protocol
                    String state = stub.state();
                    System.out.println(state);
                    break;
                    
                default:            // Not recognized
                    System.out.println("Protocol not recognized");
                    break;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}
