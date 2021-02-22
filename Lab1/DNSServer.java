
import java.io.IOException;

public class DNSServer {
    public static void main (String[] args) throws IOException{
        new DNSServerThread(Integer.parseInt(args[0])).start();
    }
}
