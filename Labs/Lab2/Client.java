import java.io.*;
import java.net.*;


public class Client {
    public static void main(String[] args) throws IOException{
        
        if( args.length != 2 ){
            System.out.println("Usage: java Client <multi_cast group> <multicast_port>");
            return;
        }
        
        // get a datagram socket
        
        MulticastSocket socket = new MulticastSocket(Integer.parseInt(args[1]));
        InetAddress address = InetAddress.getByName(args[0]);
        socket.joinGroup(address);
        
        while(true){
            byte[] buf = new byte[256];
        
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            String res = new String(packet.getData(), 0, packet.getLength());
            System.out.println(res);
        }
        
    }
}
