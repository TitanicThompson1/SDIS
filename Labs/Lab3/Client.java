
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {
    public static void main(String[] args){
        
        if(args.length < 4 || args.length > 5){
            System.out.println("Usage: java Client <host> <remote_name> <oper> <opnd>*");
            return;
        }

        try {
            Registry registry = LocateRegistry.getRegistry(args[0]);
            RMIInterface stub = (RMIInterface) registry.lookup(args[1]);
            
            switch (args[2]) {
                case "REGISTER":
                    int respR = stub.register(args[3], args[4]);
                    System.out.println("REGISTER " + args[3] + " " + args[4] + ":: " + String.valueOf(respR));        
                    break;
                case "LOOKUP":
                    String respL = stub.lookup(args[3]);
                    System.out.println("REGISTER " + args[3] + ":: " + respL); 
                    break;
                default:
                    System.err.println("You dumb bitch");
                    break;
            }
            
        } catch (Exception e) {
            //TODO: handle exception
        }
    }
}
