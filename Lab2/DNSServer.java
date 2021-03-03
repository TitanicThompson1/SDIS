
import java.io.IOException;

public class DNSServer {
    public static void main (String[] args) throws IOException{
        new AnnounceServer(args[0], Integer.parseInt(args[1]),Integer.parseInt(args[2])).start();
    }
}
