
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIInterface extends Remote {
    int register(String ip, String port) throws RemoteException;

    String lookup(String dnsName) throws RemoteException;
}

