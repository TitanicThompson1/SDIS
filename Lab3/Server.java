
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Hashtable;

public class Server implements RMIInterface {

    private Hashtable<String, String> dnsTable = new Hashtable<>(); 

    public static void main(String[] args){
        if(args.length != 1){
            System.out.println("Usage: java Server <remote_name>");
            return;
        }
        
        try {
            Server obj = new Server();
            RMIInterface stub = (RMIInterface) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(args[0], stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public int register(String ip, String port) throws RemoteException {
        if(hasHostname(ip)){
            return -1;
        }else {
            dnsTable.put(ip, port);
            return dnsTable.size();
        }
    }

    private boolean hasHostname(String ip) {
        return dnsTable.containsKey(ip);
    }

    @Override
    public String lookup(String dnsName) throws RemoteException {
        String ip = dnsTable.get(dnsName);
        if (ip == null) return "NOT_FOUND"; 
        else return ip;
    }

}
